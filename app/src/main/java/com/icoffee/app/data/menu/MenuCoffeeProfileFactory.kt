package com.icoffee.app.data.menu

import com.icoffee.app.data.model.AcidityLevel
import com.icoffee.app.data.model.CoffeeProfile
import com.icoffee.app.data.model.CoffeeType
import com.icoffee.app.data.model.NormalizedCoffeeType
import com.icoffee.app.data.model.RoastLevel
import com.icoffee.app.data.model.StrengthLevel
import com.icoffee.app.data.model.TasteNote

object MenuCoffeeProfileFactory {

    fun createProfile(
        type: NormalizedCoffeeType,
        confidence: Int,
        scanId: String,
        venueHint: String?
    ): CoffeeProfile {
        val base = baseProfile(type)
        return CoffeeProfile(
            barcode = "menu:$scanId:${type.name.lowercase()}",
            productName = type.displayName(),
            brand = venueHint,
            imageUrl = null,
            coffeeType = base.coffeeType,
            roastLevel = base.roastLevel,
            originCountry = null,
            tasteNotes = base.tasteNotes,
            strength = base.strength,
            acidity = base.acidity,
            milkFriendly = base.milkFriendly,
            confidenceScore = confidence.coerceIn(20, 99),
            source = "Menu OCR"
        )
    }

    private fun baseProfile(type: NormalizedCoffeeType): BaseProfile = when (type) {
        NormalizedCoffeeType.ESPRESSO -> BaseProfile(
            coffeeType = CoffeeType.GROUND,
            roastLevel = RoastLevel.DARK,
            tasteNotes = listOf(TasteNote.BOLD, TasteNote.SMOKY),
            strength = StrengthLevel.HIGH,
            acidity = AcidityLevel.LOW,
            milkFriendly = false
        )
        NormalizedCoffeeType.AMERICANO -> BaseProfile(
            coffeeType = CoffeeType.GROUND,
            roastLevel = RoastLevel.DARK,
            tasteNotes = listOf(TasteNote.BOLD, TasteNote.SMOOTH),
            strength = StrengthLevel.MEDIUM,
            acidity = AcidityLevel.LOW,
            milkFriendly = false
        )
        NormalizedCoffeeType.CAPPUCCINO -> BaseProfile(
            coffeeType = CoffeeType.GROUND,
            roastLevel = RoastLevel.MEDIUM,
            tasteNotes = listOf(TasteNote.CARAMEL, TasteNote.SMOOTH),
            strength = StrengthLevel.MEDIUM,
            acidity = AcidityLevel.LOW,
            milkFriendly = true
        )
        NormalizedCoffeeType.LATTE -> BaseProfile(
            coffeeType = CoffeeType.GROUND,
            roastLevel = RoastLevel.MEDIUM,
            tasteNotes = listOf(TasteNote.SMOOTH, TasteNote.CARAMEL),
            strength = StrengthLevel.MEDIUM,
            acidity = AcidityLevel.LOW,
            milkFriendly = true
        )
        NormalizedCoffeeType.FLAT_WHITE -> BaseProfile(
            coffeeType = CoffeeType.GROUND,
            roastLevel = RoastLevel.MEDIUM,
            tasteNotes = listOf(TasteNote.SMOOTH, TasteNote.CHOCOLATE),
            strength = StrengthLevel.MEDIUM,
            acidity = AcidityLevel.LOW,
            milkFriendly = true
        )
        NormalizedCoffeeType.MACCHIATO -> BaseProfile(
            coffeeType = CoffeeType.GROUND,
            roastLevel = RoastLevel.DARK,
            tasteNotes = listOf(TasteNote.BOLD, TasteNote.CARAMEL),
            strength = StrengthLevel.HIGH,
            acidity = AcidityLevel.LOW,
            milkFriendly = true
        )
        NormalizedCoffeeType.MOCHA -> BaseProfile(
            coffeeType = CoffeeType.GROUND,
            roastLevel = RoastLevel.MEDIUM,
            tasteNotes = listOf(TasteNote.CHOCOLATE, TasteNote.CARAMEL),
            strength = StrengthLevel.MEDIUM,
            acidity = AcidityLevel.LOW,
            milkFriendly = true
        )
        NormalizedCoffeeType.CORTADO -> BaseProfile(
            coffeeType = CoffeeType.GROUND,
            roastLevel = RoastLevel.DARK,
            tasteNotes = listOf(TasteNote.BOLD, TasteNote.SMOOTH),
            strength = StrengthLevel.HIGH,
            acidity = AcidityLevel.LOW,
            milkFriendly = true
        )
        NormalizedCoffeeType.RISTRETTO -> BaseProfile(
            coffeeType = CoffeeType.GROUND,
            roastLevel = RoastLevel.DARK,
            tasteNotes = listOf(TasteNote.BOLD, TasteNote.SMOKY),
            strength = StrengthLevel.HIGH,
            acidity = AcidityLevel.LOW,
            milkFriendly = false
        )
        NormalizedCoffeeType.LUNGO -> BaseProfile(
            coffeeType = CoffeeType.GROUND,
            roastLevel = RoastLevel.DARK,
            tasteNotes = listOf(TasteNote.BOLD, TasteNote.SMOOTH),
            strength = StrengthLevel.MEDIUM,
            acidity = AcidityLevel.MEDIUM,
            milkFriendly = false
        )
        NormalizedCoffeeType.FILTER_V60 -> BaseProfile(
            coffeeType = CoffeeType.GROUND,
            roastLevel = RoastLevel.LIGHT,
            tasteNotes = listOf(TasteNote.BRIGHT, TasteNote.FRUITY, TasteNote.FLORAL),
            strength = StrengthLevel.LOW,
            acidity = AcidityLevel.HIGH,
            milkFriendly = false
        )
        NormalizedCoffeeType.POUR_OVER -> BaseProfile(
            coffeeType = CoffeeType.GROUND,
            roastLevel = RoastLevel.LIGHT,
            tasteNotes = listOf(TasteNote.BRIGHT, TasteNote.FRUITY),
            strength = StrengthLevel.LOW,
            acidity = AcidityLevel.HIGH,
            milkFriendly = false
        )
        NormalizedCoffeeType.CHEMEX -> BaseProfile(
            coffeeType = CoffeeType.GROUND,
            roastLevel = RoastLevel.LIGHT,
            tasteNotes = listOf(TasteNote.FRUITY, TasteNote.BRIGHT, TasteNote.SMOOTH),
            strength = StrengthLevel.LOW,
            acidity = AcidityLevel.MEDIUM,
            milkFriendly = false
        )
        NormalizedCoffeeType.AEROPRESS -> BaseProfile(
            coffeeType = CoffeeType.GROUND,
            roastLevel = RoastLevel.MEDIUM,
            tasteNotes = listOf(TasteNote.SMOOTH, TasteNote.NUTTY),
            strength = StrengthLevel.MEDIUM,
            acidity = AcidityLevel.MEDIUM,
            milkFriendly = false
        )
        NormalizedCoffeeType.COLD_BREW -> BaseProfile(
            coffeeType = CoffeeType.READY_TO_DRINK,
            roastLevel = RoastLevel.MEDIUM,
            tasteNotes = listOf(TasteNote.SMOOTH, TasteNote.CHOCOLATE),
            strength = StrengthLevel.MEDIUM,
            acidity = AcidityLevel.LOW,
            milkFriendly = true
        )
        NormalizedCoffeeType.ICED_COFFEE -> BaseProfile(
            coffeeType = CoffeeType.READY_TO_DRINK,
            roastLevel = RoastLevel.MEDIUM,
            tasteNotes = listOf(TasteNote.SMOOTH, TasteNote.CARAMEL),
            strength = StrengthLevel.MEDIUM,
            acidity = AcidityLevel.LOW,
            milkFriendly = true
        )
        NormalizedCoffeeType.TURKISH_COFFEE -> BaseProfile(
            coffeeType = CoffeeType.GROUND,
            roastLevel = RoastLevel.DARK,
            tasteNotes = listOf(TasteNote.BOLD, TasteNote.SMOKY),
            strength = StrengthLevel.HIGH,
            acidity = AcidityLevel.LOW,
            milkFriendly = false
        )
        NormalizedCoffeeType.FILTER_COFFEE -> BaseProfile(
            coffeeType = CoffeeType.GROUND,
            roastLevel = RoastLevel.MEDIUM,
            tasteNotes = listOf(TasteNote.NUTTY, TasteNote.SMOOTH),
            strength = StrengthLevel.MEDIUM,
            acidity = AcidityLevel.MEDIUM,
            milkFriendly = false
        )
        NormalizedCoffeeType.UNKNOWN -> BaseProfile(
            coffeeType = CoffeeType.UNKNOWN,
            roastLevel = RoastLevel.UNKNOWN,
            tasteNotes = emptyList(),
            strength = StrengthLevel.UNKNOWN,
            acidity = AcidityLevel.UNKNOWN,
            milkFriendly = false
        )
    }

    private fun NormalizedCoffeeType.displayName(): String = when (this) {
        NormalizedCoffeeType.ESPRESSO -> "Espresso"
        NormalizedCoffeeType.AMERICANO -> "Americano"
        NormalizedCoffeeType.CAPPUCCINO -> "Cappuccino"
        NormalizedCoffeeType.LATTE -> "Latte"
        NormalizedCoffeeType.FLAT_WHITE -> "Flat White"
        NormalizedCoffeeType.MACCHIATO -> "Macchiato"
        NormalizedCoffeeType.MOCHA -> "Mocha"
        NormalizedCoffeeType.CORTADO -> "Cortado"
        NormalizedCoffeeType.RISTRETTO -> "Ristretto"
        NormalizedCoffeeType.LUNGO -> "Lungo"
        NormalizedCoffeeType.FILTER_V60 -> "V60"
        NormalizedCoffeeType.POUR_OVER -> "Pour Over"
        NormalizedCoffeeType.CHEMEX -> "Chemex"
        NormalizedCoffeeType.AEROPRESS -> "Aeropress"
        NormalizedCoffeeType.COLD_BREW -> "Cold Brew"
        NormalizedCoffeeType.ICED_COFFEE -> "Iced Coffee"
        NormalizedCoffeeType.TURKISH_COFFEE -> "Turkish Coffee"
        NormalizedCoffeeType.FILTER_COFFEE -> "Filter Coffee"
        NormalizedCoffeeType.UNKNOWN -> "Unknown Coffee"
    }

    private data class BaseProfile(
        val coffeeType: CoffeeType,
        val roastLevel: RoastLevel,
        val tasteNotes: List<TasteNote>,
        val strength: StrengthLevel,
        val acidity: AcidityLevel,
        val milkFriendly: Boolean
    )
}
