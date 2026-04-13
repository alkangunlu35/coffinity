package com.icoffee.app.data.model.beans

data class CountryBeans(
    val id: String,
    val country: String,
    val continent: String,
    val flagEmoji: String,
    val varieties: List<BeanVariety>
)

data class BeanVariety(
    val name: String,
    val description: String,
    val flavorNotes: List<String>,
    val processing: String? = null,
    val altitude: String? = null,
    val roast: String? = null,
    val species: String? = null,
    val recommendedBrewing: List<String> = emptyList()
)

data class CountryGroup(
    val continent: String,
    val countries: List<CountryBeans>
)
