package com.icoffee.app.localization.formatter

import android.content.Context
import com.icoffee.app.R
import com.icoffee.app.localization.AppLocaleManager
import java.time.LocalDate
import java.time.LocalTime

object DateTimeFormatterHelper {

    private fun localizedContext(context: Context): Context = AppLocaleManager.wrapContext(context)

    fun formatMeetSummary(
        context: Context,
        date: LocalDate,
        startTime: LocalTime,
        durationMinutes: Int
    ): String {
        val ctx = localizedContext(context)
        val dateText = formatMeetDateLabel(ctx, date)
        val timeText = TimeFormatter.format(ctx, startTime)
        val durationText = formatDuration(ctx, durationMinutes)
        return ctx.getString(R.string.meet_time_summary_format, dateText, timeText, durationText)
    }

    fun formatMeetDateOption(context: Context, date: LocalDate): String {
        val ctx = localizedContext(context)
        val today = LocalDate.now()
        return when {
            date == today -> ctx.getString(R.string.meet_today)
            date == today.plusDays(1) -> ctx.getString(R.string.meet_tomorrow)
            else -> DateFormatter.formatOption(date, AppLocaleManager.currentLocale(ctx))
        }
    }

    fun formatDuration(context: Context, minutes: Int): String {
        val ctx = localizedContext(context)
        return if (minutes % 60 == 0) {
            val hours = minutes / 60
            ctx.resources.getQuantityString(R.plurals.meet_duration_hours_short, hours, hours)
        } else {
            ctx.resources.getQuantityString(R.plurals.meet_duration_minutes_short, minutes, minutes)
        }
    }

    private fun formatMeetDateLabel(context: Context, date: LocalDate): String {
        val today = LocalDate.now()
        return when {
            date == today -> context.getString(R.string.meet_today)
            date == today.plusDays(1) -> context.getString(R.string.meet_tomorrow)
            else -> DateFormatter.formatSummaryDate(date, AppLocaleManager.currentLocale(context))
        }
    }
}
