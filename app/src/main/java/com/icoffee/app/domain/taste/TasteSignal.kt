package com.icoffee.app.domain.taste

enum class TasteSignalDimension {
    NOTE,
    ROAST,
    ACIDITY,
    BODY,
    ORIGIN,
    COFFEE_TYPE,
    BREW_STYLE,
    MILK_TENDENCY
}

enum class TasteSignalSource {
    SCAN,
    FAVORITE,
    MENU_PICK,
    PROFILE_METADATA
}

data class TasteSignal(
    val dimension: TasteSignalDimension,
    val key: String,
    val weight: Int,
    val source: TasteSignalSource
)
