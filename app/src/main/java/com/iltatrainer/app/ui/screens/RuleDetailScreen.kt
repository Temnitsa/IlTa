package com.iltatrainer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
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
import com.iltatrainer.app.ui.components.AccuracyBar
import com.iltatrainer.app.ui.components.AppCard
import com.iltatrainer.app.ui.components.BackTopBar
import com.iltatrainer.app.ui.components.EmptyState
import com.iltatrainer.app.ui.components.LevelChip
import com.iltatrainer.app.ui.formatPercent
import com.iltatrainer.app.ui.localized

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
            title = shortRuleTitle(rule),
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
            Spacer(Modifier.height(8.dp))
            AccuracyBar(accuracy = themeProgress / 100f)
        }

        if (rule.sectionId != "tenses") {
            val summary = rule.summary.localized()
            if (summary.isNotBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium.copy(lineBreak = LineBreak.Heading),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
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
            )
        }
    }
}

@Composable
private fun RuleSectionCard(section: RuleSection) {
    val heading = section.heading.localized()
    val body = section.body.localized()
    val isFormula = heading.contains("Форм", ignoreCase = true) || heading.contains("Form", ignoreCase = true)

    AppCard {
        if (heading.isNotBlank()) {
            Text(
                text = heading,
                style = MaterialTheme.typography.titleMedium.copy(lineBreak = LineBreak.Heading),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
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
                section.examples.forEachIndexed { index, example ->
                    val translation = section.exampleTranslations.getOrNull(index).orEmpty()
                    val showTranslation = remember(example) { androidx.compose.runtime.mutableStateOf(false) }
                    Column {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(example, translation) {
                                    detectTapGestures(
                                        onTap = {
                                            if (showTranslation.value) showTranslation.value = false
                                        },
                                        onLongPress = {
                                            if (translation.isNotBlank()) showTranslation.value = true
                                        },
                                    )
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
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
                        if (showTranslation.value && translation.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .pointerInput(translation) {
                                        detectTapGestures(onTap = { showTranslation.value = false })
                                    },
                            ) {
                                Text(
                                    text = translation,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
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
@Composable
private fun shortRuleTitle(rule: Rule?): String {
    val full = rule?.title?.localized().orEmpty().trim()
    if (full.isBlank()) return "Тема"
    val base = full.substringBefore(":").substringBefore(" — ").substringBefore(" (").trim()
    val candidate = if (base.length >= 6) base else full
    if (candidate.length <= 34) return candidate
    val cut = candidate.take(34).substringBeforeLast(" ").trimEnd(' ', ',', ':', '—', '-')
    return if (cut.length >= 3) cut else candidate.take(34)
}

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
