package com.iltatrainer.app.core

/** UI / explanation language of the app. Content itself is bilingual (ru + en). */
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
