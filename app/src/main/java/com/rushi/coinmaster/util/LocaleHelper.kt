package com.rushi.coinmaster.util

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {

    private const val PREFS_NAME = "coin_master_prefs"
    private const val KEY_LANGUAGE = "selected_language"
    const val DEFAULT_LANGUAGE = "en"

    /**
     * Applies the selected language to the app context and saves it to SharedPreferences.
     * Also calls AppCompatDelegate to notify the framework of the locale change.
     */
    fun setLocale(context: Context, languageCode: String): Context {
        persistLanguage(context, languageCode)

        // Set system-wide locale via AppCompatDelegate (works on API 33+ natively and backports to older APIs)
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)

        return updateResources(context, languageCode)
    }

    /**
     * Retrieves the saved language code from SharedPreferences. Defaults to "en".
     */
    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    /**
     * Context wrapper method to apply the language settings inside attachBaseContext.
     */
    fun wrap(context: Context): Context {
        val lang = getLanguage(context)
        return updateResources(context, lang)
    }

    private fun persistLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    private fun updateResources(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = Configuration(resources.configuration)

        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        return context.createConfigurationContext(configuration)
    }
}
