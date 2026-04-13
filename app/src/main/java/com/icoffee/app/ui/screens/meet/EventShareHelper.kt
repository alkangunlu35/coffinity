package com.icoffee.app.ui.screens.meet

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.icoffee.app.R
import com.icoffee.app.data.model.CoffeeMeet

object EventShareHelper {

    private const val SHARE_DEBOUNCE_MS = 700L
    @Volatile
    private var lastShareLaunchAtMillis: Long = 0L
    private val shareLaunchLock = Any()

    enum class ShareLaunchResult {
        LAUNCHED,
        THROTTLED,
        FAILED
    }

    fun deepLinkForEvent(eventId: String): String? {
        val cleanId = eventId.trim()
        if (cleanId.isBlank()) return null
        return "coffinity://event/${Uri.encode(cleanId)}"
    }

    fun buildShareText(
        context: Context,
        event: CoffeeMeet
    ): String {
        val lines = mutableListOf<String>()
        lines += context.getString(R.string.meet_share_intro)
        lines += event.title.ifBlank { context.getString(R.string.meet_title) }

        if (event.time.isNotBlank()) {
            lines += context.getString(R.string.meet_share_when, event.time)
        }
        if (event.locationName.isNotBlank()) {
            lines += context.getString(R.string.meet_share_where, event.locationName)
        }
        event.description
            .trim()
            .takeIf { it.isNotBlank() }
            ?.let { description ->
                lines += description.take(140)
            }
        deepLinkForEvent(event.id)?.let { deepLink ->
            lines += context.getString(
                R.string.meet_share_open_link,
                deepLink
            )
        }
        return lines.joinToString(separator = "\n")
    }

    fun shareEvent(
        context: Context,
        event: CoffeeMeet
    ): ShareLaunchResult {
        val now = System.currentTimeMillis()
        synchronized(shareLaunchLock) {
            if (now - lastShareLaunchAtMillis < SHARE_DEBOUNCE_MS) {
                return ShareLaunchResult.THROTTLED
            }
            lastShareLaunchAtMillis = now
        }
        val shareText = buildShareText(context, event)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, event.title.ifBlank { context.getString(R.string.meet_title) })
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(
            intent,
            context.getString(R.string.meet_share_chooser_title)
        )
        if (context !is Activity) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(chooser)
            ShareLaunchResult.LAUNCHED
        }.getOrElse {
            synchronized(shareLaunchLock) {
                lastShareLaunchAtMillis = 0L
            }
            ShareLaunchResult.FAILED
        }
    }
}
