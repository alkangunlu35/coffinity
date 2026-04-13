package com.icoffee.app.data.model

data class AffiliateOffer(
    val id: String,
    val title: String,
    val subtitle: String,
    val destinationUrl: String,
    val retailerName: String,
    val priceHint: String? = null,
    val tag: AffiliateTag,
    val relatedKey: String
)

enum class AffiliateTag { BEAN, GEAR, SUBSCRIPTION }
