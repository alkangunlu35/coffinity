package com.icoffee.app.localization

sealed interface LocalePreference {
    data object FollowSystem : LocalePreference
    data class Manual(val language: SupportedLanguage) : LocalePreference
}
