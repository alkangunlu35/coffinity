package com.icoffee.app.data.profile

import com.icoffee.app.data.model.AcidityLevel
import com.icoffee.app.data.model.CoffeeProfile
import com.icoffee.app.data.model.CoffeeType
import com.icoffee.app.data.model.RoastLevel
import com.icoffee.app.data.model.StrengthLevel
import com.icoffee.app.data.model.TasteNote
import com.icoffee.app.data.model.UserTasteProfile
import kotlin.math.max

object UserTasteEngine {
    private const val MAP_MAX_WEIGHT = 120
    private const val MILK_SCORE_MIN = -120
    private const val MILK_SCORE_MAX = 120

    fun createDefaultProfile(): UserTasteProfile = UserTasteProfile(
        preferredNotes = emptyMap(),
        avoidedNotes = emptyMap(),
        roastPreference = emptyMap(),
        strengthPreference = emptyMap(),
        acidityPreference = emptyMap(),
        milkFriendlyPreferenceScore = 0,
        favoriteOrigins = emptyMap(),
        favoriteCoffeeTypes = emptyMap(),
        eventPurposePreference = emptyMap(),
        interactionCount = 0,
        lastUpdated = now()
    )

    fun applyEvent(
        current: UserTasteProfile,
        event: UserTasteEvent
    ): UserTasteProfile {
        val notes = current.preferredNotes.toMutableMap()
        val avoidedNotes = current.avoidedNotes.toMutableMap()
        val roasts = current.roastPreference.toMutableMap()
        val strengths = current.strengthPreference.toMutableMap()
        val acidities = current.acidityPreference.toMutableMap()
        val origins = current.favoriteOrigins.toMutableMap()
        val types = current.favoriteCoffeeTypes.toMutableMap()
        val purposes = current.eventPurposePreference.toMutableMap()
        var milkScore = current.milkFriendlyPreferenceScore
        var interactions = current.interactionCount

        when (event) {
            is UserTasteEvent.OnboardingCompleted -> {
                applyOnboardingSeed(
                    event = event,
                    notes = notes,
                    roasts = roasts,
                    strengths = strengths,
                    acidities = acidities
                )
                event.prefersMilkDrinks?.let { milk ->
                    milkScore += if (milk) 8 else -6
                }
                interactions += 1
            }

            is UserTasteEvent.ProductScanned -> {
                applyCoffeeSignal(
                    coffeeProfile = event.coffeeProfile,
                    weight = 1,
                    notes = notes,
                    avoidedNotes = avoidedNotes,
                    roasts = roasts,
                    strengths = strengths,
                    acidities = acidities,
                    origins = origins,
                    types = types
                )
                milkScore += milkDelta(event.coffeeProfile, 1)
                interactions += 1
            }

            is UserTasteEvent.ProductFavorited -> {
                applyCoffeeSignal(
                    coffeeProfile = event.coffeeProfile,
                    weight = 4,
                    notes = notes,
                    avoidedNotes = avoidedNotes,
                    roasts = roasts,
                    strengths = strengths,
                    acidities = acidities,
                    origins = origins,
                    types = types
                )
                milkScore += milkDelta(event.coffeeProfile, 3)
                interactions += 1
            }

            is UserTasteEvent.ProductUnfavorited -> {
                applyCoffeeSignal(
                    coffeeProfile = event.coffeeProfile,
                    weight = -4,
                    notes = notes,
                    avoidedNotes = avoidedNotes,
                    roasts = roasts,
                    strengths = strengths,
                    acidities = acidities,
                    origins = origins,
                    types = types
                )
                milkScore += milkDelta(event.coffeeProfile, -3)
                interactions += 1
            }

            is UserTasteEvent.QuickReaction -> {
                applyQuickReaction(
                    coffeeProfile = event.coffeeProfile,
                    reaction = event.reaction,
                    notes = notes,
                    avoidedNotes = avoidedNotes,
                    roasts = roasts,
                    strengths = strengths,
                    acidities = acidities,
                    origins = origins,
                    types = types
                )
                milkScore += reactionMilkDelta(event.reaction, event.coffeeProfile)
                interactions += 1
            }

            is UserTasteEvent.EventJoined -> {
                val key = event.purpose.trim()
                if (key.isNotBlank()) purposes.bump(key, 1)
                interactions += 1
            }

            is UserTasteEvent.EventCreated -> {
                val key = event.purpose.trim()
                if (key.isNotBlank()) purposes.bump(key, 2)
                interactions += 1
            }

            is UserTasteEvent.MenuItemViewed -> {
                applyCoffeeSignal(
                    coffeeProfile = event.coffeeProfile,
                    weight = 1,
                    notes = notes,
                    avoidedNotes = avoidedNotes,
                    roasts = roasts,
                    strengths = strengths,
                    acidities = acidities,
                    origins = origins,
                    types = types
                )
                milkScore += milkDelta(event.coffeeProfile, 1)
                interactions += 1
            }

            is UserTasteEvent.MenuItemFavorited -> {
                applyCoffeeSignal(
                    coffeeProfile = event.coffeeProfile,
                    weight = 3,
                    notes = notes,
                    avoidedNotes = avoidedNotes,
                    roasts = roasts,
                    strengths = strengths,
                    acidities = acidities,
                    origins = origins,
                    types = types
                )
                milkScore += milkDelta(event.coffeeProfile, 2)
                interactions += 1
            }
        }

        return UserTasteProfile(
            preferredNotes = notes.cleanMap(),
            avoidedNotes = avoidedNotes.cleanMap(),
            roastPreference = roasts.cleanMap(),
            strengthPreference = strengths.cleanMap(),
            acidityPreference = acidities.cleanMap(),
            milkFriendlyPreferenceScore = milkScore.coerceIn(MILK_SCORE_MIN, MILK_SCORE_MAX),
            favoriteOrigins = origins.cleanMap(),
            favoriteCoffeeTypes = types.cleanMap(),
            eventPurposePreference = purposes.cleanMap(),
            interactionCount = max(0, interactions),
            lastUpdated = now()
        )
    }

    private fun applyOnboardingSeed(
        event: UserTasteEvent.OnboardingCompleted,
        notes: MutableMap<TasteNote, Int>,
        roasts: MutableMap<RoastLevel, Int>,
        strengths: MutableMap<StrengthLevel, Int>,
        acidities: MutableMap<AcidityLevel, Int>
    ) {
        event.flavorStyles.forEach { style ->
            when (style) {
                OnboardingFlavorStyle.CHOCOLATEY -> {
                    notes.bump(TasteNote.CHOCOLATE, 3)
                    notes.bump(TasteNote.NUTTY, 2)
                    notes.bump(TasteNote.SMOOTH, 2)
                }
                OnboardingFlavorStyle.FRUITY -> {
                    notes.bump(TasteNote.FRUITY, 3)
                    notes.bump(TasteNote.BRIGHT, 2)
                }
                OnboardingFlavorStyle.FLORAL -> {
                    notes.bump(TasteNote.FLORAL, 3)
                    notes.bump(TasteNote.BRIGHT, 1)
                }
                OnboardingFlavorStyle.BOLD -> {
                    notes.bump(TasteNote.BOLD, 3)
                    notes.bump(TasteNote.SMOKY, 2)
                    roasts.bump(RoastLevel.DARK, 2)
                    strengths.bump(StrengthLevel.HIGH, 2)
                }
                OnboardingFlavorStyle.CARAMEL -> {
                    notes.bump(TasteNote.CARAMEL, 3)
                    notes.bump(TasteNote.SMOOTH, 2)
                }
                OnboardingFlavorStyle.SMOOTH -> {
                    notes.bump(TasteNote.SMOOTH, 3)
                    notes.bump(TasteNote.CHOCOLATE, 1)
                }
            }
        }

        event.preferredRoast?.let { if (it != RoastLevel.UNKNOWN) roasts.bump(it, 3) }
        event.preferredStrength?.let { if (it != StrengthLevel.UNKNOWN) strengths.bump(it, 3) }
        event.preferredAcidity?.let { if (it != AcidityLevel.UNKNOWN) acidities.bump(it, 3) }
    }

    private fun applyQuickReaction(
        coffeeProfile: CoffeeProfile,
        reaction: TasteReaction,
        notes: MutableMap<TasteNote, Int>,
        avoidedNotes: MutableMap<TasteNote, Int>,
        roasts: MutableMap<RoastLevel, Int>,
        strengths: MutableMap<StrengthLevel, Int>,
        acidities: MutableMap<AcidityLevel, Int>,
        origins: MutableMap<String, Int>,
        types: MutableMap<CoffeeType, Int>
    ) {
        when (reaction) {
            TasteReaction.LOVED_IT -> applyCoffeeSignal(
                coffeeProfile = coffeeProfile,
                weight = 5,
                notes = notes,
                avoidedNotes = avoidedNotes,
                roasts = roasts,
                strengths = strengths,
                acidities = acidities,
                origins = origins,
                types = types
            )

            TasteReaction.TOO_BITTER -> {
                roasts.bump(RoastLevel.DARK, -4)
                strengths.bump(StrengthLevel.HIGH, -4)
                notes.bump(TasteNote.BOLD, -3)
                notes.bump(TasteNote.SMOKY, -2)
                avoidedNotes.bump(TasteNote.BOLD, 3)
                avoidedNotes.bump(TasteNote.SMOKY, 2)
                roasts.bump(RoastLevel.MEDIUM, 2)
                strengths.bump(StrengthLevel.MEDIUM, 2)
            }

            TasteReaction.TOO_ACIDIC -> {
                acidities.bump(AcidityLevel.HIGH, -4)
                acidities.bump(AcidityLevel.MEDIUM, 2)
                acidities.bump(AcidityLevel.LOW, 2)
                notes.bump(TasteNote.BRIGHT, -2)
                avoidedNotes.bump(TasteNote.BRIGHT, 3)
                avoidedNotes.bump(TasteNote.FRUITY, 1)
            }

            TasteReaction.TOO_WEAK -> {
                strengths.bump(StrengthLevel.LOW, -3)
                strengths.bump(StrengthLevel.MEDIUM, 2)
                strengths.bump(StrengthLevel.HIGH, 3)
                notes.bump(TasteNote.BOLD, 1)
                avoidedNotes.bump(TasteNote.BOLD, -1)
            }

            TasteReaction.TOO_STRONG -> {
                strengths.bump(StrengthLevel.HIGH, -4)
                strengths.bump(StrengthLevel.MEDIUM, 2)
                strengths.bump(StrengthLevel.LOW, 2)
                roasts.bump(RoastLevel.DARK, -2)
                roasts.bump(RoastLevel.MEDIUM, 1)
                avoidedNotes.bump(TasteNote.BOLD, 2)
            }

            TasteReaction.TOO_SWEET -> {
                notes.bump(TasteNote.CARAMEL, -2)
                notes.bump(TasteNote.SMOOTH, -1)
                strengths.bump(StrengthLevel.MEDIUM, 1)
                avoidedNotes.bump(TasteNote.CARAMEL, 2)
            }

            TasteReaction.NOT_FOR_ME -> applyCoffeeSignal(
                coffeeProfile = coffeeProfile,
                weight = -2,
                notes = notes,
                avoidedNotes = avoidedNotes,
                roasts = roasts,
                strengths = strengths,
                acidities = acidities,
                origins = origins,
                types = types
            )
        }
    }

    private fun applyCoffeeSignal(
        coffeeProfile: CoffeeProfile,
        weight: Int,
        notes: MutableMap<TasteNote, Int>,
        avoidedNotes: MutableMap<TasteNote, Int>,
        roasts: MutableMap<RoastLevel, Int>,
        strengths: MutableMap<StrengthLevel, Int>,
        acidities: MutableMap<AcidityLevel, Int>,
        origins: MutableMap<String, Int>,
        types: MutableMap<CoffeeType, Int>
    ) {
        if (weight >= 0) {
            coffeeProfile.tasteNotes.forEach { note ->
                notes.bump(note, weight)
                avoidedNotes.bump(note, -weight)
            }
        } else {
            val inverse = -weight
            coffeeProfile.tasteNotes.forEach { note ->
                notes.bump(note, weight)
                avoidedNotes.bump(note, inverse)
            }
        }
        if (coffeeProfile.roastLevel != RoastLevel.UNKNOWN) {
            roasts.bump(coffeeProfile.roastLevel, weight)
        }
        if (coffeeProfile.strength != StrengthLevel.UNKNOWN) {
            strengths.bump(coffeeProfile.strength, weight)
        }
        if (coffeeProfile.acidity != AcidityLevel.UNKNOWN) {
            acidities.bump(coffeeProfile.acidity, weight)
        }
        val origin = coffeeProfile.originCountry?.trim().orEmpty()
        if (origin.isNotBlank()) origins.bump(origin, weight)
        if (coffeeProfile.coffeeType != CoffeeType.UNKNOWN) {
            types.bump(coffeeProfile.coffeeType, weight)
        }
    }

    private fun reactionMilkDelta(
        reaction: TasteReaction,
        coffeeProfile: CoffeeProfile
    ): Int = when (reaction) {
        TasteReaction.LOVED_IT -> milkDelta(coffeeProfile, 3)
        TasteReaction.TOO_SWEET -> -3
        TasteReaction.NOT_FOR_ME -> milkDelta(coffeeProfile, -2)
        TasteReaction.TOO_BITTER -> if (coffeeProfile.milkFriendly) 2 else 0
        TasteReaction.TOO_ACIDIC -> -1
        TasteReaction.TOO_WEAK -> if (coffeeProfile.milkFriendly) -1 else 0
        TasteReaction.TOO_STRONG -> if (coffeeProfile.milkFriendly) 2 else 0
    }

    private fun milkDelta(profile: CoffeeProfile, weight: Int): Int =
        if (profile.milkFriendly) weight else -weight

    private fun <K> MutableMap<K, Int>.bump(key: K, delta: Int) {
        val newValue = (this[key] ?: 0) + delta
        if (newValue <= 0) {
            remove(key)
        } else {
            this[key] = newValue.coerceAtMost(MAP_MAX_WEIGHT)
        }
    }

    private fun <K> MutableMap<K, Int>.cleanMap(): Map<K, Int> =
        entries
            .filter { it.value > 0 }
            .associate { it.key to it.value.coerceAtMost(MAP_MAX_WEIGHT) }

    private fun now(): Long = System.currentTimeMillis()
}
