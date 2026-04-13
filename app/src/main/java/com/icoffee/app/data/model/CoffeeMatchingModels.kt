package com.icoffee.app.data.model

data class UserTasteProfile(
    val preferredNotes: Map<TasteNote, Int> = emptyMap(),
    val avoidedNotes: Map<TasteNote, Int> = emptyMap(),
    val roastPreference: Map<RoastLevel, Int> = emptyMap(),
    val strengthPreference: Map<StrengthLevel, Int> = emptyMap(),
    val acidityPreference: Map<AcidityLevel, Int> = emptyMap(),
    val milkFriendlyPreferenceScore: Int = 0,
    val favoriteOrigins: Map<String, Int> = emptyMap(),
    val favoriteCoffeeTypes: Map<CoffeeType, Int> = emptyMap(),
    val eventPurposePreference: Map<String, Int> = emptyMap(),
    val interactionCount: Int = 0,
    val lastUpdated: Long = 0L
)

data class UserTasteSummary(
    val topNotes: List<TasteNote>,
    val avoidedNotes: List<TasteNote>,
    val likelyRoast: RoastLevel?,
    val likelyStrength: StrengthLevel?,
    val likelyAcidity: AcidityLevel?,
    val likelyMilkFriendly: Boolean?,
    val topOrigins: List<String>,
    val topCoffeeTypes: List<CoffeeType>,
    val topEventPurposes: List<String>,
    val interactionCount: Int
)

fun UserTasteProfile.topPreferredNotes(limit: Int = 3): List<TasteNote> =
    preferredNotes.topKeys(limit)

fun UserTasteProfile.topAvoidedNotes(limit: Int = 3): List<TasteNote> =
    avoidedNotes.topKeys(limit)

fun UserTasteProfile.strongestRoastPreference(): RoastLevel? =
    roastPreference.topKey()

fun UserTasteProfile.strongestStrengthPreference(): StrengthLevel? =
    strengthPreference.topKey()

fun UserTasteProfile.strongestAcidityPreference(): AcidityLevel? =
    acidityPreference.topKey()

fun UserTasteProfile.likelyMilkFriendly(): Boolean? = when {
    milkFriendlyPreferenceScore >= 4 -> true
    milkFriendlyPreferenceScore <= -4 -> false
    else -> null
}

fun UserTasteProfile.topOrigins(limit: Int = 3): List<String> =
    favoriteOrigins.topKeys(limit)

fun UserTasteProfile.topCoffeeTypes(limit: Int = 3): List<CoffeeType> =
    favoriteCoffeeTypes.topKeys(limit)

fun UserTasteProfile.topEventPurposes(limit: Int = 3): List<String> =
    eventPurposePreference.topKeys(limit)

fun UserTasteProfile.toSummary(): UserTasteSummary =
    UserTasteSummary(
        topNotes = topPreferredNotes(limit = 3),
        avoidedNotes = topAvoidedNotes(limit = 3),
        likelyRoast = strongestRoastPreference(),
        likelyStrength = strongestStrengthPreference(),
        likelyAcidity = strongestAcidityPreference(),
        likelyMilkFriendly = likelyMilkFriendly(),
        topOrigins = topOrigins(limit = 3),
        topCoffeeTypes = topCoffeeTypes(limit = 2),
        topEventPurposes = topEventPurposes(limit = 2),
        interactionCount = interactionCount
    )

data class CoffeeMatchResult(
    val score: Int,
    val level: MatchLevel,
    val reason: String,
    val insight: TasteInsight
)

enum class MatchLevel {
    LOW,
    GOOD,
    GREAT,
    EXCELLENT
}

data class TasteInsight(
    val state: TasteInsightState,
    val signals: List<TasteInsightSignal>,
    val evidenceCount: Int
)

enum class TasteInsightState {
    NOT_ENOUGH_DATA,
    PARTIAL_MATCH,
    LIKELY_ALIGNED,
    POTENTIAL_MISMATCH
}

sealed class TasteInsightSignal {
    data class SharedNotes(val notes: List<TasteNote>) : TasteInsightSignal()
    data class AvoidedNotes(val notes: List<TasteNote>) : TasteInsightSignal()
    data class RoastAligned(val roast: RoastLevel) : TasteInsightSignal()
    data class RoastDifferent(
        val expected: RoastLevel,
        val actual: RoastLevel
    ) : TasteInsightSignal()
    data class AcidityAligned(val acidity: AcidityLevel) : TasteInsightSignal()
    data class AcidityDifferent(
        val expected: AcidityLevel,
        val actual: AcidityLevel
    ) : TasteInsightSignal()
    data class FamiliarOrigin(val origin: String) : TasteInsightSignal()
    data class FamiliarCoffeeType(val type: CoffeeType) : TasteInsightSignal()
    data object LimitedEvidence : TasteInsightSignal()
}

private fun <K> Map<K, Int>.topKey(): K? =
    entries
        .filter { it.value > 0 }
        .maxByOrNull { it.value }
        ?.key

private fun <K> Map<K, Int>.topKeys(limit: Int): List<K> =
    entries
        .asSequence()
        .filter { it.value > 0 }
        .sortedByDescending { it.value }
        .map { it.key }
        .take(limit)
        .toList()
