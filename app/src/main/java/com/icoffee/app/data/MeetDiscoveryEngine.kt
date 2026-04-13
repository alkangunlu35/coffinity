package com.icoffee.app.data

import com.icoffee.app.data.model.CoffeeMeet
import com.icoffee.app.data.model.EventHostType
import com.icoffee.app.data.model.MeetDiscoverySection
import com.icoffee.app.data.model.MeetDiscoverySectionType
import com.icoffee.app.data.model.MeetEventType
import com.icoffee.app.data.model.MeetExploreSort
import com.icoffee.app.data.model.MeetMood
import com.icoffee.app.data.model.RankedMeetEvent
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object MeetDiscoveryEngine {
    internal const val DEFAULT_USER_LAT = 41.0082
    internal const val DEFAULT_USER_LON = 28.9784
    private const val DEFAULT_RADIUS_KM = 18.0

    fun buildSections(
        events: List<CoffeeMeet>,
        selectedMood: MeetMood,
        userLatitude: Double = DEFAULT_USER_LAT,
        userLongitude: Double = DEFAULT_USER_LON,
        radiusKm: Double = DEFAULT_RADIUS_KM
    ): List<MeetDiscoverySection> {
        if (events.isEmpty()) return emptyList()

        val activeEvents = filterActiveFutureEvents(events)
        val localEvents = filterLocalEvents(activeEvents, userLatitude, userLongitude, radiusKm)
        val source = if (localEvents.isNotEmpty()) localEvents else activeEvents

        val ranked = rankEvents(
            events = source,
            selectedMood = selectedMood,
            userLatitude = userLatitude,
            userLongitude = userLongitude
        )

        val smartPicks = ranked
            .sortedByDescending { it.score }
            .balancedTake(maxItems = 4, maxBusinessItems = 2)
            .map { it.meet }
        val nearYou = ranked
            .sortedWith(compareBy<RankedMeetEvent> { it.distanceKm }.thenByDescending { it.score })
            .balancedTake(maxItems = 4, maxBusinessItems = 2)
            .map { it.meet }

        val happeningSoon = ranked
            .sortedWith(compareByDescending<RankedMeetEvent> { it.timeScore }.thenByDescending { it.score })
            .balancedTake(maxItems = 4, maxBusinessItems = 2)
            .map { it.meet }

        val moodSections = MeetMood.entries.mapNotNull { mood ->
            val moodEvents = ranked
                .filter { mapPurposeToMood(it.meet.purpose) == mood }
                .balancedTake(maxItems = 3, maxBusinessItems = 1)
                .map { it.meet }
            if (moodEvents.isEmpty()) null else {
                MeetDiscoverySection(
                    id = "mood_${mood.name.lowercase()}",
                    type = MeetDiscoverySectionType.MOOD_BASED,
                    mood = mood,
                    events = moodEvents
                )
            }
        }

        return buildList {
            if (smartPicks.isNotEmpty()) {
                add(
                    MeetDiscoverySection(
                        id = "smart_picks",
                        type = MeetDiscoverySectionType.SMART_PICKS,
                        events = smartPicks
                    )
                )
            }
            if (nearYou.isNotEmpty()) {
                add(
                    MeetDiscoverySection(
                        id = "near_you",
                        type = MeetDiscoverySectionType.NEAR_YOU,
                        events = nearYou
                    )
                )
            }
            if (happeningSoon.isNotEmpty()) {
                add(
                    MeetDiscoverySection(
                        id = "happening_soon",
                        type = MeetDiscoverySectionType.HAPPENING_SOON,
                        events = happeningSoon
                    )
                )
            }
            addAll(moodSections)
        }
    }

    fun exploreEvents(
        events: List<CoffeeMeet>,
        selectedMood: MeetMood,
        moodFilter: MeetMood?,
        sort: MeetExploreSort,
        userLatitude: Double = DEFAULT_USER_LAT,
        userLongitude: Double = DEFAULT_USER_LON,
        radiusKm: Double = DEFAULT_RADIUS_KM
    ): List<CoffeeMeet> {
        val activeEvents = filterActiveFutureEvents(events)
        val localEvents = filterLocalEvents(activeEvents, userLatitude, userLongitude, radiusKm)
        val source = if (localEvents.isNotEmpty()) localEvents else activeEvents
        val filtered = if (moodFilter == null) {
            source
        } else {
            source.filter { mapPurposeToMood(it.purpose) == moodFilter }
        }
        val ranked = rankEvents(
            events = filtered,
            selectedMood = selectedMood,
            userLatitude = userLatitude,
            userLongitude = userLongitude
        )
        return when (sort) {
            MeetExploreSort.RELEVANCE -> ranked.sortedByDescending { it.score }
            MeetExploreSort.DISTANCE -> ranked.sortedBy { it.distanceKm }
            MeetExploreSort.SOONEST -> ranked.sortedByDescending { it.timeScore }
        }.map { it.meet }
    }

    fun mapPurposeToMood(purpose: String): MeetMood {
        val normalized = purpose.trim().lowercase()
        return when {
            "network" in normalized -> MeetMood.NETWORKING
            "study" in normalized || "work" in normalized || "focus" in normalized ->
                MeetMood.PRODUCTIVE
            "deep" in normalized || "conversation" in normalized || "talk" in normalized ->
                MeetMood.DEEP_TALK
            else -> MeetMood.CHILL
        }
    }

    private fun filterActiveFutureEvents(events: List<CoffeeMeet>): List<CoffeeMeet> {
        val now = LocalDateTime.now()
        return events.filter { meet ->
            if (meet.scheduledAt <= 0L) return@filter true
            val meetTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(meet.scheduledAt),
                ZoneId.systemDefault()
            )
            ChronoUnit.HOURS.between(meetTime, now) <= 2
        }
    }

    private fun filterLocalEvents(
        events: List<CoffeeMeet>,
        userLatitude: Double,
        userLongitude: Double,
        radiusKm: Double
    ): List<CoffeeMeet> {
        return events.filter {
            distanceKm(
                lat1 = userLatitude,
                lon1 = userLongitude,
                lat2 = it.latitude,
                lon2 = it.longitude
            ) <= radiusKm
        }
    }

    private fun rankEvents(
        events: List<CoffeeMeet>,
        selectedMood: MeetMood,
        userLatitude: Double,
        userLongitude: Double
    ): List<RankedMeetEvent> {
        return events.map { meet ->
            val distance = distanceKm(
                lat1 = userLatitude,
                lon1 = userLongitude,
                lat2 = meet.latitude,
                lon2 = meet.longitude
            )
            val distanceScore = when {
                distance <= 1.5 -> 26.0
                distance <= 4.0 -> 20.0
                distance <= 8.0 -> 14.0
                distance <= 15.0 -> 8.0
                else -> 3.0
            }

            val timeScore = computeTimeScore(meet)

            val mappedMood = mapPurposeToMood(meet.purpose)
            val moodScore = when {
                mappedMood == selectedMood -> 22.0
                selectedMood == MeetMood.CHILL && mappedMood == MeetMood.DEEP_TALK -> 8.0
                selectedMood == MeetMood.PRODUCTIVE && mappedMood == MeetMood.NETWORKING -> 7.0
                else -> 4.0
            }

            val occupancy = meet.participants.size.toDouble() / meet.maxParticipants.coerceAtLeast(1).toDouble()
            val popularityScore = (occupancy * 14.0).coerceAtMost(14.0)
            val urgencyScore = when {
                occupancy >= 0.92 -> 8.0
                occupancy >= 0.75 -> 6.0
                occupancy >= 0.5 -> 4.0
                else -> 2.0
            }

            val createdByUserScore = if (meet.isCreatedByUser) 3.0 else 0.0

            val businessDampener = when {
                meet.eventType == MeetEventType.BUSINESS && meet.businessOffer != null -> -2.0
                meet.eventType == MeetEventType.BUSINESS -> -1.0
                else -> 0.0
            }

            val totalScore = distanceScore + timeScore + moodScore +
                popularityScore + urgencyScore + createdByUserScore + businessDampener

            RankedMeetEvent(
                meet = meet,
                score = totalScore,
                distanceKm = distance,
                distanceScore = distanceScore,
                timeScore = timeScore,
                moodScore = moodScore,
                popularityScore = popularityScore,
                urgencyScore = urgencyScore
            )
        }
    }

    private fun computeTimeScore(meet: CoffeeMeet): Double {
        if (meet.scheduledAt > 0L) {
            val meetTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(meet.scheduledAt),
                ZoneId.systemDefault()
            )
            val now = LocalDateTime.now()
            val hoursUntil = ChronoUnit.HOURS.between(now, meetTime)
            return when {
                hoursUntil < -1 -> 1.0   // already past
                hoursUntil <= 1 -> 24.0  // happening now / imminent
                hoursUntil <= 6 -> 20.0  // today, very soon
                hoursUntil <= 14 -> 16.0 // today later
                hoursUntil <= 28 -> 12.0 // tomorrow
                hoursUntil <= 72 -> 8.0  // this week
                else -> 4.0
            }
        }
        // Legacy string-based fallback for pre-migration entries
        return when {
            meet.time.contains("now", ignoreCase = true) -> 24.0
            meet.time.contains("today", ignoreCase = true) -> 20.0
            meet.time.contains("tonight", ignoreCase = true) -> 18.0
            meet.time.contains("tomorrow", ignoreCase = true) -> 12.0
            else -> 6.0
        }
    }

    private fun distanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val originLat = Math.toRadians(lat1)
        val targetLat = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
            sin(dLon / 2) * sin(dLon / 2) * cos(originLat) * cos(targetLat)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    private fun List<RankedMeetEvent>.balancedTake(
        maxItems: Int,
        maxBusinessItems: Int
    ): List<RankedMeetEvent> {
        if (isEmpty() || maxItems <= 0) return emptyList()

        val selected = mutableListOf<RankedMeetEvent>()
        var businessCount = 0
        val deferredBusiness = mutableListOf<RankedMeetEvent>()

        for (ranked in this) {
            if (selected.size >= maxItems) break
            if (ranked.meet.isBusinessEvent()) {
                if (businessCount < maxBusinessItems) {
                    selected += ranked
                    businessCount++
                } else {
                    deferredBusiness += ranked
                }
            } else {
                selected += ranked
            }
        }

        if (selected.size < maxItems && deferredBusiness.isNotEmpty()) {
            for (businessEvent in deferredBusiness) {
                if (selected.size >= maxItems) break
                selected += businessEvent
            }
        }

        return selected
    }

    private fun CoffeeMeet.isBusinessEvent(): Boolean {
        return eventType == MeetEventType.BUSINESS ||
            hostType == EventHostType.BUSINESS
    }
}
