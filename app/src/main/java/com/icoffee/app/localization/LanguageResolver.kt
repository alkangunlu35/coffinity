package com.icoffee.app.localization

import androidx.core.os.LocaleListCompat
import java.util.Locale

object LanguageResolver {

    fun resolveDeviceLanguage(deviceLocales: List<Locale>): SupportedLanguage {
        deviceLocales.forEach { locale ->
            SupportedLanguage.fromCode(locale.language)?.let { return it }
        }
        return SupportedLanguage.EN
    }

    fun resolveFromSystem(): SupportedLanguage {
        val localeList = LocaleListCompat.getDefault()
        val locales = buildList {
            for (index in 0 until localeList.size()) {
                localeList[index]?.let(::add)
            }
        }
        return resolveDeviceLanguage(locales)
    }
}
