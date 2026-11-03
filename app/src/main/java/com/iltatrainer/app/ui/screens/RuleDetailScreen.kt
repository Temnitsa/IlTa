package com.iltatrainer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iltatrainer.app.R
import com.iltatrainer.app.data.ProgressState
import com.iltatrainer.app.data.Question
import com.iltatrainer.app.data.Rule
import com.iltatrainer.app.data.RuleSection
import com.iltatrainer.app.domain.StatsCalculator
import com.iltatrainer.app.domain.TOPIC_SESSION_SIZE
import com.iltatrainer.app.ui.components.AccuracyBar
import com.iltatrainer.app.ui.components.AppCard
import com.iltatrainer.app.ui.components.BackTopBar
import com.iltatrainer.app.ui.components.EmptyState
import com.iltatrainer.app.ui.components.LevelChip
import com.iltatrainer.app.ui.formatPercent
import com.iltatrainer.app.ui.localized
import com.iltatrainer.app.ui.questionsLabel

@Composable
fun RuleDetailScreen(
    rule: Rule?,
    questions: List<Question>,
    progress: ProgressState,
    onBack: () -> Unit,
    onTrain: (ruleId: String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(
            title = rule?.title?.localized() ?: "Тема",
            onBack = onBack,
        )
        if (rule == null) {
            EmptyState(
                title = "Тема не найдена",
                message = "Вернитесь к списку тем и выберите другую.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            )
        } else {
            RuleDetailContent(
                rule = rule,
                questions = questions,
                progress = progress,
                onTrain = onTrain,
            )
        }
    }
}

@Composable
private fun RuleDetailContent(
    rule: Rule,
    questions: List<Question>,
    progress: ProgressState,
    onTrain: (String) -> Unit,
) {
    val themeProgress = StatsCalculator.ruleProgress(progress, questions)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LevelChip(level = rule.level)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Прогресс темы",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatPercent(themeProgress),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            val summary = rule.summary.localized()
            if (summary.isNotBlank() && rule.sectionId != "tenses") {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium.copy(lineBreak = LineBreak.Heading),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            AccuracyBar(accuracy = themeProgress / 100f)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${questionsLabel(TOPIC_SESSION_SIZE)} в тренировке · " +
                    "${questionsLabel(questions.size)} в теме",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (rule.sections.isEmpty()) {
            AppCard {
                Text(
                    text = "Пояснений к этой теме пока нет.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            rule.sections.forEach { section ->
                RuleSectionCard(section = section)
            }
        }

        Button(
            onClick = { onTrain(rule.id) },
            enabled = questions.isNotEmpty(),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
        ) {
            Text(
                text = "Тренировать тему",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = questionsLabel(TOPIC_SESSION_SIZE),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun RuleSectionCard(section: RuleSection) {
    val heading = section.heading.localized()
    val body = section.body.localized()
    val isFormula = heading.contains("Форм", ignoreCase = true) || heading.contains("Form", ignoreCase = true)
    val iconRes = when {
        isFormula -> R.drawable.ic_rule_form
        heading.contains("Маркер", ignoreCase = true) || heading.contains("Marker", ignoreCase = true) ->
            R.drawable.ic_rule_markers
        else -> R.drawable.ic_rule_when
    }

    AppCard {
        if (heading.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp),
                    )
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    text = heading,
                    style = MaterialTheme.typography.titleMedium.copy(lineBreak = LineBreak.Heading),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (body.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            if (isFormula) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        body.lines().filter { it.isNotBlank() }.forEach { line ->
                            Text(
                                text = remember(line) { formatRuleText(line) },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 22.sp,
                                ),
                            )
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    body.lines().filter { it.isNotBlank() }.forEach { rawLine ->
                        val line = rawLine.trim()
                        if (line.startsWith("•")) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = remember(line) {
                                        formatRuleText(line.removePrefix("•").trimStart())
                                    },
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        lineBreak = LineBreak.Heading,
                                        lineHeight = 22.sp,
                                    ),
                                )
                            }
                        } else {
                            Text(
                                text = remember(line) { formatRuleText(line) },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineBreak = LineBreak.Heading,
                                    lineHeight = 22.sp,
                                ),
                            )
                        }
                    }
                }
            }
        }

        if (section.examples.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
            Spacer(Modifier.height(9.dp))
            Text(
                text = "Примеры",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                section.examples.forEach { example ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                text = "→",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = remember(example) { formatRuleText(example) },
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders the simple markup used by rule bodies: line breaks are preserved exactly,
 * `**bold**` becomes bold and `*italic*` becomes italic.
 */
private fun formatRuleText(raw: String): AnnotatedString = buildAnnotatedString {
    var bold = false
    var italic = false
    raw.split(Regex("(\\*\\*|\\*)")).forEach { chunk ->
        when (chunk) {
            "**" -> bold = !bold
            "*" -> italic = !italic
            else -> {
                if (chunk.isEmpty()) return@forEach
                if (bold || italic) {
                    withStyle(
                        SpanStyle(
                            fontWeight = if (bold) FontWeight.SemiBold else null,
                            fontStyle = if (italic) FontStyle.Italic else null,
                        ),
                    ) {
                        append(chunk)
                    }
                } else {
                    append(chunk)
                }
            }
        }
    }
}
