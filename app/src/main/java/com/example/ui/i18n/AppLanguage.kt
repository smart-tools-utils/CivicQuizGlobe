package com.example.ui.i18n

import android.content.Context
import androidx.compose.runtime.compositionLocalOf

/**
 * Supported app languages with code, native display name, flag emoji, and layout direction.
 * Designed to be easily extensible for additional languages in the future.
 */
enum class AppLanguage(
    val code: String,
    val displayName: String,
    val flag: String,
    val isRtl: Boolean
) {
    SWEDISH("sv", "Svenska", "🇸🇪", false),
    ENGLISH("en", "English", "🇬🇧", false),
    ARABIC("ar", "العربية", "🇸🇦", true);

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SWEDISH
        }
    }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.SWEDISH }

object LanguageManager {
    private const val PREFS_NAME = "civicquiz_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    fun getSavedLanguage(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_LANGUAGE, AppLanguage.SWEDISH.code)
        return AppLanguage.fromCode(code)
    }

    fun saveLanguage(context: Context, language: AppLanguage) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }
}
