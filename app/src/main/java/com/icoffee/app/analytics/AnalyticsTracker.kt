package com.icoffee.app.analytics

interface AnalyticsTracker {
    fun logEvent(eventName: String, params: Map<String, String?> = emptyMap())
}

object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun logEvent(eventName: String, params: Map<String, String?>) = Unit
}
