package com.icoffee.app.data.model

data class RankedMeetEvent(
    val meet: CoffeeMeet,
    val score: Double,
    val distanceKm: Double,
    val distanceScore: Double,
    val timeScore: Double,
    val moodScore: Double,
    val popularityScore: Double,
    val urgencyScore: Double
)

enum class MeetDiscoverySectionType {
    SMART_PICKS,
    NEAR_YOU,
    HAPPENING_SOON,
    MOOD_BASED
}

data class MeetDiscoverySection(
    val id: String,
    val type: MeetDiscoverySectionType,
    val events: List<CoffeeMeet>,
    val mood: MeetMood? = null
)

enum class MeetExploreSort {
    RELEVANCE,
    DISTANCE,
    SOONEST
}
