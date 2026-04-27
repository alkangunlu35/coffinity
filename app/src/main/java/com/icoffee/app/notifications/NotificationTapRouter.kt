// FILE: app/src/main/java/com/icoffee/app/notifications/NotificationTapRouter.kt
// FULL REPLACEMENT

package com.icoffee.app.notifications

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.icoffee.app.analytics.AnalyticsEvents
import com.icoffee.app.analytics.AnalyticsParams
import com.icoffee.app.analytics.AnalyticsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class NotificationTapDestination(open val deliveryKey: String) {
    data class EventDetail(
        val eventId: String,
        override val deliveryKey: String
    ) : NotificationTapDestination(deliveryKey)

    data class Chat(
        val chatId: String,
        val eventId: String?,
        val inviteId: String?,
        val messageId: String?,
        val senderId: String?,
        override val deliveryKey: String
    ) : NotificationTapDestination(deliveryKey)

    data class Social(
        val inviteId: String?,
        override val deliveryKey: String
    ) : NotificationTapDestination(deliveryKey)
}

private data class NotificationPayload(
    val type: String,
    val route: String,
    val eventId: String,
    val inviteId: String,
    val messageId: String,
    val senderId: String,
    val userId: String,
    val chatId: String,
    val deliveryId: String
)

object NotificationTapRouter {
    object Extras {
        const val TYPE = "notif_type"
        const val ROUTE = "notif_route"
        const val EVENT_ID = "notif_eventId"
        const val INVITE_ID = "notif_inviteId"
        const val MESSAGE_ID = "notif_messageId"
        const val SENDER_ID = "notif_senderId"
        const val USER_ID = "notif_userId"
        const val CHAT_ID = "notif_chatId"
        const val DELIVERY_ID = "notif_deliveryId"
    }

    private const val TAG = "NotificationRouter"
    private const val MAX_RECENT_KEYS = 80

    private val _pendingDestination = MutableStateFlow<NotificationTapDestination?>(null)
    val pendingDestination: StateFlow<NotificationTapDestination?> = _pendingDestination.asStateFlow()

    private val _pendingChatDestination = MutableStateFlow<NotificationTapDestination.Chat?>(null)
    val pendingChatDestination: StateFlow<NotificationTapDestination.Chat?> =
        _pendingChatDestination.asStateFlow()

    private val recentKeyOrder = ArrayDeque<String>()
    private val handledKeys = linkedSetOf<String>()
    private val lock = Any()

    fun handleIntent(intent: Intent?, source: String) {
        if (intent == null) return

        val payload = readPayload(intent) ?: return

        Log.d(
            TAG,
            "NOTIF_ROUTE_RECEIVED source=$source type=${payload.type} route=${payload.route} eventId=${payload.eventId} inviteId=${payload.inviteId} messageId=${payload.messageId} chatId=${payload.chatId}"
        )

        val destination = parseDestination(payload)
        if (destination == null) {
            Log.w(
                TAG,
                "NOTIF_ROUTE_INVALID source=$source type=${payload.type} route=${payload.route} eventId=${payload.eventId} inviteId=${payload.inviteId} chatId=${payload.chatId}"
            )
            return
        }

        if (destination.deliveryKey.isBlank()) {
            Log.w(TAG, "NOTIF_ROUTE_INVALID reason=blank_delivery_key")
            return
        }

        val shouldStore = synchronized(lock) {
            if (handledKeys.contains(destination.deliveryKey)) {
                false
            } else {
                handledKeys.add(destination.deliveryKey)
                recentKeyOrder.addLast(destination.deliveryKey)
                while (recentKeyOrder.size > MAX_RECENT_KEYS) {
                    val removed = recentKeyOrder.removeFirst()
                    handledKeys.remove(removed)
                }
                true
            }
        }

        if (!shouldStore) {
            Log.d(TAG, "NOTIF_ROUTE_DUPLICATE_IGNORED key=${destination.deliveryKey}")
            return
        }

        Log.d(
            TAG,
            "NOTIF_ROUTE_PARSED key=${destination.deliveryKey} destination=${destination.javaClass.simpleName}"
        )

        AnalyticsProvider.tracker.logEvent(
            AnalyticsEvents.PUSH_OPENED,
            mapOf(
                AnalyticsParams.TYPE to payload.type.ifBlank { "unknown" },
                AnalyticsParams.ROUTE to payload.route.ifBlank { "unknown" },
                AnalyticsParams.EVENT_ID to payload.eventId.ifBlank { "unknown" },
                AnalyticsParams.INVITE_ID to payload.inviteId.ifBlank { "unknown" },
                AnalyticsParams.CHAT_ID to payload.chatId.ifBlank { "unknown" }
            )
        )

        _pendingDestination.value = destination
        Log.d(TAG, "NOTIF_ROUTE_PENDING key=${destination.deliveryKey} stage=navigation")
    }

    fun consumePendingDestination(expectedKey: String? = null): NotificationTapDestination? {
        val current = _pendingDestination.value ?: return null
        if (!expectedKey.isNullOrBlank() && current.deliveryKey != expectedKey) return null
        _pendingDestination.value = null
        Log.d(TAG, "NOTIF_ROUTE_CONSUMED key=${current.deliveryKey} stage=navigation")
        return current
    }

    fun queuePendingChatDestination(destination: NotificationTapDestination.Chat) {
        _pendingChatDestination.value = destination
        Log.d(TAG, "NOTIF_ROUTE_PENDING key=${destination.deliveryKey} stage=chat")
    }

    fun consumePendingChatDestination(expectedKey: String? = null): NotificationTapDestination.Chat? {
        val current = _pendingChatDestination.value ?: return null
        if (!expectedKey.isNullOrBlank() && current.deliveryKey != expectedKey) return null
        _pendingChatDestination.value = null
        Log.d(TAG, "NOTIF_ROUTE_CONSUMED key=${current.deliveryKey} stage=chat")
        return current
    }

    private fun parseDestination(payload: NotificationPayload): NotificationTapDestination? {
        val type = payload.type.trim().lowercase()
        val route = payload.route.trim().lowercase()
        val deliveryKey = buildDeliveryKey(payload)

        return when {
            route == "chat" -> {
                val chatId = payload.chatId.trim()
                if (chatId.isBlank()) {
                    Log.w(TAG, "NOTIF_ROUTE_INVALID invalid_chat chatId_blank type=$type route=$route")
                    null
                } else {
                    NotificationTapDestination.Chat(
                        chatId = chatId,
                        eventId = payload.eventId.ifBlank { null },
                        inviteId = payload.inviteId.ifBlank { null },
                        messageId = payload.messageId.ifBlank { null },
                        senderId = payload.senderId.ifBlank { null },
                        deliveryKey = deliveryKey
                    )
                }
            }

            route == "event_detail" -> {
                val eventId = payload.eventId.trim()
                if (eventId.isBlank()) {
                    Log.w(TAG, "NOTIF_ROUTE_INVALID invalid_event eventId_blank type=$type route=$route")
                    null
                } else {
                    NotificationTapDestination.EventDetail(
                        eventId = eventId,
                        deliveryKey = deliveryKey
                    )
                }
            }

            route == "social" -> {
                NotificationTapDestination.Social(
                    inviteId = payload.inviteId.ifBlank { null },
                    deliveryKey = deliveryKey
                )
            }

            type == "chat_message" -> {
                val chatId = payload.chatId.trim()
                if (chatId.isBlank()) {
                    Log.w(TAG, "NOTIF_ROUTE_INVALID invalid_chat chatId_blank type=$type route=$route")
                    null
                } else {
                    NotificationTapDestination.Chat(
                        chatId = chatId,
                        eventId = payload.eventId.ifBlank { null },
                        inviteId = payload.inviteId.ifBlank { null },
                        messageId = payload.messageId.ifBlank { null },
                        senderId = payload.senderId.ifBlank { null },
                        deliveryKey = deliveryKey
                    )
                }
            }

            type == "invite_new" || type == "invite_accepted" -> {
                NotificationTapDestination.Social(
                    inviteId = payload.inviteId.ifBlank { null },
                    deliveryKey = deliveryKey
                )
            }

            else -> null
        }
    }

    private fun buildDeliveryKey(payload: NotificationPayload): String {
        val explicit = payload.deliveryId.trim()
        if (explicit.isNotBlank()) return explicit

        return when (payload.type.trim().lowercase()) {
            "chat_message" -> "chat:${payload.chatId}:${payload.messageId}"
            "invite_new", "invite_accepted" ->
                "invite:${payload.type}:${payload.inviteId}:${payload.eventId}:${payload.chatId}"
            else -> "route:${payload.route}:${payload.eventId}:${payload.chatId}:${payload.messageId}"
        }
    }

    private fun readPayload(intent: Intent): NotificationPayload? {
        val extras = intent.extras ?: return null

        val type = readString(extras, Extras.TYPE, "type")
        val route = readString(extras, Extras.ROUTE, "route")
        val eventId = readString(extras, Extras.EVENT_ID, "eventId", "meetId")
        val inviteId = readString(extras, Extras.INVITE_ID, "inviteId")
        val messageId = readString(extras, Extras.MESSAGE_ID, "messageId")
        val senderId = readString(extras, Extras.SENDER_ID, "senderId")
        val userId = readString(extras, Extras.USER_ID, "userId")
        val chatId = readString(extras, Extras.CHAT_ID, "chatId")
        val deliveryId = readString(extras, Extras.DELIVERY_ID, "google.message_id", "message_id")

        val hasRoutingSignal = type.isNotBlank() || route.isNotBlank()
        if (!hasRoutingSignal) return null

        return NotificationPayload(
            type = type,
            route = route,
            eventId = eventId,
            inviteId = inviteId,
            messageId = messageId,
            senderId = senderId,
            userId = userId,
            chatId = chatId,
            deliveryId = deliveryId
        )
    }

    private fun readString(bundle: Bundle, vararg keys: String): String {
        for (key in keys) {
            val stringValue = bundle.getString(key)?.trim().orEmpty()
            if (stringValue.isNotBlank()) return stringValue

            if (bundle.containsKey(key)) {
                val fallback = bundle.get(key)?.toString()?.trim().orEmpty()
                if (fallback.isNotBlank()) return fallback
            }
        }
        return ""
    }
}