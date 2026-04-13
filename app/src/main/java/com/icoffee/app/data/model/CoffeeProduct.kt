package com.icoffee.app.data.model

import androidx.annotation.DrawableRes

data class CoffeeProduct(
    val barcode: String,
    val name: String,
    val brand: String,
    val origin: String,
    val roast: String,
    val type: String,
    val notes: List<String>,
    val recommendation: String,
    val strength: String = "",
    val profile: String? = null,
    val matchScore: Int? = null,
    @field:DrawableRes val imageRes: Int? = null
)
