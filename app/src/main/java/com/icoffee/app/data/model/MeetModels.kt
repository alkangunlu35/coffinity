package com.icoffee.app.data.model

import androidx.annotation.StringRes
import com.icoffee.app.R

enum class MeetMood(@StringRes val labelRes: Int) {
    CHILL(R.string.meet_mood_chill),
    PRODUCTIVE(R.string.meet_mood_productive),
    DEEP_TALK(R.string.meet_mood_deep_talk),
    NETWORKING(R.string.meet_mood_networking)
}

enum class CoffeeBuddySignalType {
    SAME_CITY,
    SAME_MOOD,
    TASTE_SIMILARITY,
    SHARED_BREW,
    SHARED_ORIGIN,
    SHARED_ACTIVITY
}

data class CoffeeBuddySignal(
    val type: CoffeeBuddySignalType,
    val detail: String? = null
)

data class CoffeeBuddyCandidate(
    val userId: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val cityOrArea: String? = null,
    val score: Int,
    val sharedSignals: List<CoffeeBuddySignal>,
    val socialActivityCount: Int,
    val eventId: String? = null
)

data class CoffeeBuddyDiscoveryResult(
    val mood: MeetMood,
    val discoverableUserCount: Int,
    val candidates: List<CoffeeBuddyCandidate>,
    val profileReady: Boolean
)
