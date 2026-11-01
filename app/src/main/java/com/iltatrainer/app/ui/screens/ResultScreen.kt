package com.iltatrainer.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iltatrainer.app.core.AppLanguage
import com.iltatrainer.app.data.Question
import com.iltatrainer.app.data.Rule
import com.iltatrainer.app.domain.AnswerChecker
import com.iltatrainer.app.domain.SessionMistake
import com.iltatrainer.app.domain.SessionResult
import com.iltatrainer.app.ui.components.AppCard
import com.iltatrainer.app.ui.components.LevelChip
import com.iltatrainer.app.ui.components.ProgressRing
import com.iltatrainer.app.ui.components.SectionHeader
import com.iltatrainer.app.ui.components.SentenceText
import com.iltatrainer.app.ui.itemLabel
import com.iltatrainer.app.ui.pluralRu
import com.iltatrainer.app.ui.tr

@Composable
fun ResultScreen(
    result: SessionResult,
    rulesById: Map<String, Rule>,
    language: AppLanguage,
    onReviewMistakes: () -> Unit,
    onDone: () -> Unit,
) {
    val fraction = if (result.total <= 0) 0f else result.correct.toFloat() / result.total

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = tr("Результат тренировки", "Practice results"),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = tr(
                "Каждый ответ уже учтён в вашей статистике.",
                "Every answer is already added to your stats.",
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(
                    progress = fraction,
                    size = 116.dp,
                    strokeWidth = 12.dp,
                    label = "${result.percent}%",
                    caption = tr("верно", "correct"),
                )
                Spacer(Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scoreHeadline(result.percent),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tr(
                            "Верно ${result.correct} из ${result.total}",
                            "${result.correct} of ${result.total} correct",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (result.mistakes.isEmpty()) {
                            tr("Ошибок нет 🎉", "No mistakes 🎉")
                        } else {
                            tr(
                                "${result.mistakes.size} ${pluralRu(result.mistakes.size, "ошибка", "ошибки", "ошибок")}",
                                "${result.mistakes.size} ${if (result.mistakes.size == 1) "mistake" else "mistakes"}",
                            )
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (result.mistakes.isEmpty()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
            }
        }

        if (result.mistakes.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SectionHeader(tr("Разбор ошибок", "Review mistakes"))
            Spacer(Modifier.height(4.dp))
            Text(
                text = tr(
                    "Ошибка закроется, когда вы верно ответите на этот вопрос и ещё на 3 похожих вопроса этой темы.",
                    "A mistake is closed after you answer this question correctly and 3 similar questions of the same theme correctly.",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            result.mistakes.forEach { mistake ->
                MistakeCard(
                    mistake = mistake,
                    rulesById = rulesById,
                    language = language,
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
        if (result.mistakes.isNotEmpty()) {
            Button(
                onClick = onReviewMistakes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    tr(
                        "Разобрать ошибки (${result.mistakes.size})",
                        "Review mistakes (${result.mistakes.size})",
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(tr("Готово", "Done"), style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    tr("Отлично! Готово", "Great! Done"),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun scoreHeadline(percent: Int): String = when {
    percent >= 90 -> tr("Отличная работа!", "Excellent work!")
    percent >= 70 -> tr("Хороший результат!", "Well done!")
    percent >= 50 -> tr("Неплохо!", "Not bad!")
    else -> tr("Стоит повторить правила", "Review the rules")
}

@Composable
private fun MistakeCard(
    mistake: SessionMistake,
    rulesById: Map<String, Rule>,
    language: AppLanguage,
) {
    val question = mistake.question
    val sectionId = rulesById[question.ruleId]?.sectionId ?: "articles"
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LevelChip(level = question.level)
            Spacer(Modifier.width(8.dp))
            Text(
                text = rulesById[question.ruleId]?.title?.text(language).orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        if (question.isBuild) {
            Text(
                text = tr("Правильное предложение", "Correct sentence"),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = AnswerChecker.correctText(question),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = tr(
                    "Ваш ответ: ${mistake.userInput}",
                    "Your answer: ${mistake.userInput}",
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        } else if (question.isMatch) {
            Text(
                text = question.prompt?.text(language)
                    ?: tr("Сопоставление правила и примера", "Rule ↔ example matching"),
                style = MaterialTheme.typography.titleMedium,
            )
        } else {
            SentenceText(
                sentence = question.sentence.orEmpty(),
                filled = AnswerChecker.canonicalAnswer(question),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = tr(
                    "Ваш ответ: ${userAnswerText(mistake.userInput, sectionId, language)}",
                    "Your answer: ${userAnswerText(mistake.userInput, sectionId, language)}",
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = question.explanation.text(language),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun userAnswerText(raw: String, sectionId: String, language: AppLanguage): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) {
        return if (language == AppLanguage.RU) "— (пусто)" else "— (empty)"
    }
    return itemLabel(trimmed.lowercase(), sectionId, language)
}
