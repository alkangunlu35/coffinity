package com.icoffee.app.analytics

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

class FirebaseAnalyticsTracker(
    private val firebaseAnalytics: FirebaseAnalytics
) : AnalyticsTracker {

    override fun logEvent(eventName: String, params: Map<String, String?>) {
        runCatching {
            val bundle = Bundle()
            params.forEach { (key, value) ->
                val normalizedKey = key.trim()
                val normalizedValue = value?.trim()
                if (normalizedKey.isNotBlank() && !normalizedValue.isNullOrBlank()) {
                    bundle.putString(normalizedKey, normalizedValue)
                }
            }
            firebaseAnalytics.logEvent(eventName.trim(), bundle)
        }.onFailure { error ->
            Log.e("ANALYTICS_ERROR", "event=$eventName message=${error.message}", error)
        }
    }
}
