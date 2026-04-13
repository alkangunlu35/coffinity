package com.icoffee.app.data.model

import androidx.annotation.ArrayRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.icoffee.app.R

enum class MoodType(@StringRes val labelRes: Int) {
    RELAXED(R.string.mood_type_relaxed),
    ENERGETIC(R.string.mood_type_energetic),
    FOCUSED(R.string.mood_type_focused),
    SOCIAL(R.string.mood_type_social),
    CURIOUS(R.string.mood_type_curious),
    CALM(R.string.mood_type_calm),
    ADVENTUROUS(R.string.mood_type_adventurous),
    COZY(R.string.mood_type_cozy)
}

enum class TimeBucket {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT
}

enum class RecommendationTrait {
    MILK,
    BOLD,
    CLEAN,
    FILTER,
    COLD,
    SWEET,
    FRUITY,
    NUTTY
}

data class RecommendationItem(
    val id: String,
    val mood: MoodType,
    val coffeeId: String,
    @StringRes val coffeeNameRes: Int,
    @StringRes val brewingStyleRes: Int,
    @StringRes val reasonRes: Int,
    val reason: String? = null,
    @field:DrawableRes val imageRes: Int,
    @ArrayRes val tagsRes: Int,
    val methodId: String,
    val rating: Double,
    val preferredTimes: Set<TimeBucket> = TimeBucket.entries.toSet(),
    val moodAffinity: Int = 1,
    val traits: Set<RecommendationTrait> = emptySet()
)

data class HomeMachineTip(
    val machineType: String,
    val settings: List<String>,
    val bestFor: String
)

data class BrewingMethod(
    val id: String,
    val category: String,
    val title: String,
    val cardSubtitle: String,
    val summary: String,
    val howItWorks: String,
    val brewTime: String,
    val tasteProfile: List<String>,
    val brewCharacteristics: List<String>,
    @field:DrawableRes val imageRes: Int,
    val howToBrew: List<String>,
    val homeMachineTips: List<HomeMachineTip>,
    val homeMachineNote: String? = null,
    val bestFor: List<String>
)
