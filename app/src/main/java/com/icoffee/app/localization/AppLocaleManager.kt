package com.icoffee.app.localization

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object AppLocaleManager {
    private const val PREFS_NAME = "coffinity_locale"
    private const val KEY_MODE = "locale_mode"
    private const val KEY_MANUAL_LANGUAGE = "manual_language_code"
    private const val LEGACY_KEY_LANGUAGE = "language_code"

    private const val MODE_SYSTEM = "system"
    private const val MODE_MANUAL = "manual"

    @Volatile
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val appContext = context.applicationContext
            migrateLegacyPreference(appContext)
            applyPreference(currentPreference(appContext))
            initialized = true
        }
    }

    fun currentLanguage(context: Context): SupportedLanguage {
        return resolveEffectiveLanguage(context.applicationContext)
    }

    fun currentLocale(context: Context): Locale = currentLanguage(context).locale

    fun currentPreference(context: Context): LocalePreference {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mode = prefs.getString(KEY_MODE, MODE_SYSTEM)
        val manualLanguage = SupportedLanguage.fromCode(prefs.getString(KEY_MANUAL_LANGUAGE, null))
        return if (mode == MODE_MANUAL && manualLanguage != null) {
            LocalePreference.Manual(manualLanguage)
        } else {
            LocalePreference.FollowSystem
        }
    }

    fun setLanguage(context: Context, language: SupportedLanguage) {
        persistPreference(
            context = context.applicationContext,
            preference = LocalePreference.Manual(language)
        )
        applyPreference(LocalePreference.Manual(language))
    }

    fun resetToDeviceLanguage(context: Context) {
        persistPreference(
            context = context.applicationContext,
            preference = LocalePreference.FollowSystem
        )
        applyPreference(LocalePreference.FollowSystem)
    }

    fun wrapContext(baseContext: Context): Context {
        val locale = resolveEffectiveLanguage(baseContext.applicationContext).locale
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return baseContext.createConfigurationContext(config)
    }

    private fun applyPreference(preference: LocalePreference) {
        when (preference) {
            LocalePreference.FollowSystem -> {
                val resolved = LanguageResolver.resolveFromSystem()
                Locale.setDefault(resolved.locale)
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            }
            is LocalePreference.Manual -> {
                Locale.setDefault(preference.language.locale)
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(preference.language.code)
                )
            }
        }
    }

    private fun resolveEffectiveLanguage(context: Context): SupportedLanguage {
        return when (val preference = currentPreference(context.applicationContext)) {
            LocalePreference.FollowSystem -> LanguageResolver.resolveFromSystem()
            is LocalePreference.Manual -> preference.language
        }
    }

    private fun persistPreference(context: Context, preference: LocalePreference) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        when (preference) {
            LocalePreference.FollowSystem -> {
                editor.putString(KEY_MODE, MODE_SYSTEM)
                editor.remove(KEY_MANUAL_LANGUAGE)
            }
            is LocalePreference.Manual -> {
                editor.putString(KEY_MODE, MODE_MANUAL)
                editor.putString(KEY_MANUAL_LANGUAGE, preference.language.code)
            }
        }
        editor.commit()
    }

    private fun migrateLegacyPreference(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasMode = prefs.contains(KEY_MODE)
        val manualLanguageCode = prefs.getString(KEY_MANUAL_LANGUAGE, null)

        if (manualLanguageCode.equals("pt", ignoreCase = true)) {
            prefs.edit()
                .putString(KEY_MANUAL_LANGUAGE, SupportedLanguage.PT.code)
                .apply()
        }

        if (!hasMode && prefs.contains(LEGACY_KEY_LANGUAGE)) {
            // Previous implementation persisted resolved device language as a hard value.
            // Migrate to explicit system-follow mode to prevent accidental manual lock-in.
            prefs.edit()
                .remove(LEGACY_KEY_LANGUAGE)
                .putString(KEY_MODE, MODE_SYSTEM)
                .apply()
        }
    }
}
