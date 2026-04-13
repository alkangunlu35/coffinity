package com.icoffee.app.data.profile

data class FavoriteScanProduct(
    val barcode: String,
    val name: String,
    val brand: String?,
    val origin: String?,
    val roast: String,
    val imageUrl: String?,
    val savedAt: Long
)

data class FavoriteBeanItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val origin: String,
    val savedAt: Long
)

data class FavoriteMenuPick(
    val id: String,
    val title: String,
    val subtitle: String,
    val savedAt: Long
)
