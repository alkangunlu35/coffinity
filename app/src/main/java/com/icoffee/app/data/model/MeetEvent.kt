package com.icoffee.app.data.model

enum class EventHostType {
    PERSONAL,
    BUSINESS
}

enum class MeetEventType {
    COMMUNITY,
    BUSINESS
}

enum class OfferPaymentMode {
    PAY_AT_VENUE,
    INCLUDED_WITH_ATTENDANCE
}

data class IncludedOfferItem(
    val quantity: Int,
    val label: String
)

data class BusinessOffer(
    val offerTitle: String,
    val offerDescription: String? = null,
    val includedItems: List<IncludedOfferItem>,
    val priceAmount: Double? = null,
    val currency: String? = null,
    val paymentMode: OfferPaymentMode = OfferPaymentMode.PAY_AT_VENUE,
    val availabilityLimit: Int? = null,
    val termsNote: String? = null
)

data class CoffeeMeet(
    val id: String,
    val title: String,
    val description: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val scheduledAt: Long = 0L,     // epoch millis; 0 = unknown/legacy
    val time: String,               // formatted display label
    val purpose: String,
    val participants: List<String>,
    val maxParticipants: Int,
    val hostId: String,
    val hostUserType: UserType? = null,
    val hostType: EventHostType = if (hostUserType == UserType.BUSINESS) {
        EventHostType.BUSINESS
    } else {
        EventHostType.PERSONAL
    },
    val eventType: MeetEventType = if (hostType == EventHostType.BUSINESS) {
        MeetEventType.BUSINESS
    } else {
        MeetEventType.COMMUNITY
    },
    val brewingType: String? = null,
    val businessOffer: BusinessOffer? = null,
    val distanceLabel: String = "",
    val isCreatedByUser: Boolean = false
)
