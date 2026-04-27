package com.icoffee.app.util

import java.util.Locale

object LegalLinks {
    private const val TURKISH_LANGUAGE = "tr"
    private const val ENGLISH_LANGUAGE = "en"

    private const val TERMS_TR_URL = "https://coffinity.net/legal/tr/terms"
    private const val PRIVACY_TR_URL = "https://coffinity.net/legal/tr/privacy-policy"
    private const val TERMS_EN_URL = "https://coffinity.net/legal/en/terms"
    private const val PRIVACY_EN_URL = "https://coffinity.net/legal/en/privacy-policy"

    fun normalizeLanguageCode(raw: String?): String {
        val normalized = raw
            ?.substringBefore('-')
            ?.substringBefore('_')
            ?.trim()
            ?.lowercase(Locale.ROOT)
            .orEmpty()
        return when (normalized) {
            TURKISH_LANGUAGE -> TURKISH_LANGUAGE
            ENGLISH_LANGUAGE -> ENGLISH_LANGUAGE
            else -> ENGLISH_LANGUAGE
        }
    }

    fun termsUrl(languageCode: String?): String =
        if (normalizeLanguageCode(languageCode) == TURKISH_LANGUAGE) TERMS_TR_URL else TERMS_EN_URL

    fun privacyUrl(languageCode: String?): String =
        if (normalizeLanguageCode(languageCode) == TURKISH_LANGUAGE) PRIVACY_TR_URL else PRIVACY_EN_URL
}
