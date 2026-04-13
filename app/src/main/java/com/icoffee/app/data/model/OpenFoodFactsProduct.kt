package com.icoffee.app.data.model

data class OpenFoodFactsProduct(
    val barcode: String,
    val name: String,
    val brand: String?,
    val countries: String?,
    val categories: String?,
    val imageUrl: String?,
    val genericName: String?,
    val quantity: String?,
    val packaging: String?,
    val ingredientsText: String?,
    val labels: String?,
    val stores: String?
)
