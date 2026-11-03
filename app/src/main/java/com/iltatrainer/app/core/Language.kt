package com.iltatrainer.app.core

/**
 * Content packages stay bilingual (Russian + English fields), but the IlTa 3.0 UI is
 * Russian-only. [AppLanguage.EN] is kept so old exported configs still load.
 */
enum class AppLanguage(val code: String) {
    RU("ru"),
    EN("en");

    companion object {
        fun fromCode(code: String?): AppLanguage = if (code == EN.code) EN else RU
    }
}

/** Theme preference: follow the system or pin light / dark. */
enum class ThemeMode(val code: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromCode(code: String?): ThemeMode = entries.firstOrNull { it.code == code } ?: SYSTEM
    }
}
