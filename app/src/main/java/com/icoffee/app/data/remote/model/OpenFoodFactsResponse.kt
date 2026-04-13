package com.icoffee.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class OpenFoodFactsProductResponse(
    @SerializedName("code") val code: String? = null,
    @SerializedName("status") val status: Int? = null,
    @SerializedName("product") val product: OpenFoodFactsProductDto? = null
)

data class OpenFoodFactsProductDto(
    @SerializedName("product_name") val productName: String? = null,
    @SerializedName("brands") val brands: String? = null,
    @SerializedName("countries") val countries: String? = null,
    @SerializedName("categories") val categories: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("image_front_url") val imageFrontUrl: String? = null,
    @SerializedName("generic_name") val genericName: String? = null,
    @SerializedName("quantity") val quantity: String? = null,
    @SerializedName("packaging") val packaging: String? = null,
    @SerializedName("ingredients_text") val ingredientsText: String? = null,
    @SerializedName("product_quantity") val productQuantity: String? = null,
    @SerializedName("labels") val labels: String? = null,
    @SerializedName("stores") val stores: String? = null
)
