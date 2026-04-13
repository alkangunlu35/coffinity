package com.icoffee.app.data

import com.icoffee.app.data.model.CoffeeBuddyCandidate
import com.icoffee.app.data.model.CoffeeBuddyDiscoveryResult
import com.icoffee.app.data.model.CoffeeBuddySignal
import com.icoffee.app.data.model.CoffeeBuddySignalType
import com.icoffee.app.data.model.CoffeeMeet
import com.icoffee.app.data.model.CoffeeType
import com.icoffee.app.data.model.MeetMood
import com.icoffee.app.data.model.TasteNote
import com.icoffee.app.data.model.UserTasteProfile
import com.icoffee.app.data.model.topCoffeeTypes
import com.icoffee.app.data.model.topOrigins
import com.icoffee.app.data.model.topPreferredNotes
import java.text.Normalizer
import java.util.Locale

object CoffeeFriendDiscoveryEngine {

    private const val MAX_RESULTS = 5
    private const val RECENT_EVENT_WINDOW_MS = 24 * 60 * 60 * 1000L

    private val demoIdPrefixes = listOf("guest", "demo_", "sample_", "test_", "host_", "room_")

    private val knownOrigins = linkedMapOf(
        "ethiopia" to "Ethiopia",
        "colombia" to "Colombia",
        "brazil" to "Brazil",
        "kenya" to "Kenya",
        "guatemala" to "Guatemala",
        "peru" to "Peru",
        "honduras" to "Honduras",
        "costa rica" to "Costa Rica",
        "sumatra" to "Sumatra",
        "indonesia" to "Indonesia",
        "yemen" to "Yemen",
        "mexico" to "Mexico",
        "nicaragua" to "Nicaragua",
        "el salvador" to "El Salvador",
        "panama" to "Panama",
        "rwanda" to "Rwanda",
        "uganda" to "Uganda",
        "burundi" to "Burundi",
        "turkiye" to "Türkiye",
        "turkey" to "Türkiye"
    )

    private val knownCityOrArea = linkedMapOf(
        "istanbul" to "Istanbul",
        "izmir" to "İzmir",
        "ankara" to "Ankara",
        "antalya" to "Antalya",
        "bursa" to "Bursa",
        "kadikoy" to "Kadıköy",
        "moda" to "Moda",
        "cihangir" to "Cihangir",
        "karakoy" to "Karaköy",
        "besiktas" to "Beşiktaş",
        "berlin" to "Berlin",
        "munich" to "Munich",
        "hamburg" to "Hamburg",
        "paris" to "Paris",
        "lyon" to "Lyon",
        "madrid" to "Madrid",
        "barcelona" to "Barcelona",
        "valencia" to "Valencia",
        "lisbon" to "Lisbon",
        "porto" to "Porto",
        "sao paulo" to "São Paulo",
        "rio de janeiro" to "Rio de Janeiro",
        "new york" to "New York",
        "london" to "London",
        "rome" to "Rome",
        "milan" to "Milan",
        "vienna" to "Vienna",
        "amsterdam" to "Amsterdam",
        "athens" to "Athens"
    )

    fun discover(
        events: List<CoffeeMeet>,
        currentUserId: String,
        selectedMood: MeetMood,
        currentTasteProfile: UserTasteProfile
    ): CoffeeBuddyDiscoveryResult {
        if (currentUserId.isBlank() || currentUserId == "guest") {
            return CoffeeBuddyDiscoveryResult(
                mood = selectedMood,
                discoverableUserCount = 0,
                candidates = emptyList(),
                profileReady = false
            )
        }

        val activeEvents = events.filter(::isEventActiveForDiscovery)
        val currentUserEvents = activeEvents.filter { event ->
            event.hostId == currentUserId || currentUserId in event.participants
        }

        val currentContext = buildCurrentUserContext(
            currentUserEvents = currentUserEvents,
            selectedMood = selectedMood,
            currentTasteProfile = currentTasteProfile
        )

        val candidateIds = activeEvents
            .flatMap { event -> listOf(event.hostId) + event.participants }
            .distinct()
            .filter { userId -> isEligibleUserId(userId, currentUserId) }

        val scoredCandidates = candidateIds.mapNotNull { userId ->
            val aggregate = aggregateUser(userId = userId, events = activeEvents)
            scoreCandidate(
                aggregate = aggregate,
                currentContext = currentContext,
                selectedMood = selectedMood
            )
        }

        val ranked = scoredCandidates
            .sortedWith(
                compareByDescending<CoffeeBuddyCandidate> { it.score }
                    .thenByDescending { it.socialActivityCount }
                    .thenBy { it.userId }
            )

        return CoffeeBuddyDiscoveryResult(
            mood = selectedMood,
            discoverableUserCount = ranked.size,
            candidates = ranked.take(MAX_RESULTS),
            profileReady = currentTasteProfile.interactionCount >= 3 || currentUserEvents.isNotEmpty()
        )
    }

    private fun isEventActiveForDiscovery(event: CoffeeMeet): Boolean {
        if (event.scheduledAt <= 0L) return true
        val now = System.currentTimeMillis()
        return event.scheduledAt >= now - RECENT_EVENT_WINDOW_MS
    }

    private fun isEligibleUserId(userId: String, currentUserId: String): Boolean {
        if (userId.isBlank() || userId == currentUserId) return false
        val normalized = normalizeToken(userId)
        if (normalized.isBlank()) return false
        if (demoIdPrefixes.any { prefix -> normalized.startsWith(prefix) }) return false
        if (normalized.length < 4) return false
        return true
    }

    private data class UserAggregate(
        val userId: String,
        val displayName: String?,
        val cityOrArea: String?,
        val dominantMood: MeetMood,
        val brewInterests: Set<String>,
        val originInterests: Map<String, String>,
        val inferredNotes: Set<TasteNote>,
        val eventIds: Set<String>,
        val primaryEventId: String?
    )

    private data class CurrentUserContext(
        val cityOrArea: String?,
        val preferredNotes: Set<TasteNote>,
        val brewInterests: Set<String>,
        val originInterests: Map<String, String>,
        val eventIds: Set<String>
    )

    private fun aggregateUser(userId: String, events: List<CoffeeMeet>): UserAggregate {
        val userEvents = events.filter { event ->
            event.hostId == userId || userId in event.participants
        }

        val cityOrArea = mostFrequent(
            userEvents.mapNotNull { extractCityOrArea(it.locationName) }
        )

        val moodCounts = userEvents
            .groupingBy { MeetDiscoveryEngine.mapPurposeToMood(it.purpose) }
            .eachCount()
        val dominantMood = moodCounts.maxByOrNull { it.value }?.key ?: MeetMood.CHILL

        val brewInterests = userEvents
            .mapNotNull { normalizeBrewType(it.brewingType) }
            .toSet()

        val originInterests = extractOrigins(userEvents)

        val inferredNotes = brewInterests
            .flatMap { brew -> inferredNotesForBrew(brew) }
            .toSet()

        val primaryEventId = userEvents
            .sortedWith(compareBy<CoffeeMeet> { it.scheduledAt.takeIf { ts -> ts > 0L } ?: Long.MAX_VALUE })
            .firstOrNull()
            ?.id

        return UserAggregate(
            userId = userId,
            displayName = readableDisplayNameFromUserId(userId),
            cityOrArea = cityOrArea,
            dominantMood = dominantMood,
            brewInterests = brewInterests,
            originInterests = originInterests,
            inferredNotes = inferredNotes,
            eventIds = userEvents.map { it.id }.toSet(),
            primaryEventId = primaryEventId
        )
    }

    private fun buildCurrentUserContext(
        currentUserEvents: List<CoffeeMeet>,
        selectedMood: MeetMood,
        currentTasteProfile: UserTasteProfile
    ): CurrentUserContext {
        val cityOrArea = mostFrequent(
            currentUserEvents.mapNotNull { extractCityOrArea(it.locationName) }
        )

        val eventBrews = currentUserEvents
            .mapNotNull { normalizeBrewType(it.brewingType) }
            .toSet()

        val profileBrews = currentTasteProfile
            .topCoffeeTypes(limit = 3)
            .mapNotNull(::brewFromCoffeeType)
            .toSet()

        val preferredNotes = currentTasteProfile.topPreferredNotes(limit = 4).toSet()
        val fallbackNotes = inferredNotesForMood(selectedMood)

        val originInterests = buildMap {
            currentTasteProfile.topOrigins(limit = 5).forEach { origin ->
                val normalized = normalizeToken(origin)
                if (normalized.isNotBlank()) {
                    put(normalized, origin)
                }
            }
            extractOrigins(currentUserEvents).forEach { (key, value) ->
                putIfAbsent(key, value)
            }
        }

        return CurrentUserContext(
            cityOrArea = cityOrArea,
            preferredNotes = if (preferredNotes.isEmpty()) fallbackNotes else preferredNotes,
            brewInterests = eventBrews + profileBrews,
            originInterests = originInterests,
            eventIds = currentUserEvents.map { it.id }.toSet()
        )
    }

    private fun scoreCandidate(
        aggregate: UserAggregate,
        currentContext: CurrentUserContext,
        selectedMood: MeetMood
    ): CoffeeBuddyCandidate? {
        var score = 0
        val signals = mutableListOf<CoffeeBuddySignal>()

        if (
            !currentContext.cityOrArea.isNullOrBlank() &&
            !aggregate.cityOrArea.isNullOrBlank() &&
            normalizeToken(currentContext.cityOrArea) == normalizeToken(aggregate.cityOrArea)
        ) {
            score += 34
            signals += CoffeeBuddySignal(
                type = CoffeeBuddySignalType.SAME_CITY,
                detail = aggregate.cityOrArea
            )
        }

        if (aggregate.dominantMood == selectedMood) {
            score += 22
            signals += CoffeeBuddySignal(type = CoffeeBuddySignalType.SAME_MOOD)
        }

        val tasteOverlapCount = currentContext.preferredNotes
            .intersect(aggregate.inferredNotes)
            .size
        if (tasteOverlapCount > 0) {
            score += (6 + tasteOverlapCount * 4).coerceAtMost(18)
            signals += CoffeeBuddySignal(type = CoffeeBuddySignalType.TASTE_SIMILARITY)
        }

        val sharedBrew = currentContext.brewInterests
            .intersect(aggregate.brewInterests)
            .toList()
            .sorted()
        if (sharedBrew.isNotEmpty()) {
            score += (7 + sharedBrew.size * 4).coerceAtMost(16)
            signals += CoffeeBuddySignal(
                type = CoffeeBuddySignalType.SHARED_BREW,
                detail = sharedBrew.take(2).joinToString(" • ")
            )
        }

        val sharedOriginKeys = currentContext.originInterests.keys
            .intersect(aggregate.originInterests.keys)
        if (sharedOriginKeys.isNotEmpty()) {
            score += (6 + sharedOriginKeys.size * 2).coerceAtMost(12)
            val originLabel = sharedOriginKeys.firstNotNullOfOrNull { key ->
                aggregate.originInterests[key] ?: currentContext.originInterests[key]
            }
            signals += CoffeeBuddySignal(
                type = CoffeeBuddySignalType.SHARED_ORIGIN,
                detail = originLabel
            )
        }

        val sharedEventCount = currentContext.eventIds
            .intersect(aggregate.eventIds)
            .size
        if (sharedEventCount > 0) {
            score += (4 + sharedEventCount * 2).coerceAtMost(10)
            signals += CoffeeBuddySignal(type = CoffeeBuddySignalType.SHARED_ACTIVITY)
        }

        score += (aggregate.eventIds.size * 2).coerceAtMost(8)

        if (signals.isEmpty()) return null

        return CoffeeBuddyCandidate(
            userId = aggregate.userId,
            displayName = aggregate.displayName,
            cityOrArea = aggregate.cityOrArea,
            score = score.coerceIn(0, 100),
            sharedSignals = signals.take(3),
            socialActivityCount = aggregate.eventIds.size,
            eventId = aggregate.primaryEventId
        )
    }

    private fun brewFromCoffeeType(type: CoffeeType): String? = when (type) {
        CoffeeType.WHOLE_BEAN,
        CoffeeType.GROUND -> "Filter"
        CoffeeType.CAPSULE -> "Espresso"
        CoffeeType.READY_TO_DRINK -> "Cold Brew"
        CoffeeType.INSTANT -> "Instant"
        CoffeeType.UNKNOWN -> null
    }

    private fun inferredNotesForMood(mood: MeetMood): Set<TasteNote> = when (mood) {
        MeetMood.CHILL -> setOf(TasteNote.SMOOTH, TasteNote.CHOCOLATE)
        MeetMood.PRODUCTIVE -> setOf(TasteNote.BRIGHT, TasteNote.NUTTY)
        MeetMood.DEEP_TALK -> setOf(TasteNote.BOLD, TasteNote.SMOKY)
        MeetMood.NETWORKING -> setOf(TasteNote.CARAMEL, TasteNote.FRUITY)
    }

    private fun inferredNotesForBrew(brew: String): Set<TasteNote> {
        val normalized = normalizeToken(brew)
        return when {
            normalized.contains("espresso") -> setOf(TasteNote.BOLD, TasteNote.CHOCOLATE)
            normalized.contains("v60") || normalized.contains("filter") || normalized.contains("pour") ->
                setOf(TasteNote.BRIGHT, TasteNote.FRUITY)
            normalized.contains("cold") -> setOf(TasteNote.SMOOTH, TasteNote.CHOCOLATE)
            normalized.contains("turk") -> setOf(TasteNote.BOLD, TasteNote.SMOKY)
            normalized.contains("latte") || normalized.contains("cappuccino") || normalized.contains("flat") ->
                setOf(TasteNote.SMOOTH, TasteNote.CARAMEL)
            else -> emptySet()
        }
    }

    private fun normalizeBrewType(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return null
        val normalized = normalizeToken(value)
        return when {
            "v60" in normalized -> "V60"
            "pour" in normalized -> "Pour Over"
            "filter" in normalized || "filtre" in normalized -> "Filter"
            "espresso" in normalized -> "Espresso"
            "cold brew" in normalized || "cold" in normalized || "soguk" in normalized -> "Cold Brew"
            "turk" in normalized -> "Turkish Coffee"
            "latte" in normalized -> "Latte"
            "cappuccino" in normalized -> "Cappuccino"
            else -> value
        }
    }

    private fun extractOrigins(events: List<CoffeeMeet>): Map<String, String> {
        val joinedText = events.joinToString(" ") { event ->
            listOfNotNull(
                event.title,
                event.description,
                event.purpose,
                event.locationName,
                event.brewingType
            ).joinToString(" ")
        }
        val normalizedText = normalizeToken(joinedText)
        return buildMap {
            knownOrigins.forEach { (token, display) ->
                if (normalizedText.contains(token)) {
                    put(normalizeToken(display), display)
                }
            }
        }
    }

    private fun extractCityOrArea(raw: String): String? {
        val value = raw.trim()
        if (value.isBlank()) return null

        val commaParts = value
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (commaParts.size >= 2) {
            return commaParts.last()
        }

        val normalized = normalizeToken(value)
        knownCityOrArea.forEach { (token, display) ->
            if (normalized.contains(token)) return display
        }

        return null
    }

    private fun readableDisplayNameFromUserId(userId: String): String? {
        val raw = userId.trim()
        if (raw.isBlank()) return null
        if ('@' in raw) {
            val localPart = raw.substringBefore('@')
            return localPart
                .replace(Regex("[._-]+"), " ")
                .trim()
                .takeIf { it.length >= 2 }
        }
        if (raw.contains(' ') && raw.length <= 40) {
            return raw
        }
        return null
    }

    private fun normalizeToken(value: String): String {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
        return normalized
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun <T> mostFrequent(values: List<T>): T? {
        if (values.isEmpty()) return null
        return values.groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }
}
