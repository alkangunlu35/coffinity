package com.icoffee.app.data.model

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun BusinessOffer.formattedPriceOrNull(locale: Locale = Locale.getDefault()): String? {
    val amount = priceAmount ?: return null
    val code = currency ?: return null
    return runCatching {
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = Currency.getInstance(code)
        formatter.maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
        formatter.minimumFractionDigits = 0
        formatter.format(amount)
    }.getOrNull()
}

