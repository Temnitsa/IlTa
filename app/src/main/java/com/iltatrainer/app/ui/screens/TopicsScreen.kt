package com.iltatrainer.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iltatrainer.app.data.ProgressState
import com.iltatrainer.app.data.Question
import com.iltatrainer.app.data.Rule
import com.iltatrainer.app.data.Section
import com.iltatrainer.app.domain.SessionBuilder
import com.iltatrainer.app.domain.StatsCalculator
import com.iltatrainer.app.ui.components.AccuracyBar
import com.iltatrainer.app.ui.components.AppCard
import com.iltatrainer.app.ui.components.EmptyState
import com.iltatrainer.app.ui.components.SectionHeader
import com.iltatrainer.app.ui.formatPercent
import com.iltatrainer.app.ui.localized
import com.iltatrainer.app.ui.pluralRu
import com.iltatrainer.app.ui.questionsLabel
import com.iltatrainer.app.ui.tr

/** Exercise types available in the quick-practice filter (Question.type values). */
private val ALL_EXERCISE_TYPES = setOf("choice", "match", "build")

@Composable
fun TopicsScreen(
    sections: List<Section>,
    rules: List<Rule>,
    questions: List<Question>,
    progress: ProgressState,
    contentErrors: List<String>,
    onStartQuickPractice: (selectedRuleIds: List<String>, selectedTypes: Set<String>) -> Unit,
    onStartMistakes: () -> Unit,
    onOpenSection: (sectionId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sessionSize = progress.settings.sessionSize
    val streak = StatsCalculator.streak(progress)
    val openMistakesCount = SessionBuilder.openMistakeCount(progress)
    val rulesBySection = remember(rules) { rules.groupBy { it.sectionId } }
    val questionsByRule = remember(questions) { questions.groupingBy { it.ruleId }.eachCount() }

    var showPicker by rememberSaveable { mutableStateOf(false) }
    var selectedRuleIds by rememberSaveable { mutableStateOf(rules.map { it.id }) }
    var selectedTypes by rememberSaveable { mutableStateOf(ALL_EXERCISE_TYPES) }

    fun toggleSection(sectionId: String) {
        val sectionRuleIds = rulesBySection[sectionId].orEmpty().map { it.id }
        if (sectionRuleIds.isEmpty()) return
        val allSelected = sectionRuleIds.all { it in selectedRuleIds }
        selectedRuleIds = if (allSelected) {
            selectedRuleIds.filterNot { it in sectionRuleIds }
        } else {
            (selectedRuleIds + sectionRuleIds).distinct()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") {
            TopicsHeader(streak = streak)
        }

        item(key = "learning-progress") {
            LearningProgressCard(
                sections = sections,
                rules = rules,
                questions = questions,
                progress = progress,
            )
        }

        item(key = "quick-practice") {
            Button(
                onClick = { showPicker = true },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = tr("Тренировка", "Practice"),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = tr(
                            "${questionsLabel(sessionSize)} · выбрать темы",
                            "${questionsLabel(sessionSize)} · choose topics",
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    )
                }
            }
        }

        item(key = "mistake-review") {
            MistakeReviewCard(
                mistakeCount = openMistakesCount,
                onClick = onStartMistakes,
            )
        }

        if (contentErrors.isNotEmpty()) {
            item(key = "content-error") {
                ContentWarningCard(contentErrors.first())
            }
        }

        item(key = "sections-header") {
            SectionHeader(tr("Разделы", "Sections"))
        }

        if (sections.isEmpty()) {
            item(key = "empty-sections") {
                EmptyState(
                    emoji = "📚",
                    title = tr("Разделы не загрузились", "Sections not loaded"),
                    message = tr(
                        "Проверьте встроенный контент приложения и перезапустите его.",
                        "Check the app's bundled content and restart it.",
                    ),
                )
            }
        } else {
            items(items = sections, key = { it.id }) { section ->
                SectionCard(
                    section = section,
                    sectionRules = rulesBySection[section.id].orEmpty(),
                    questionsByRule = questionsByRule,
                    questions = questions,
                    progress = progress,
                    onClick = { onOpenSection(section.id) },
                )
            }
        }

        item(key = "footer") {
            Text(
                text = tr(
                    "Всё работает офлайн. Прогресс хранится только на этом устройстве.",
                    "Fully offline. Progress is stored on this device only.",
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }

    if (showPicker) {
        QuickPracticePickerSheet(
            sections = sections,
            rules = rules,
            questionsByRule = questionsByRule,
            sessionSize = sessionSize,
            selectedRuleIds = selectedRuleIds,
            selectedTypes = selectedTypes,
            onToggleRule = { ruleId ->
                selectedRuleIds = if (ruleId in selectedRuleIds) {
                    selectedRuleIds - ruleId
                } else {
                    selectedRuleIds + ruleId
                }
            },
            onSelectAll = { selectedRuleIds = rules.map { it.id } },
            onClearAll = { selectedRuleIds = emptyList() },
            onToggleSection = { sectionId -> toggleSection(sectionId) },
            onToggleType = { type ->
                selectedTypes = if (type in selectedTypes) selectedTypes - type else selectedTypes + type
            },
            onDismiss = { showPicker = false },
            onStart = {
                val ruleIds = selectedRuleIds.toList()
                val types = selectedTypes.toSet()
                showPicker = false
                if (ruleIds.isNotEmpty() && types.isNotEmpty()) {
                    onStartQuickPractice(ruleIds, types)
                }
            },
        )
    }
}

@Composable
private fun TopicsHeader(streak: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tr("IlTa", "IlTa"),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = tr("Тренажёр английской грамматики", "English grammar trainer"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        StreakBadge(streak = streak)
    }
}

@Composable
private fun StreakBadge(streak: Int) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text("🔥", fontSize = 16.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                text = "$streak",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun LearningProgressCard(
    sections: List<Section>,
    rules: List<Rule>,
    questions: List<Question>,
    progress: ProgressState,
) {
    val percent = StatsCalculator.learningProgress(progress, rules, questions)
    val learned = questions.count { question -> (progress.questionStats[question.id]?.correct ?: 0) > 0 }
    val total = questions.size
    val titlePrefix = tr("Вы изучили ", "You've learned ")
    val titleSuffix = tr(" грамматики", " of the grammar")
    val percentText = formatPercent(percent)
    val title = buildAnnotatedString {
        append(titlePrefix)
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(percentText)
        }
        append(titleSuffix)
    }
    val learnedLine = tr(
        "$learned из $total ${pluralRu(total, "вопроса", "вопросов", "вопросов")}",
        "$learned of $total ${if (total == 1) "question" else "questions"}",
    )

    AppCard {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = learnedLine,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        AccuracyBar(accuracy = percent / 100f)
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            sections.forEach { section ->
                val sectionRules = rules.filter { it.sectionId == section.id }
                SectionLearningRow(
                    section = section,
                    percent = StatsCalculator.sectionProgress(progress, sectionRules, questions),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = tr(
                "Чем больше отвечаете — тем выше процент",
                "The more you practise, the higher it goes",
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionLearningRow(section: Section, percent: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(section.emoji, fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = section.title.localized(),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatPercent(percent),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        ThinProgressBar(progress = percent / 100f)
    }
}

@Composable
private fun ThinProgressBar(progress: Float) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickPracticePickerSheet(
    sections: List<Section>,
    rules: List<Rule>,
    questionsByRule: Map<String, Int>,
    sessionSize: Int,
    selectedRuleIds: List<String>,
    selectedTypes: Set<String>,
    onToggleRule: (ruleId: String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onToggleSection: (String) -> Unit,
    onToggleType: (String) -> Unit,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedRuleIdSet = remember(selectedRuleIds) { selectedRuleIds.toSet() }
    var expandedSectionIds by remember { mutableStateOf(emptySet<String>()) }
    val typeOptions = listOf(
        "choice" to tr("Выбор", "Choice"),
        "match" to tr("Соответствие", "Match"),
        "build" to tr("Сборка", "Build"),
    )
    val selectedQuestionCount = remember(selectedRuleIds, questionsByRule) {
        selectedRuleIds.sumOf { ruleId -> questionsByRule[ruleId] ?: 0 }
    }
    val selectedCount = selectedRuleIds.size
    val allTypesSelected = selectedTypes.size == ALL_EXERCISE_TYPES.size
    val selectedSummary = tr(
        "Выбрано: $selectedCount ${pluralRu(selectedCount, "тема", "темы", "тем")} · ${questionsLabel(selectedQuestionCount)}",
        "Selected: $selectedCount ${if (selectedCount == 1) "topic" else "topics"} · ${questionsLabel(selectedQuestionCount)}",
    )
    val typesSummary = if (!allTypesSelected) {
        val labels = typeOptions
            .filter { (type, _) -> type in selectedTypes }
            .map { it.second }
        if (labels.isEmpty()) {
            tr("Тип заданий не выбран", "No exercise type selected")
        } else {
            tr("Типы: ${labels.joinToString(", ")}", "Types: ${labels.joinToString(", ")}")
        }
    } else {
        null
    }
    val sessionLine = tr(
        "В одной тренировке: ${questionsLabel(sessionSize)}",
        "Session size: ${questionsLabel(sessionSize)}",
    )
    val canStart = selectedCount > 0 && selectedTypes.isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            Text(
                text = tr("Темы для тренировки", "Topics for practice"),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = tr(
                    "Выберите одну или несколько тем — можно собрать микс",
                    "Pick one or more topics — mix them freely",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = tr("Тип заданий", "Exercise types"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                typeOptions.forEach { (type, label) ->
                    FilterChip(
                        selected = type in selectedTypes,
                        onClick = { onToggleType(type) },
                        label = { Text(label) },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSelectAll) {
                    Text(tr("Выбрать все", "Select all"))
                }
                TextButton(onClick = onClearAll) {
                    Text(tr("Снять все", "Clear"))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                sections.forEach { section ->
                    val sectionRules = rules.filter { it.sectionId == section.id }
                    if (sectionRules.isNotEmpty()) {
                        val sectionRuleIds = sectionRules.map { it.id }
                        val allSelected = sectionRuleIds.all { it in selectedRuleIdSet }
                        val selectedInSection = sectionRuleIds.count { it in selectedRuleIdSet }
                        val expanded = section.id in expandedSectionIds
                        item(key = "sheet-section-${section.id}") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedSectionIds = if (expanded) {
                                            expandedSectionIds - section.id
                                        } else {
                                            expandedSectionIds + section.id
                                        }
                                    }
                                    .padding(top = 8.dp, bottom = 2.dp),
                            ) {
                                Checkbox(
                                    checked = allSelected,
                                    onCheckedChange = { onToggleSection(section.id) },
                                )
                                Text(section.emoji, fontSize = 18.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = section.title.localized(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (selectedInSection > 0 && !allSelected) {
                                    Text(
                                        text = "$selectedInSection/${sectionRuleIds.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (expanded) "▾" else "▸",
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (expanded) {
                            items(items = sectionRules, key = { it.id }) { rule ->
                                RuleCheckRow(
                                    rule = rule,
                                    checked = rule.id in selectedRuleIdSet,
                                    onToggle = { onToggleRule(rule.id) },
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Spacer(Modifier.height(10.dp))
            Text(
                text = selectedSummary,
                style = MaterialTheme.typography.titleMedium,
            )
            if (typesSummary != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = typesSummary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = sessionLine,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onStart,
                enabled = canStart,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(tr("Начать тренировку", "Start practice"))
            }
        }
    }
}

@Composable
private fun RuleCheckRow(
    rule: Rule,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
        )
        Text(
            text = rule.title.localized(),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MistakeReviewCard(mistakeCount: Int, onClick: () -> Unit) {
    val enabled = mistakeCount > 0
    val caption = if (enabled) {
        tr(
            "$mistakeCount ${pluralRu(mistakeCount, "ошибка", "ошибки", "ошибок")} в работе",
            "$mistakeCount ${if (mistakeCount == 1) "mistake" else "mistakes"} to review",
        )
    } else {
        tr("Пока нет ошибок", "No open mistakes")
    }
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.55f),
        onClick = if (enabled) onClick else null,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎯", fontSize = 26.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr("Работа над ошибками", "Mistake review"),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (enabled) {
                Text(
                    text = "›",
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    section: Section,
    sectionRules: List<Rule>,
    questionsByRule: Map<String, Int>,
    questions: List<Question>,
    progress: ProgressState,
    onClick: () -> Unit,
) {
    val sectionProgress = StatsCalculator.sectionProgress(progress, sectionRules, questions)
    val questionCount = sectionRules.sumOf { rule -> questionsByRule[rule.id] ?: 0 }
    val countsText = sectionCountsLabel(topicCount = sectionRules.size, questionCount = questionCount)

    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(section.emoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.title.localized(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = section.short.localized(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "›",
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = section.description.localized(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AccuracyBar(
                accuracy = sectionProgress / 100f,
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = formatPercent(sectionProgress),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = countsText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

@Composable
private fun ContentWarningCard(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = tr("Часть контента не загрузилась", "Some content failed to load"),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
