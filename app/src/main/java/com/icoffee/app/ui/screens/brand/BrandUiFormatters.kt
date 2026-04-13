package com.icoffee.app.ui.screens.brand

import java.util.Locale

internal fun formatBrandLocation(
    cityOrArea: String?,
    countryOrCode: String?,
    locale: Locale
): String {
    val city = cityOrArea?.trim().orEmpty()
    val country = countryOrCode.toDisplayCountry(locale)
    return when {
        city.isNotBlank() && country.isNotBlank() -> "$city, $country"
        city.isNotBlank() -> city
        country.isNotBlank() -> country
        else -> ""
    }
}

private fun String?.toDisplayCountry(locale: Locale): String {
    val raw = this?.trim().orEmpty()
    if (raw.isBlank()) return ""

    val looksLikeCode = raw.length == 2 && raw.all { it.isLetter() }
    if (looksLikeCode) {
        val code = raw.uppercase(Locale.ROOT)
        val localized = Locale("", code).getDisplayCountry(locale).trim()
        if (localized.isNotBlank()) return localized
    }
    return raw
}
