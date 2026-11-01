package com.iltatrainer.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iltatrainer.app.data.MistakeEntry
import com.iltatrainer.app.data.ProgressState
import com.iltatrainer.app.data.Question
import com.iltatrainer.app.data.Rule
import com.iltatrainer.app.domain.SessionBuilder
import com.iltatrainer.app.domain.StatsCalculator
import com.iltatrainer.app.ui.answersLabel
import com.iltatrainer.app.ui.components.AccuracyBar
import com.iltatrainer.app.ui.components.AppCard
import com.iltatrainer.app.ui.components.EmptyState
import com.iltatrainer.app.ui.components.LevelChip
import com.iltatrainer.app.ui.components.ProgressRing
import com.iltatrainer.app.ui.components.SectionHeader
import com.iltatrainer.app.ui.components.StatTile
import com.iltatrainer.app.ui.components.levelColor
import com.iltatrainer.app.ui.formatPercent
import com.iltatrainer.app.ui.localized
import com.iltatrainer.app.ui.pluralRu
import com.iltatrainer.app.ui.tr

private const val MISTAKE_LIMIT = 5

@Composable
fun StatsScreen(
    rules: List<Rule>,
    questions: List<Question>,
    progress: ProgressState,
    onStartMistakes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasData = progress.totalAnswered > 0
    val hasMistakes = progress.mistakes.isNotEmpty()
    val openMistakes = remember(progress.mistakes, questions) {
        val questionsById = questions.associateBy { it.id }
        SessionBuilder.openMistakes(progress)
            .mapNotNull { (questionId, entry) ->
                questionsById[questionId]?.let { question -> question to entry }
            }
            .take(MISTAKE_LIMIT)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = tr("Статистика", "Statistics"),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = tr("Ваш прогресс за всё время", "Your all-time progress"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (!hasData) {
            item(key = "empty") {
                EmptyState(
                    emoji = "📊",
                    title = tr("Пока нет данных", "No data yet"),
                    message = tr(
                        "Ответьте на несколько вопросов — и здесь появится ваша статистика.",
                        "Answer a few questions and your statistics will appear here.",
                    ),
                )
            }
            item(key = "empty-actions") {
                StatsActionButtons(
                    mistakesEnabled = false,
                    onStartMistakes = onStartMistakes,
                )
            }
        } else {
            item(key = "overview") {
                StatsOverview(progress = progress)
            }
            item(key = "learning") {
                StatsLearningCard(
                    rules = rules,
                    questions = questions,
                    progress = progress,
                )
            }
            item(key = "levels-header") {
                SectionHeader(tr("Уровни", "Levels"))
            }
            item(key = "levels") {
                StatsLevelsCard(progress = progress, questions = questions)
            }
            item(key = "mistakes-header") {
                SectionHeader(tr("Ошибки в работе", "Mistakes to review"))
            }
            item(key = "mistakes") {
                StatsMistakesCard(
                    mistakes = openMistakes,
                    rules = rules,
                )
            }
            item(key = "actions") {
                StatsActionButtons(
                    mistakesEnabled = hasMistakes,
                    onStartMistakes = onStartMistakes,
                )
            }
        }
    }
}

@Composable
private fun StatsOverview(progress: ProgressState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = tr("Ответов", "Answered"),
                value = progress.totalAnswered.toString(),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = tr("Точность", "Accuracy"),
                value = formatPercent(StatsCalculator.overallAccuracy(progress)),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = tr("Освоено", "Mastered"),
                value = StatsCalculator.masteredCount(progress).toString(),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = tr("Лучшая серия", "Best streak"),
                value = StatsCalculator.bestStreak(progress).toString(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatsLearningCard(
    rules: List<Rule>,
    questions: List<Question>,
    progress: ProgressState,
) {
    val percent = StatsCalculator.learningProgress(progress, rules, questions)
    val learned = questions.count { question -> (progress.questionStats[question.id]?.correct ?: 0) > 0 }
    val total = questions.size
    val startedTopics = rules.count { rule -> (progress.ruleStats[rule.id]?.attempts ?: 0) > 0 }
    val streak = StatsCalculator.streak(progress)
    val bestStreak = StatsCalculator.bestStreak(progress)
    val learnedLine = tr(
        "$learned из $total ${pluralRu(total, "вопроса", "вопросов", "вопросов")}",
        "$learned of $total ${if (total == 1) "question" else "questions"}",
    )

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = percent / 100f,
                label = formatPercent(percent),
                caption = tr("грамматики", "grammar"),
            )
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr("Изучено грамматики", "Grammar learned"),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = learnedLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = tr(
                        "Тем начато: $startedTopics",
                        "$startedTopics ${if (startedTopics == 1) "topic" else "topics"} started",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = tr(
                        "🔥 $streak ${pluralRu(streak, "день", "дня", "дней")} подряд",
                        "🔥 $streak-day streak",
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = tr("Лучшая серия: $bestStreak", "Best streak: $bestStreak"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatsLevelsCard(progress: ProgressState, questions: List<Question>) {
    val levelStats = StatsCalculator.levelStats(progress, questions)
    AppCard {
        if (levelStats.isEmpty()) {
            Text(
                text = tr("Пока нет ответов по уровням.", "No answers by level yet."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                levelStats.forEach { (level, stat) ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LevelChip(level = level)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "${answersLabel(stat.attempts)} · ${formatPercent(stat.accuracy)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AccuracyBar(
                            accuracy = stat.accuracy,
                            color = levelColor(level),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsMistakesCard(
    mistakes: List<Pair<Question, MistakeEntry>>,
    rules: List<Rule>,
) {
    val rulesById = remember(rules) { rules.associateBy { it.id } }
    AppCard {
        if (mistakes.isEmpty()) {
            Text(
                text = tr("Ошибок нет — отлично!", "No open mistakes — great!"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                mistakes.forEachIndexed { index, (question, entry) ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    }
                    val ruleTitle = rulesById[question.ruleId]?.title?.localized() ?: question.ruleId
                    val preview = question.sentence
                        ?.takeIf { it.isNotBlank() }
                        ?.replace("___", "…")
                        ?: question.prompt?.localized()
                        ?: question.id
                    val originalMark = if (entry.originalCorrect) "✓" else "—"
                    val closureStatus = tr(
                        "Исходный: $originalMark · Похожих: ${entry.similarCorrect}/3",
                        "Original: $originalMark · Similar: ${entry.similarCorrect}/3",
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = ruleTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = closureStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Text(
                    text = tr(
                        "Ошибка закрывается после верного ответа на исходный вопрос + 3 похожих.",
                        "A mistake closes after the original + 3 similar questions are answered correctly.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatsActionButtons(
    mistakesEnabled: Boolean,
    onStartMistakes: () -> Unit,
) {
    Button(
        onClick = onStartMistakes,
        enabled = mistakesEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(tr("Работа над ошибками", "Practise mistakes"))
    }
}
