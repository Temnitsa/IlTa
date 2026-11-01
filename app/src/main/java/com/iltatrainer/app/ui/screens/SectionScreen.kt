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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iltatrainer.app.data.ProgressState
import com.iltatrainer.app.data.Question
import com.iltatrainer.app.data.Rule
import com.iltatrainer.app.data.Section
import com.iltatrainer.app.domain.StatsCalculator
import com.iltatrainer.app.ui.components.AccuracyBar
import com.iltatrainer.app.ui.components.AppCard
import com.iltatrainer.app.ui.components.BackTopBar
import com.iltatrainer.app.ui.components.EmptyState
import com.iltatrainer.app.ui.components.LevelChip
import com.iltatrainer.app.ui.components.SectionHeader
import com.iltatrainer.app.ui.components.levelColor
import com.iltatrainer.app.ui.formatPercent
import com.iltatrainer.app.ui.localized
import com.iltatrainer.app.ui.pluralRu
import com.iltatrainer.app.ui.questionsLabel
import com.iltatrainer.app.ui.tr

@Composable
fun SectionScreen(
    section: Section?,
    rules: List<Rule>,
    questions: List<Question>,
    questionsByRule: Map<String, Int>,
    progress: ProgressState,
    onBack: () -> Unit,
    onOpenRule: (ruleId: String) -> Unit,
    onPracticeSection: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(
            title = if (section != null) section.title.localized() else tr("Раздел", "Section"),
            onBack = onBack,
        )
        if (section == null) {
            EmptyState(
                emoji = "🤔",
                title = tr("Раздел не найден", "Section not found"),
                message = tr(
                    "Возможно, контент приложения обновился. Вернитесь к списку разделов.",
                    "The app content may have changed. Go back to the list of sections.",
                ),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            )
        } else {
            SectionContent(
                section = section,
                sectionRules = rules.filter { it.sectionId == section.id },
                questions = questions,
                questionsByRule = questionsByRule,
                progress = progress,
                onOpenRule = onOpenRule,
                onPracticeSection = onPracticeSection,
            )
        }
    }
}

@Composable
private fun SectionContent(
    section: Section,
    sectionRules: List<Rule>,
    questions: List<Question>,
    questionsByRule: Map<String, Int>,
    progress: ProgressState,
    onOpenRule: (ruleId: String) -> Unit,
    onPracticeSection: () -> Unit,
) {
    val sectionProgress = StatsCalculator.sectionProgress(progress, sectionRules, questions)
    val questionCount = sectionRules.sumOf { rule -> questionsByRule[rule.id] ?: 0 }
    val countsText = sectionCountsLabel(topicCount = sectionRules.size, questionCount = questionCount)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "summary") {
            SectionSummaryCard(
                section = section,
                countsText = countsText,
                progressPercent = sectionProgress,
            )
        }

        item(key = "practice-button") {
            Button(
                onClick = onPracticeSection,
                enabled = questionCount > 0,
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            ) {
                Text(
                    text = tr("Тренировать раздел", "Practise this section"),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
            }
        }

        item(key = "topics-header") {
            SectionHeader(tr("Темы", "Topics"))
        }

        if (sectionRules.isEmpty()) {
            item(key = "empty-topics") {
                EmptyState(
                    emoji = "📘",
                    title = tr("В разделе пока нет тем", "No topics in this section yet"),
                    message = tr(
                        "Проверьте встроенный контент приложения.",
                        "Check the app's bundled content.",
                    ),
                )
            }
        } else {
            items(items = sectionRules, key = { it.id }) { rule ->
                val themeQuestions = questions.filter { it.ruleId == rule.id }
                RuleRowCard(
                    rule = rule,
                    questionCount = questionsByRule[rule.id] ?: themeQuestions.size,
                    progressPercent = StatsCalculator.ruleProgress(progress, themeQuestions),
                    onClick = { onOpenRule(rule.id) },
                )
            }
        }
    }
}

@Composable
private fun SectionSummaryCard(
    section: Section,
    countsText: String,
    progressPercent: Int,
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(section.emoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = countsText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatPercent(progressPercent),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(8.dp))
        AccuracyBar(accuracy = progressPercent / 100f)
    }
}

@Composable
private fun RuleRowCard(
    rule: Rule,
    questionCount: Int,
    progressPercent: Int,
    onClick: () -> Unit,
) {
    val accent = levelColor(rule.level)
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = rule.title.localized(),
                        style = MaterialTheme.typography.titleMedium.copy(lineBreak = LineBreak.Heading),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    LevelChip(level = rule.level)
                }
                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AccuracyBar(
                        accuracy = progressPercent / 100f,
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp),
                        color = accent,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = formatPercent(progressPercent),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = questionsLabel(questionCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun sectionCountsLabel(topicCount: Int, questionCount: Int): String {
    val questionsText = questionsLabel(questionCount)
    return tr(
        "$topicCount ${pluralRu(topicCount, "тема", "темы", "тем")} · $questionsText",
        "$topicCount ${if (topicCount == 1) "topic" else "topics"} · $questionsText",
    )
}
