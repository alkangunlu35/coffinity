package com.icoffee.app.data.model

data class CoffeeProfile(
    val barcode: String,
    val productName: String,
    val brand: String?,
    val imageUrl: String?,
    val coffeeType: CoffeeType,
    val roastLevel: RoastLevel,
    val originCountry: String?,
    val tasteNotes: List<TasteNote>,
    val strength: StrengthLevel,
    val acidity: AcidityLevel,
    val milkFriendly: Boolean,
    val confidenceScore: Int,
    val source: String = "Open Food Facts"
)

enum class CoffeeType {
    WHOLE_BEAN,
    GROUND,
    INSTANT,
    CAPSULE,
    READY_TO_DRINK,
    UNKNOWN
}

enum class RoastLevel {
    LIGHT,
    MEDIUM,
    DARK,
    UNKNOWN
}

enum class TasteNote {
    CHOCOLATE,
    NUTTY,
    FRUITY,
    FLORAL,
    CARAMEL,
    BOLD,
    SMOOTH,
    BRIGHT,
    SMOKY
}

enum class StrengthLevel {
    LOW,
    MEDIUM,
    HIGH,
    UNKNOWN
}

enum class AcidityLevel {
    LOW,
    MEDIUM,
    HIGH,
    UNKNOWN
}
