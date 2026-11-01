package com.iltatrainer.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.iltatrainer.app.core.AppLanguage
import com.iltatrainer.app.data.Bilingual
import kotlin.math.abs
import kotlin.math.roundToInt

/** Current UI language, provided once at the top of the composition. */
val LocalLanguage = staticCompositionLocalOf<AppLanguage> { AppLanguage.RU }

@Composable
fun tr(ru: String, en: String): String =
    if (LocalLanguage.current == AppLanguage.RU) ru else en

@Composable
fun Bilingual.localized(): String = text(LocalLanguage.current)

fun Bilingual.localized(language: AppLanguage): String = text(language)

/**
 * The em dash option means "no article" in the Articles section and "no preposition"
 * in the Prepositions section, so the label depends on the section.
 */
@Composable
fun zeroItemLabel(sectionId: String): String =
    if (sectionId == "articles") {
        tr("\u2014 (без артикля)", "\u2014 (no article)")
    } else {
        tr("\u2014 (без предлога)", "\u2014 (no preposition)")
    }

@Composable
fun itemLabel(value: String, sectionId: String): String =
    if (value == "\u2014") zeroItemLabel(sectionId) else value

fun itemLabel(value: String, sectionId: String, language: AppLanguage): String =
    if (value == "\u2014") {
        if (language == AppLanguage.RU) {
            if (sectionId == "articles") "\u2014 (без артикля)" else "\u2014 (без предлога)"
        } else {
            if (sectionId == "articles") "\u2014 (no article)" else "\u2014 (no preposition)"
        }
    } else {
        value
    }

@Composable
fun questionsLabel(count: Int): String = when (LocalLanguage.current) {
    AppLanguage.RU -> "$count ${pluralRu(count, "вопрос", "вопроса", "вопросов")}"
    AppLanguage.EN -> if (count == 1) "1 question" else "$count questions"
}

@Composable
fun answersLabel(count: Int): String = when (LocalLanguage.current) {
    AppLanguage.RU -> "$count ${pluralRu(count, "ответ", "ответа", "ответов")}"
    AppLanguage.EN -> if (count == 1) "1 answer" else "$count answers"
}

fun pluralRu(count: Int, one: String, few: String, many: String): String {
    val n = abs(count)
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
}

fun formatPercent(accuracy: Float): String = "${(accuracy.coerceIn(0f, 1f) * 100).roundToInt()}%"

fun formatPercent(rawPercent: Int): String = "${rawPercent.coerceIn(0, 100)}%"

/** "the" / "a" / "an" / "—", capped for use inside a sentence. */
fun articleForDisplay(article: String): String = article
