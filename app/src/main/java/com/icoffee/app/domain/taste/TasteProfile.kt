package com.icoffee.app.domain.taste

import com.icoffee.app.data.model.AcidityLevel
import com.icoffee.app.data.model.CoffeeType
import com.icoffee.app.data.model.RoastLevel
import com.icoffee.app.data.model.StrengthLevel
import com.icoffee.app.data.model.TasteNote

enum class BrewStylePreference {
    ESPRESSO_BASED,
    FILTER,
    COLD_BREW,
    TURKISH,
    MILK_BASED,
    INSTANT
}

enum class TasteDataState {
    NOT_ENOUGH_DATA,
    LEARNING,
    READY
}

data class TasteProfile(
    val state: TasteDataState,
    val analyzedItemsCount: Int,
    val totalSignalWeight: Int,
    val strongestNotes: List<TasteNote>,
    val roastPreference: RoastLevel?,
    val acidityTendency: AcidityLevel?,
    val bodyTendency: StrengthLevel?,
    val milkTendency: Boolean?,
    val topCoffeeTypes: List<CoffeeType>,
    val topOrigins: List<String>,
    val topBrewStyles: List<BrewStylePreference>,
    val signals: List<TasteSignal>
) {
    val hasEnoughData: Boolean
        get() = state != TasteDataState.NOT_ENOUGH_DATA
}
