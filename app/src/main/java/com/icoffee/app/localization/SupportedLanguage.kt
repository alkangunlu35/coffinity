package com.icoffee.app.localization

import androidx.annotation.StringRes
import com.icoffee.app.R
import java.util.Locale

enum class SupportedLanguage(
    val code: String,
    val locale: Locale,
    @param:StringRes val displayNameResId: Int
) {
    EN("en", Locale.ENGLISH, R.string.settings_language_english),
    TR("tr", Locale("tr"), R.string.settings_language_turkish),
    DE("de", Locale.GERMAN, R.string.settings_language_german),
    FR("fr", Locale.FRENCH, R.string.settings_language_french),
    ES("es", Locale("es"), R.string.settings_language_spanish),
    PT("pt-BR", Locale("pt", "BR"), R.string.settings_language_portuguese),
    AR("ar", Locale("ar"), R.string.settings_language_arabic);

    companion object {
        private val byCode = buildMap<String, SupportedLanguage> {
            SupportedLanguage.entries.forEach { language ->
                val languageCode = language.code.lowercase(Locale.ROOT)
                put(languageCode, language)
                put(languageCode.replace('_', '-'), language)
                put(language.locale.language.lowercase(Locale.ROOT), language)
                put(language.locale.toLanguageTag().lowercase(Locale.ROOT), language)
            }
            // Backward compatibility for previously stored manual selection.
            put("pt", PT)
            put("pt-br", PT)
        }
        val pickerLanguages: List<SupportedLanguage> = listOf(EN, TR, DE, FR, ES, PT)

        fun fromCode(languageCode: String?): SupportedLanguage? {
            if (languageCode.isNullOrBlank()) return null
            val normalized = languageCode
                .trim()
                .replace('_', '-')
                .lowercase(Locale.ROOT)
            return byCode[normalized]
        }
    }
}
