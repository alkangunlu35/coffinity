package com.icoffee.app.analytics

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.Locale

object AnalyticsProvider {
    @Volatile
    private var trackerInstance: AnalyticsTracker = NoOpAnalyticsTracker

    val tracker: AnalyticsTracker
        get() = trackerInstance

    fun initialize(context: Context) {
        val analytics = FirebaseAnalytics.getInstance(context.applicationContext)
        trackerInstance = FirebaseAnalyticsTracker(analytics)
    }

    fun boolFlag(value: Boolean): String = if (value) "1" else "0"

    fun normalizeSource(value: String?, fallback: String = "unknown"): String {
        val normalized = value?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return if (normalized.isBlank()) fallback else normalized
    }

    fun normalizeFailureReason(raw: String?): String {
        val normalized = raw?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (normalized.isBlank()) return "unknown"
        return when {
            normalized.contains("permission") || normalized.contains("denied") -> "permission_denied"
            normalized.contains("not_signed_in") || normalized.contains("unauthorized") -> "not_signed_in"
            normalized.contains("already_joined") || normalized.contains("already") -> "already_joined"
            normalized.contains("event_full") || normalized.contains("full") -> "event_full"
            normalized.contains("network") || normalized.contains("timeout") -> "network_error"
            normalized.contains("invalid") -> "invalid_input"
            normalized.contains("limit") -> "limit_reached"
            normalized.contains("in_progress") || normalized.contains("in-progress") -> "in_progress"
            normalized.contains("not_found") -> "not_found"
            else -> "unknown"
        }
    }
}
