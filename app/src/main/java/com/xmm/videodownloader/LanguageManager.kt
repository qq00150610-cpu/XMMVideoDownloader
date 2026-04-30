package com.xmm.videodownloader

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

object LanguageManager {

    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LOCALE = "selected_locale"

    data class Language(val code: String, val displayName: String)

    val languages = listOf(
        Language("en", "English"),
        Language("zh", "中文"),
        Language("ja", "日本語"),
        Language("ko", "한국어"),
        Language("es", "Español"),
        Language("fr", "Français"),
        Language("de", "Deutsch")
    )

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSelectedCode(context: Context): String {
        return prefs(context).getString(KEY_LOCALE, "en") ?: "en"
    }

    fun setSelectedCode(context: Context, code: String) {
        prefs(context).edit().putString(KEY_LOCALE, code).apply()
    }

    fun getSelectedIndex(context: Context): Int {
        val code = getSelectedCode(context)
        return languages.indexOfFirst { it.code == code }.coerceAtLeast(0)
    }

    fun applyLocale(context: Context): Context {
        val code = getSelectedCode(context)
        val locale = Locale(code)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
