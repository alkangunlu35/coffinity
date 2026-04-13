package com.icoffee.app.data.profile

import com.icoffee.app.data.model.AcidityLevel
import com.icoffee.app.data.model.CoffeeProfile
import com.icoffee.app.data.model.RoastLevel
import com.icoffee.app.data.model.StrengthLevel

sealed class UserTasteEvent {
    data class OnboardingCompleted(
        val flavorStyles: List<OnboardingFlavorStyle> = emptyList(),
        val preferredRoast: RoastLevel? = null,
        val preferredStrength: StrengthLevel? = null,
        val preferredAcidity: AcidityLevel? = null,
        val prefersMilkDrinks: Boolean? = null
    ) : UserTasteEvent()

    data class ProductScanned(val coffeeProfile: CoffeeProfile) : UserTasteEvent()
    data class ProductFavorited(val coffeeProfile: CoffeeProfile) : UserTasteEvent()
    data class ProductUnfavorited(val coffeeProfile: CoffeeProfile) : UserTasteEvent()
    data class QuickReaction(
        val coffeeProfile: CoffeeProfile,
        val reaction: TasteReaction
    ) : UserTasteEvent()

    data class EventJoined(val purpose: String) : UserTasteEvent()
    data class EventCreated(val purpose: String) : UserTasteEvent()
    data class MenuItemViewed(val coffeeProfile: CoffeeProfile) : UserTasteEvent()
    data class MenuItemFavorited(val coffeeProfile: CoffeeProfile) : UserTasteEvent()
}

enum class OnboardingFlavorStyle {
    CHOCOLATEY,
    FRUITY,
    FLORAL,
    BOLD,
    CARAMEL,
    SMOOTH
}

enum class TasteReaction {
    LOVED_IT,
    TOO_BITTER,
    TOO_ACIDIC,
    TOO_WEAK,
    TOO_STRONG,
    TOO_SWEET,
    NOT_FOR_ME
}
