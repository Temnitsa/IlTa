package com.iltatrainer.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.iltatrainer.app.core.AppLanguage
import com.iltatrainer.app.data.Bilingual
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * IlTa 3.0 is Russian-only: the UI language is pinned to RU.
 * The composition local is kept so existing call sites (`tr`, `localized`) keep working.
 */
val LocalLanguage = staticCompositionLocalOf<AppLanguage> { AppLanguage.RU }

/** The app is Russian-only; the English variant is kept only for old call sites. */
@Composable
fun tr(ru: String, @Suppress("UNUSED_PARAMETER") en: String): String = ru

@Composable
fun Bilingual.localized(): String = text(AppLanguage.RU)

fun Bilingual.localized(language: AppLanguage): String = text(language)

/** The em dash option means "no article" / "no preposition" depending on the section. */
@Composable
fun zeroItemLabel(sectionId: String): String =
    if (sectionId == "articles") "\u2014 (без артикля)" else "\u2014 (без предлога)"

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
fun questionsLabel(count: Int): String = "$count ${pluralRu(count, "вопрос", "вопроса", "вопросов")}"

@Composable
fun answersLabel(count: Int): String = "$count ${pluralRu(count, "ответ", "ответа", "ответов")}"

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
