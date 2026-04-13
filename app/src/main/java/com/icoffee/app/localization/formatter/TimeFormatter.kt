package com.icoffee.app.localization.formatter

import android.content.Context
import android.text.format.DateFormat
import com.icoffee.app.localization.AppLocaleManager
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object TimeFormatter {
    fun format(
        context: Context,
        time: LocalTime
    ): String {
        val locale = AppLocaleManager.currentLocale(context)
        return if (DateFormat.is24HourFormat(context)) {
            time.format(DateTimeFormatter.ofPattern("HH:mm", locale))
        } else {
            time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
        }
    }
}
