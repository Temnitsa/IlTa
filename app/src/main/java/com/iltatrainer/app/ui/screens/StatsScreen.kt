package com.iltatrainer.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iltatrainer.app.data.ProgressState
import com.iltatrainer.app.data.Question
import com.iltatrainer.app.data.Rule
import com.iltatrainer.app.domain.StatsCalculator
import com.iltatrainer.app.ui.components.AccuracyBar
import com.iltatrainer.app.ui.components.AppCard
import com.iltatrainer.app.ui.components.LevelChip
import com.iltatrainer.app.ui.components.ProgressRing
import com.iltatrainer.app.ui.components.RadarChart
import com.iltatrainer.app.ui.components.SectionHeader
import com.iltatrainer.app.ui.components.StatTile
import com.iltatrainer.app.ui.formatPercent
import com.iltatrainer.app.ui.pluralRu
import com.iltatrainer.app.ui.questionsLabel

private data class StatHint(val title: String, val message: String)

@Composable
fun StatsScreen(
    rules: List<Rule>,
    questions: List<Question>,
    progress: ProgressState,
    modifier: Modifier = Modifier,
) {
    var hint by remember { mutableStateOf<StatHint?>(null) }
    val hasData = progress.totalAnswered > 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text("Статистика", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = "Ответов",
                value = progress.totalAnswered.toString(),
                modifier = Modifier.weight(1f),
                onClick = {
                    hint = StatHint(
                        title = "Ответов",
                        message = "Всего ответов за всё время. Повторные попытки тоже учитываются.",
                    )
                },
            )
            StatTile(
                label = "Точность",
                value = formatPercent(StatsCalculator.overallAccuracy(progress)),
                modifier = Modifier.weight(1f),
                onClick = {
                    hint = StatHint(
                        title = "Точность",
                        message = "Доля правильных ответов среди всех: " +
                            "${progress.totalCorrect} из ${progress.totalAnswered}.",
                    )
                },
            )
            StatTile(
                label = "Освоено",
                value = StatsCalculator.masteredCount(progress).toString(),
                modifier = Modifier.weight(1f),
                onClick = {
                    hint = StatHint(
                        title = "Освоено",
                        message = "Вопросы, отвеченные не меньше 3 раз с точностью от 80%.",
                    )
                },
            )
        }

        if (!hasData) {
            Spacer(Modifier.height(16.dp))
            AppCard {
                Text(
                    "Пока нет данных",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Ответьте на несколько вопросов — здесь появится прогресс.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Spacer(Modifier.height(16.dp))
            StatsLearningCard(rules = rules, questions = questions, progress = progress)
            Spacer(Modifier.height(16.dp))
            StatsRadarCard(rules = rules, questions = questions, progress = progress)
            Spacer(Modifier.height(16.dp))
            SectionHeader("Уровни")
            Spacer(Modifier.height(6.dp))
            StatsLevelsCard(progress = progress, questions = questions)
        }
    }

    val shownHint = hint
    if (shownHint != null) {
        AlertDialog(
            onDismissRequest = { hint = null },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(shownHint.title, style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    text = shownHint.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = { hint = null }) { Text("Понятно") }
            },
        )
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

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = percent / 100f,
                label = formatPercent(percent),
                caption = "грамматики",
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Прогресс грамматики", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(3.dp))
                Text(
                    "$learned из $total ${pluralRu(total, "вопроса", "вопросов", "вопросов")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Тем начато: $startedTopics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatsRadarCard(
    rules: List<Rule>,
    questions: List<Question>,
    progress: ProgressState,
) {
    val metrics = remember(progress, rules, questions) {
        StatsCalculator.radarMetrics(progress, rules, questions)
    }
    AppCard {
        Text("Профиль знаний", style = MaterialTheme.typography.titleMedium)
        if (metrics.count { it.value > 0.02f } < 3) {
            Text(
                "Мало данных — ответьте ещё на несколько вопросов",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        RadarChart(
            metrics = metrics,
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp),
        )
    }
}

@Composable
private fun StatsLevelsCard(progress: ProgressState, questions: List<Question>) {
    val levelStats = StatsCalculator.levelStats(progress, questions)
    AppCard {
        if (levelStats.isEmpty()) {
            Text(
                "Пока нет ответов по уровням.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                levelStats.forEach { (level, stat) ->
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LevelChip(level = level)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${questionsLabel(stat.attempts)} · ${formatPercent(stat.accuracy)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AccuracyBar(accuracy = stat.accuracy)
                    }
                }
            }
        }
    }
}
