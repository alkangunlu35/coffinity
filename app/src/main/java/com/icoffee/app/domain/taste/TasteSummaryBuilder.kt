package com.icoffee.app.domain.taste

import com.icoffee.app.data.model.AcidityLevel
import com.icoffee.app.data.model.CoffeeType
import com.icoffee.app.data.model.RoastLevel
import com.icoffee.app.data.model.StrengthLevel
import com.icoffee.app.data.model.TasteNote

data class TasteSummary(
    val hasEnoughData: Boolean,
    val analyzedItemsCount: Int,
    val strongestNotes: List<TasteNote>,
    val roastPreference: RoastLevel?,
    val acidityTendency: AcidityLevel?,
    val bodyTendency: StrengthLevel?,
    val milkTendency: Boolean?,
    val topBrewStyles: List<BrewStylePreference>,
    val topOrigins: List<String>,
    val topCoffeeTypes: List<CoffeeType>
)

object TasteSummaryBuilder {
    fun build(profile: TasteProfile): TasteSummary = TasteSummary(
        hasEnoughData = profile.hasEnoughData,
        analyzedItemsCount = profile.analyzedItemsCount,
        strongestNotes = profile.strongestNotes,
        roastPreference = profile.roastPreference,
        acidityTendency = profile.acidityTendency,
        bodyTendency = profile.bodyTendency,
        milkTendency = profile.milkTendency,
        topBrewStyles = profile.topBrewStyles,
        topOrigins = profile.topOrigins,
        topCoffeeTypes = profile.topCoffeeTypes
    )

    fun empty(): TasteSummary = TasteSummary(
        hasEnoughData = false,
        analyzedItemsCount = 0,
        strongestNotes = emptyList(),
        roastPreference = null,
        acidityTendency = null,
        bodyTendency = null,
        milkTendency = null,
        topBrewStyles = emptyList(),
        topOrigins = emptyList(),
        topCoffeeTypes = emptyList()
    )
}
