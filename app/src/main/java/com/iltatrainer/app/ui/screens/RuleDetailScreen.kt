package com.iltatrainer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.iltatrainer.app.data.ProgressState
import com.iltatrainer.app.data.Question
import com.iltatrainer.app.data.Rule
import com.iltatrainer.app.data.RuleSection
import com.iltatrainer.app.domain.TOPIC_SESSION_SIZE
import com.iltatrainer.app.domain.StatsCalculator
import com.iltatrainer.app.ui.components.AccuracyBar
import com.iltatrainer.app.ui.components.AppCard
import com.iltatrainer.app.ui.components.BackTopBar
import com.iltatrainer.app.ui.components.EmptyState
import com.iltatrainer.app.ui.components.LevelChip
import com.iltatrainer.app.ui.components.levelColor
import com.iltatrainer.app.ui.formatPercent
import com.iltatrainer.app.ui.localized
import com.iltatrainer.app.ui.questionsLabel
import com.iltatrainer.app.ui.tr

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
            title = if (rule != null) rule.title.localized() else tr("Тема", "Topic"),
            onBack = onBack,
        )
        if (rule == null) {
            EmptyState(
                emoji = "🤔",
                title = tr("Тема не найдена", "Topic not found"),
                message = tr(
                    "Возможно, контент приложения обновился. Вернитесь к списку правил и выберите тему снова.",
                    "The app content may have changed. Go back to the list of rules and pick a topic again.",
                ),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
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
    val accent = levelColor(rule.level)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ThemeProgressCard(
            progressPercent = themeProgress,
            poolCount = questions.size,
            accent = accent,
            level = rule.level,
        )

        if (rule.sections.isEmpty()) {
            AppCard {
                Text(
                    text = tr(
                        "Пояснений к этой теме пока нет — тренируйтесь сразу на вопросах.",
                        "There is no explanation for this topic yet — practise with the questions instead.",
                    ),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineBreak = LineBreak.Heading,
                        lineHeight = 26.sp,
                    ),
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
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = tr("Тренировать тему", "Practise this topic"),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Text(
                    text = questionsLabel(TOPIC_SESSION_SIZE),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ThemeProgressCard(progressPercent: Int, poolCount: Int, accent: Color, level: String) {
    val sessionText = questionsLabel(TOPIC_SESSION_SIZE)
    val poolText = questionsLabel(poolCount)
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LevelChip(level = level)
            Spacer(Modifier.width(8.dp))
            Text(
                text = tr("Прогресс темы", "Theme progress"),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatPercent(progressPercent),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }
        Spacer(Modifier.height(8.dp))
        AccuracyBar(accuracy = progressPercent / 100f, color = accent)
        Spacer(Modifier.height(8.dp))
        Text(
            text = tr(
                "$sessionText в тренировке · $poolText в теме",
                "$sessionText per session · $poolText in this topic",
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RuleSectionCard(section: RuleSection) {
    val heading = section.heading.localized()
    val body = section.body.localized()
    val isFormula = heading.contains("Форм", ignoreCase = true) || heading.contains("Form", ignoreCase = true)

    AppCard {
        if (heading.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)),
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = heading,
                    style = MaterialTheme.typography.titleMedium.copy(lineBreak = LineBreak.Heading),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (body.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            if (isFormula) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        body.lines().filter { it.isNotBlank() }.forEach { line ->
                            Text(
                                text = remember(line) { formatRuleText(line) },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 24.sp,
                                ),
                            )
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    body.lines().filter { it.isNotBlank() }.forEach { rawLine ->
                        val line = rawLine.trim()
                        if (line.startsWith("•")) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = remember(line) { formatRuleText(line.removePrefix("•").trimStart()) },
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        lineBreak = LineBreak.Heading,
                                        lineHeight = 24.sp,
                                    ),
                                )
                            }
                        } else {
                            Text(
                                text = remember(line) { formatRuleText(line) },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineBreak = LineBreak.Heading,
                                    lineHeight = 24.sp,
                                ),
                            )
                        }
                    }
                }
            }
        }

        if (section.examples.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                section.examples.forEach { example ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 11.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = remember(example) { formatRuleText(example) },
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
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
 * `**bold**` becomes bold and `*italic*` becomes italic. Bullet lines that start with
 * "• " stay on their own line because newlines are kept as-is.
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
                            fontWeight = if (bold) FontWeight.Bold else null,
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
