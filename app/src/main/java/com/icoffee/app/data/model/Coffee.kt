package com.icoffee.app.data.model

import androidx.annotation.ArrayRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.icoffee.app.R

enum class CoffeeCategory(val label: String) {
    HOT("Hot"),
    COLD("Cold"),
    ESPRESSO("Espresso"),
    MILK("Milk")
}

enum class CoffeeMood(@StringRes val labelRes: Int) {
    SMOOTH(R.string.coffee_mood_smooth),
    CHOCOLATEY(R.string.coffee_mood_chocolatey),
    FRUITY(R.string.coffee_mood_fruity)
}

data class Coffee(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @StringRes val typeRes: Int,
    val rating: Double,
    val reviewCount: Int,
    val tags: List<String>,
    @ArrayRes val notesRes: Int,
    val origin: String,
    val roast: String,
    val altitude: String,
    val category: CoffeeCategory,
    val mood: CoffeeMood,
    @field:DrawableRes val imageRes: Int,
    @StringRes val descriptionRes: Int
)
