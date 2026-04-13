package com.icoffee.app.data

import android.content.Context
import android.content.SharedPreferences
import com.icoffee.app.data.model.MoodType
import com.icoffee.app.data.model.RecommendationItem
import com.icoffee.app.data.model.RecommendationTrait

object UserPreferenceMemory {
    private const val PREFS_NAME = "coffinity_recommendation_memory"
    private const val KEY_LAST_MOOD = "last_mood"
    private const val KEY_RECENT_RECOMMENDATIONS = "recent_recommendations"
    private const val MAX_RECENT = 10

    private lateinit var prefs: SharedPreferences

    fun initialize(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun lastSelectedMood(): MoodType? {
        val raw = prefs.getString(KEY_LAST_MOOD, null) ?: return null
        return MoodType.entries.firstOrNull { it.name == raw }
    }

    fun recordMoodSelection(mood: MoodType) {
        val moodKey = "mood_count_${mood.name.lowercase()}"
        prefs.edit()
            .putString(KEY_LAST_MOOD, mood.name)
            .putInt(moodKey, prefs.getInt(moodKey, 0) + 1)
            .apply()
    }

    fun recentShownIds(): List<String> {
        val raw = prefs.getString(KEY_RECENT_RECOMMENDATIONS, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("|").map { it.trim() }.filter { it.isNotBlank() }
    }

    fun recordRecommendationsShown(ids: List<String>) {
        if (ids.isEmpty()) return
        val merged = (ids + recentShownIds())
            .distinct()
            .take(MAX_RECENT)
        prefs.edit().putString(KEY_RECENT_RECOMMENDATIONS, merged.joinToString("|")).apply()
    }

    fun traitInterest(trait: RecommendationTrait): Int {
        return prefs.getInt("trait_interest_${trait.name.lowercase()}", 0)
    }

    fun topTraits(limit: Int = 2): List<RecommendationTrait> {
        return RecommendationTrait.entries
            .map { it to traitInterest(it) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    fun recordRecommendationTapped(item: RecommendationItem) {
        val edit = prefs.edit()
        edit.putInt("rec_tap_${item.id}", prefs.getInt("rec_tap_${item.id}", 0) + 1)
        item.traits.forEach { trait ->
            val key = "trait_interest_${trait.name.lowercase()}"
            edit.putInt(key, prefs.getInt(key, 0) + 2)
        }
        edit.apply()
    }

    fun recordBrewingMethodOpened(methodId: String, traits: Set<RecommendationTrait>) {
        val edit = prefs.edit()
        edit.putInt("method_open_$methodId", prefs.getInt("method_open_$methodId", 0) + 1)
        traits.forEach { trait ->
            val key = "trait_interest_${trait.name.lowercase()}"
            edit.putInt(key, prefs.getInt(key, 0) + 1)
        }
        edit.apply()
    }
}
