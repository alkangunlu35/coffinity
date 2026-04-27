package com.icoffee.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.icoffee.app.MainActivity
import com.icoffee.app.R
import com.icoffee.app.data.notifications.NotificationSettingsRepository

private const val CHANNEL_ID_GENERAL = "coffinity_general"
private const val TAG = "FCM_DEBUG"

class CoffinityFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken invoked tokenLength=${token.length}")
        NotificationSettingsRepository.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(
            TAG,
            "onMessageReceived from=${message.from} messageId=${message.messageId} dataKeys=${message.data.keys}"
        )
        if (!canPostNotifications()) {
            Log.d(TAG, "onMessageReceived: notification permission not granted, skipping local display")
            return
        }

        ensureNotificationChannel()

        val data = message.data
        val title = message.notification?.title
            ?: data["title"]
            ?: getString(R.string.app_name)
        val body = message.notification?.body
            ?: data["body"]
            ?: getString(R.string.notifications_default_message)

        val pendingIntent = buildContentIntent(data, message.messageId)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_GENERAL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this)
            .notify(System.currentTimeMillis().toInt(), notification)
        Log.d(TAG, "local notification displayed")
    }

    private fun buildContentIntent(data: Map<String, String>, remoteMessageId: String?): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putRouteExtra(NotificationTapRouter.Extras.TYPE, data["type"])
            putRouteExtra(NotificationTapRouter.Extras.ROUTE, data["route"])
            putRouteExtra(NotificationTapRouter.Extras.EVENT_ID, data["eventId"] ?: data["meetId"])
            putRouteExtra(NotificationTapRouter.Extras.INVITE_ID, data["inviteId"])
            putRouteExtra(NotificationTapRouter.Extras.MESSAGE_ID, data["messageId"])
            putRouteExtra(NotificationTapRouter.Extras.SENDER_ID, data["senderId"])
            putRouteExtra(NotificationTapRouter.Extras.USER_ID, data["userId"])
            putRouteExtra(NotificationTapRouter.Extras.CHAT_ID, data["chatId"])
            putRouteExtra(
                NotificationTapRouter.Extras.DELIVERY_ID,
                data["messageId"] ?: remoteMessageId
            )
        }

        val requestCodeKey = listOf(
            "type=${data["type"].orEmpty().trim()}",
            "route=${data["route"].orEmpty().trim()}",
            "messageId=${data["messageId"].orEmpty().trim()}",
            "inviteId=${data["inviteId"].orEmpty().trim()}",
            "eventId=${(data["eventId"] ?: data["meetId"]).orEmpty().trim()}",
            "chatId=${data["chatId"].orEmpty().trim()}",
            "remoteMessageId=${remoteMessageId.orEmpty().trim()}"
        ).joinToString(separator = "|")
        val requestCode = (requestCodeKey.hashCode() and 0x7FFFFFFF).takeIf { it != 0 }
            ?: (System.currentTimeMillis() and 0x7FFFFFFF).toInt().coerceAtLeast(1)

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(this, requestCode, intent, flags)
    }

    private fun Intent.putRouteExtra(key: String, value: String?) {
        val normalized = value?.trim().orEmpty()
        if (normalized.isNotBlank()) {
            putExtra(key, normalized)
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID_GENERAL,
            getString(R.string.notifications_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.notifications_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
