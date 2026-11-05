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
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.iltatrainer.app.ui.components.SectionIcon
import com.iltatrainer.app.ui.components.SectionHeader
import com.iltatrainer.app.ui.formatPercent
import com.iltatrainer.app.ui.localized
import com.iltatrainer.app.ui.pluralRu
import com.iltatrainer.app.ui.questionsLabel

@Composable
fun SectionScreen(
    section: Section?,
    rules: List<Rule>,
    questions: List<Question>,
    questionsByRule: Map<String, Int>,
    progress: ProgressState,
    sessionSize: Int,
    onBack: () -> Unit,
    onOpenRule: (ruleId: String) -> Unit,
    onPracticeSection: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(
            title = section?.title?.localized() ?: "Раздел",
            onBack = onBack,
        )
        if (section == null) {
            EmptyState(
                title = "Раздел не найден",
                message = "Вернитесь к списку разделов и выберите другой.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            )
        } else {
            SectionContent(
                section = section,
                sectionRules = rules.filter { it.sectionId == section.id },
                sessionSize = sessionSize,
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
    sessionSize: Int,
    onOpenRule: (ruleId: String) -> Unit,
    onPracticeSection: () -> Unit,
) {
    val sectionProgress = StatsCalculator.sectionProgress(progress, sectionRules, questions)
    val questionCount = sectionRules.sumOf { rule -> questionsByRule[rule.id] ?: 0 }
    val countsText = "${sectionRules.size} ${pluralRu(sectionRules.size, "тема", "темы", "тем")} · " +
        questionsLabel(questionCount)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "summary") {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionIcon(sectionId = section.id, size = 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = countsText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatPercent(sectionProgress),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Spacer(Modifier.height(10.dp))
                AccuracyBar(accuracy = sectionProgress / 100f)
            }
        }

        item(key = "practice-button") {
            Button(
                onClick = onPracticeSection,
                enabled = questionCount > 0,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
            ) {
                Text(
                    text = "Тренировать раздел",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = questionsLabel(sessionSize),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        item(key = "topics-header") {
            SectionHeader("Темы")
        }

        if (sectionRules.isEmpty()) {
            item(key = "empty-topics") {
                EmptyState(
                    title = "В разделе пока нет тем",
                    message = "Проверьте встроенный контент приложения.",
                )
            }
        } else {
            items(items = sectionRules, key = { it.id }) { rule ->
                RuleRowCard(
                    rule = rule,
                    questionCount = questionsByRule[rule.id] ?: 0,
                    progressPercent = StatsCalculator.ruleProgress(
                        progress,
                        questions.filter { it.ruleId == rule.id },
                    ),
                    onClick = { onOpenRule(rule.id) },
                )
            }
        }
    }
}

@Composable
private fun RuleRowCard(
    rule: Rule,
    questionCount: Int,
    progressPercent: Int,
    onClick: () -> Unit,
) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = rule.title.localized(),
                style = MaterialTheme.typography.titleMedium,
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
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = formatPercent(progressPercent),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
