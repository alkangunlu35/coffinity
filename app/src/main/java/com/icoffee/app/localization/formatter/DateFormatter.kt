package com.icoffee.app.localization.formatter

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormatter {
    fun formatOption(date: LocalDate, locale: Locale): String {
        val formatter = DateTimeFormatter.ofPattern("EEE d MMM", locale)
        return date.format(formatter)
    }

    fun formatSummaryDate(date: LocalDate, locale: Locale): String {
        val formatter = DateTimeFormatter.ofPattern("EEE, MMM d", locale)
        return date.format(formatter)
    }
}
