package com.icoffee.app.data.growth

import android.util.Log

object GrowthEventNames {
    const val EVENT_CARD_VIEWED = "event_card_viewed"
    const val EVENT_DETAIL_OPENED = "event_detail_opened"
    const val EVENT_SHARE_CLICKED = "event_share_clicked"
    const val EVENT_SHARE_COMPLETED = "event_share_completed"
    const val DEEP_LINK_OPENED = "deep_link_opened"
    const val DEEP_LINK_EVENT_OPENED = "deep_link_event_opened"
    const val NEARBY_SECTION_VIEWED = "nearby_section_viewed"
    const val NEARBY_EVENT_CLICKED = "nearby_event_clicked"
    const val JOIN_CTA_CLICKED = "join_cta_clicked"
    const val JOIN_SUCCESS = "join_success"
    const val PAYWALL_OPENED_FROM_JOIN_LIMIT = "paywall_opened_from_join_limit"
    const val PAYWALL_OPENED_FROM_CREATE_LIMIT = "paywall_opened_from_create_limit"
    const val EVENT_CREATED = "event_created"
    const val EVENT_CREATED_AND_SHARED = "event_created_and_shared"
    const val SHARE_FROM_EVENT_DETAIL = "share_from_event_detail"
    const val SHARE_FROM_EVENT_CARD = "share_from_event_card"
}

object GrowthAnalytics {
    private const val TAG = "CoffinityGrowth"

    fun log(
        eventName: String,
        params: Map<String, Any?> = emptyMap()
    ) {
        runCatching {
            val rendered = if (params.isEmpty()) {
                ""
            } else {
                params.entries.joinToString(
                    prefix = " {",
                    postfix = "}"
                ) { (key, value) -> "$key=${value?.toString().orEmpty()}" }
            }
            Log.d(TAG, "$eventName$rendered")
        }
    }
}
