package com.icoffee.app.data.matching

import com.icoffee.app.data.model.AcidityLevel
import com.icoffee.app.data.model.CoffeeMatchResult
import com.icoffee.app.data.model.CoffeeProfile
import com.icoffee.app.data.model.CoffeeType
import com.icoffee.app.data.model.MatchLevel
import com.icoffee.app.data.model.RoastLevel
import com.icoffee.app.data.model.TasteNote
import com.icoffee.app.data.model.TasteInsight
import com.icoffee.app.data.model.TasteInsightSignal
import com.icoffee.app.data.model.TasteInsightState
import com.icoffee.app.data.model.UserTasteProfile

object BasicMatchScoreEngine {
    private const val MIN_INTERACTIONS_FOR_INSIGHT = 4
    private const val STRONG_SIGNAL_THRESHOLD = 4
    private const val ORIGIN_SIGNAL_THRESHOLD = 3
    private const val TYPE_SIGNAL_THRESHOLD = 3
    private const val MAX_VISIBLE_SIGNALS = 3

    fun calculate(
        coffeeProfile: CoffeeProfile,
        userProfile: UserTasteProfile
    ): CoffeeMatchResult {
        val strongPreferredNotes = userProfile.preferredNotes
            .filterValues { it >= STRONG_SIGNAL_THRESHOLD }
            .toList()
            .sortedByDescending { it.second }
            .map { it.first }
        val strongAvoidedNotes = userProfile.avoidedNotes
            .filterValues { it >= STRONG_SIGNAL_THRESHOLD }
            .toList()
            .sortedByDescending { it.second }
            .map { it.first }

        val preferredRoast = userProfile.roastPreference.topKey(STRONG_SIGNAL_THRESHOLD)
        val preferredAcidity = userProfile.acidityPreference.topKey(STRONG_SIGNAL_THRESHOLD)
        val preferredOrigin = userProfile.favoriteOrigins.topKey(ORIGIN_SIGNAL_THRESHOLD)
        val preferredType = userProfile.favoriteCoffeeTypes.topKey(TYPE_SIGNAL_THRESHOLD)

        val sharedPreferredNotes = coffeeProfile.tasteNotes
            .filter { it in strongPreferredNotes }
            .distinct()
            .take(2)
        val sharedAvoidedNotes = coffeeProfile.tasteNotes
            .filter { it in strongAvoidedNotes }
            .distinct()
            .take(2)

        val roastAligned = preferredRoast != null &&
            coffeeProfile.roastLevel != RoastLevel.UNKNOWN &&
            (coffeeProfile.roastLevel == preferredRoast || isAdjacentRoast(coffeeProfile.roastLevel, preferredRoast))
        val roastDifferent = preferredRoast != null &&
            coffeeProfile.roastLevel != RoastLevel.UNKNOWN &&
            !roastAligned

        val acidityAligned = preferredAcidity != null &&
            coffeeProfile.acidity != AcidityLevel.UNKNOWN &&
            (coffeeProfile.acidity == preferredAcidity || isAdjacentAcidity(coffeeProfile.acidity, preferredAcidity))
        val acidityDifferent = preferredAcidity != null &&
            coffeeProfile.acidity != AcidityLevel.UNKNOWN &&
            !acidityAligned

        val familiarOrigin = preferredOrigin != null &&
            !coffeeProfile.originCountry.isNullOrBlank() &&
            preferredOrigin.equals(coffeeProfile.originCountry, ignoreCase = true)

        val familiarType = preferredType != null &&
            coffeeProfile.coffeeType != CoffeeType.UNKNOWN &&
            preferredType == coffeeProfile.coffeeType

        val positiveSignals = mutableListOf<TasteInsightSignal>()
        if (sharedPreferredNotes.isNotEmpty()) {
            positiveSignals += TasteInsightSignal.SharedNotes(sharedPreferredNotes)
        }
        if (roastAligned) {
            positiveSignals += TasteInsightSignal.RoastAligned(preferredRoast!!)
        }
        if (acidityAligned) {
            positiveSignals += TasteInsightSignal.AcidityAligned(preferredAcidity!!)
        }
        if (familiarOrigin) {
            positiveSignals += TasteInsightSignal.FamiliarOrigin(preferredOrigin!!)
        }
        if (familiarType) {
            positiveSignals += TasteInsightSignal.FamiliarCoffeeType(preferredType!!)
        }

        val warningSignals = mutableListOf<TasteInsightSignal>()
        if (sharedAvoidedNotes.isNotEmpty()) {
            warningSignals += TasteInsightSignal.AvoidedNotes(sharedAvoidedNotes)
        }
        if (roastDifferent) {
            warningSignals += TasteInsightSignal.RoastDifferent(
                expected = preferredRoast!!,
                actual = coffeeProfile.roastLevel
            )
        }
        if (acidityDifferent) {
            warningSignals += TasteInsightSignal.AcidityDifferent(
                expected = preferredAcidity!!,
                actual = coffeeProfile.acidity
            )
        }

        val evidenceCount = userProfile.interactionCount +
            strongPreferredNotes.size +
            strongAvoidedNotes.size +
            listOf(preferredRoast, preferredAcidity, preferredOrigin, preferredType).count { it != null }
        val enoughData = userProfile.interactionCount >= MIN_INTERACTIONS_FOR_INSIGHT &&
            (strongPreferredNotes.isNotEmpty() ||
                strongAvoidedNotes.isNotEmpty() ||
                preferredRoast != null ||
                preferredAcidity != null ||
                preferredOrigin != null ||
                preferredType != null)

        val state = when {
            !enoughData -> TasteInsightState.NOT_ENOUGH_DATA
            warningSignals.isNotEmpty() && positiveSignals.size <= 1 -> TasteInsightState.POTENTIAL_MISMATCH
            positiveSignals.size >= 3 && warningSignals.isEmpty() -> TasteInsightState.LIKELY_ALIGNED
            positiveSignals.isNotEmpty() -> TasteInsightState.PARTIAL_MATCH
            warningSignals.isNotEmpty() -> TasteInsightState.POTENTIAL_MISMATCH
            else -> TasteInsightState.PARTIAL_MATCH
        }

        val visibleSignals = when (state) {
            TasteInsightState.NOT_ENOUGH_DATA -> listOf(TasteInsightSignal.LimitedEvidence)
            TasteInsightState.POTENTIAL_MISMATCH -> (warningSignals + positiveSignals)
                .take(MAX_VISIBLE_SIGNALS)
            else -> (positiveSignals + warningSignals).take(MAX_VISIBLE_SIGNALS)
        }

        val score = when (state) {
            TasteInsightState.NOT_ENOUGH_DATA -> 50
            TasteInsightState.PARTIAL_MATCH -> (56 + positiveSignals.size * 8 - warningSignals.size * 6)
            TasteInsightState.LIKELY_ALIGNED -> (74 + positiveSignals.size * 5 - warningSignals.size * 3)
            TasteInsightState.POTENTIAL_MISMATCH -> (36 + positiveSignals.size * 3 - warningSignals.size * 10)
        }.coerceIn(0, 100)

        val level = when (state) {
            TasteInsightState.NOT_ENOUGH_DATA -> MatchLevel.GOOD
            TasteInsightState.PARTIAL_MATCH -> MatchLevel.GOOD
            TasteInsightState.LIKELY_ALIGNED -> if (positiveSignals.size >= 4) MatchLevel.EXCELLENT else MatchLevel.GREAT
            TasteInsightState.POTENTIAL_MISMATCH -> MatchLevel.LOW
        }

        return CoffeeMatchResult(
            score = score,
            level = level,
            reason = state.name,
            insight = TasteInsight(
                state = state,
                signals = visibleSignals,
                evidenceCount = evidenceCount
            )
        )
    }

    private fun isAdjacentRoast(a: RoastLevel, b: RoastLevel): Boolean =
        (a == RoastLevel.LIGHT && b == RoastLevel.MEDIUM) ||
            (a == RoastLevel.MEDIUM && b == RoastLevel.LIGHT) ||
            (a == RoastLevel.DARK && b == RoastLevel.MEDIUM) ||
            (a == RoastLevel.MEDIUM && b == RoastLevel.DARK)

    private fun isAdjacentAcidity(a: AcidityLevel, b: AcidityLevel): Boolean =
        (a == AcidityLevel.LOW && b == AcidityLevel.MEDIUM) ||
            (a == AcidityLevel.MEDIUM && b == AcidityLevel.LOW) ||
            (a == AcidityLevel.HIGH && b == AcidityLevel.MEDIUM) ||
            (a == AcidityLevel.MEDIUM && b == AcidityLevel.HIGH)

    private fun <K> Map<K, Int>.topKey(minWeight: Int): K? =
        entries
            .filter { it.value >= minWeight }
            .maxByOrNull { it.value }
            ?.key
}
