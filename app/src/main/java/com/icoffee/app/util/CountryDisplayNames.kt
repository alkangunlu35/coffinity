package com.icoffee.app.util

import java.util.Locale

object CountryDisplayNames {
    private val isoCountryCodes: Set<String> = Locale.getISOCountries().toSet()

    private val englishNameToIsoCode: Map<String, String> =
        Locale.getISOCountries().associateBy(
            keySelector = { normalizeCountryKey(Locale("", it).getDisplayCountry(Locale.ENGLISH)) },
            valueTransform = { it }
        )

    private val aliasToIsoCode: Map<String, String> = mapOf(
        normalizeCountryKey("Dominican Rep.") to "DO",
        normalizeCountryKey("Dominican Republic") to "DO",
        normalizeCountryKey("Ivory Coast") to "CI"
    )

    fun localizedName(rawCountry: String, appLanguageCode: String?): String {
        val trimmed = rawCountry.trim()
        if (trimmed.isBlank()) return rawCountry

        val targetLocale = supportedLocale(appLanguageCode)
        val countryCode = resolveCountryCode(trimmed) ?: return trimmed
        val localized = Locale("", countryCode).getDisplayCountry(targetLocale).trim()
        if (localized.isNotBlank()) return localized

        val englishFallback = Locale("", countryCode).getDisplayCountry(Locale.ENGLISH).trim()
        return if (englishFallback.isNotBlank()) englishFallback else trimmed
    }

    private fun supportedLocale(rawLanguageCode: String?): Locale {
        val normalized = rawLanguageCode
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.replace('_', '-')
            .orEmpty()

        return when {
            normalized == "tr" || normalized.startsWith("tr-") -> Locale("tr")
            normalized == "en" || normalized.startsWith("en-") -> Locale.ENGLISH
            normalized == "de" || normalized.startsWith("de-") -> Locale.GERMAN
            normalized == "fr" || normalized.startsWith("fr-") -> Locale.FRENCH
            normalized == "es" || normalized.startsWith("es-") -> Locale("es")
            normalized == "pt" || normalized == "pt-br" || normalized.startsWith("pt-") -> Locale("pt", "BR")
            else -> Locale.ENGLISH
        }
    }

    private fun resolveCountryCode(rawCountry: String): String? {
        val uppercaseCandidate = rawCountry.uppercase(Locale.ROOT)
        if (
            uppercaseCandidate.length == 2 &&
            uppercaseCandidate.all { it.isLetter() } &&
            isoCountryCodes.contains(uppercaseCandidate)
        ) {
            return uppercaseCandidate
        }

        val key = normalizeCountryKey(rawCountry)
        return aliasToIsoCode[key] ?: englishNameToIsoCode[key]
    }

    private fun normalizeCountryKey(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(".", "")
        .replace("'", "")
        .replace(Regex("\\s+"), " ")
        .trim()
}
