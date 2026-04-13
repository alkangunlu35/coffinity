package com.icoffee.app.data

import android.content.Context
import com.icoffee.app.data.model.MoodType
import com.icoffee.app.data.model.RecommendationItem
import com.icoffee.app.data.model.RecommendationTrait
import com.icoffee.app.data.model.TimeBucket
import java.time.LocalTime
import kotlin.math.max

object RecommendationEngine {
    private val reshuffleNonceByMood = mutableMapOf<MoodType, Int>()

    fun initialize(context: Context) {
        UserPreferenceMemory.initialize(context)
    }

    fun defaultMoodForNow(now: LocalTime = LocalTime.now()): MoodType {
        return when (timeBucket(now)) {
            TimeBucket.MORNING -> MoodType.FOCUSED
            TimeBucket.AFTERNOON -> MoodType.SOCIAL
            TimeBucket.EVENING -> MoodType.RELAXED
            TimeBucket.NIGHT -> MoodType.RELAXED
        }
    }

    fun lastMoodOrDefault(now: LocalTime = LocalTime.now()): MoodType {
        return UserPreferenceMemory.lastSelectedMood() ?: defaultMoodForNow(now)
    }

    fun onMoodSelected(mood: MoodType) {
        UserPreferenceMemory.recordMoodSelection(mood)
        reshuffleNonceByMood[mood] = 0
    }

    fun onRecommendationTapped(item: RecommendationItem) {
        UserPreferenceMemory.recordRecommendationTapped(item)
    }

    fun onBrewingMethodOpened(methodId: String) {
        UserPreferenceMemory.recordBrewingMethodOpened(
            methodId = methodId,
            traits = PhaseOneRepository.methodTraits(methodId)
        )
    }

    fun recommendationsForMood(
        mood: MoodType,
        reshuffle: Boolean,
        now: LocalTime = LocalTime.now()
    ): List<RecommendationItem> {
        val pool = PhaseOneRepository.recommendationPool(mood).ifEmpty {
            PhaseOneRepository.recommendationPool(MoodType.FOCUSED)
        }
        if (pool.isEmpty()) return emptyList()

        val timeBucket = timeBucket(now)
        val nonce = updateNonce(mood, reshuffle)
        val recent = UserPreferenceMemory.recentShownIds()
        val ranked = pool
            .map { item ->
                item to scoreItem(
                    item = item,
                    timeBucket = timeBucket,
                    nonce = nonce,
                    recentHistory = recent
                )
            }
            .sortedByDescending { it.second }
            .map { it.first }

        val visible = ranked.take(2)
        UserPreferenceMemory.recordRecommendationsShown(visible.map { it.id })

        return ranked.map { item ->
            item.copy(
                reason = buildWhyThisPick(item, mood, timeBucket)
            )
        }
    }

    private fun updateNonce(mood: MoodType, reshuffle: Boolean): Int {
        val current = reshuffleNonceByMood[mood] ?: 0
        val updated = if (reshuffle) current + 1 else max(current, 0)
        reshuffleNonceByMood[mood] = updated
        return updated
    }

    private fun scoreItem(
        item: RecommendationItem,
        timeBucket: TimeBucket,
        nonce: Int,
        recentHistory: List<String>
    ): Double {
        var score = item.moodAffinity * 1.15

        if (item.preferredTimes.contains(timeBucket)) {
            score += 2.2
        } else {
            score -= 0.35
        }

        item.traits.forEach { trait ->
            score += UserPreferenceMemory.traitInterest(trait) * 0.28
        }

        val recentIndex = recentHistory.indexOf(item.id)
        if (recentIndex >= 0) {
            score -= (4.2 - (recentIndex * 0.45)).coerceAtLeast(0.9)
        }

        // Reshuffle keeps coherence but rotates ranking to avoid static output.
        val rotationJitter = (((item.id.hashCode() xor (nonce * 37)) and 0xFF) / 255.0) * 0.8
        score += rotationJitter

        return score
    }

    private fun buildWhyThisPick(
        item: RecommendationItem,
        mood: MoodType,
        timeBucket: TimeBucket
    ): String {
        val topTrait = UserPreferenceMemory.topTraits(limit = 1).firstOrNull()
        val moodLine = when (mood) {
            MoodType.RELAXED -> "A smooth match for your relaxed pace"
            MoodType.ENERGETIC -> "Built for an energetic coffee boost"
            MoodType.FOCUSED -> "Designed to keep your focus sharp"
            MoodType.SOCIAL -> "Balanced for a social coffee moment"
            MoodType.CURIOUS -> "Great for your curious tasting mood"
            MoodType.CALM -> "Perfect for a calm and peaceful break"
            MoodType.ADVENTUROUS -> "Matches your adventurous coffee spirit"
            MoodType.COZY -> "A cozy companion for your quiet time"
        }

        val timeLine = when (timeBucket) {
            TimeBucket.MORNING -> "Good for the morning rhythm"
            TimeBucket.AFTERNOON -> "Balanced for the afternoon window"
            TimeBucket.EVENING -> "A softer fit for evening sipping"
            TimeBucket.NIGHT -> "Calm enough for a late coffee ritual"
        }

        val memoryLine = when (topTrait) {
            RecommendationTrait.MILK -> "because you've been exploring milk-forward cups"
            RecommendationTrait.FILTER -> "because you've been exploring filter brews"
            RecommendationTrait.BOLD -> "because you often open bold profiles"
            RecommendationTrait.CLEAN -> "because you favor clean, crisp extractions"
            RecommendationTrait.COLD -> "because chilled styles match your recent picks"
            RecommendationTrait.SWEET -> "because sweeter profiles match your activity"
            RecommendationTrait.FRUITY -> "because you've shown interest in fruit-forward notes"
            RecommendationTrait.NUTTY -> "because nutty notes fit your recent choices"
            null -> null
        }

        return when {
            memoryLine != null && item.traits.contains(topTrait) -> "$timeLine, $memoryLine."
            item.preferredTimes.contains(timeBucket) -> "$moodLine. $timeLine."
            else -> "$moodLine."
        }
    }

    private fun timeBucket(now: LocalTime): TimeBucket {
        return when (now.hour) {
            in 5..11 -> TimeBucket.MORNING
            in 12..16 -> TimeBucket.AFTERNOON
            in 17..21 -> TimeBucket.EVENING
            else -> TimeBucket.NIGHT
        }
    }
}
