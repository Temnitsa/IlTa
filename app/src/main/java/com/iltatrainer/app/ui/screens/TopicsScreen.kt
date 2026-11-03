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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iltatrainer.app.data.ProgressState
import com.iltatrainer.app.data.Question
import com.iltatrainer.app.data.Rule
import com.iltatrainer.app.data.Section
import com.iltatrainer.app.domain.SessionBuilder
import com.iltatrainer.app.domain.StatsCalculator
import com.iltatrainer.app.ui.components.AccuracyBar
import com.iltatrainer.app.ui.components.AppCard
import com.iltatrainer.app.ui.components.EmptyState
import com.iltatrainer.app.ui.components.MonogramBadge
import com.iltatrainer.app.ui.components.SectionHeader
import com.iltatrainer.app.ui.formatPercent
import com.iltatrainer.app.ui.localized
import com.iltatrainer.app.ui.pluralRu
import com.iltatrainer.app.ui.questionsLabel

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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") {
            Text("IlTa", style = MaterialTheme.typography.headlineMedium)
        }

        item(key = "learning") {
            LearningProgressCard(
                rules = rules,
                questions = questions,
                progress = progress,
            )
        }

        item(key = "quick-practice") {
            Button(
                onClick = { showPicker = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
            ) {
                Text(
                    text = "Тренировка",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = questionsLabel(sessionSize),
                    style = MaterialTheme.typography.labelSmall,
                )
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
            SectionHeader("Разделы")
        }

        if (sections.isEmpty()) {
            item(key = "empty-sections") {
                EmptyState(
                    title = "Разделов пока нет",
                    message = "Проверьте встроенный контент приложения.",
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
    }

    if (showPicker) {
        QuickPracticePickerSheet(
            sections = sections,
            rules = rules,
            questionsByRule = questionsByRule,
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
private fun LearningProgressCard(
    rules: List<Rule>,
    questions: List<Question>,
    progress: ProgressState,
) {
    val percent = StatsCalculator.learningProgress(progress, rules, questions)
    val learned = questions.count { question -> (progress.questionStats[question.id]?.correct ?: 0) > 0 }

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Прогресс грамматики",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatPercent(percent),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Spacer(Modifier.height(8.dp))
        AccuracyBar(accuracy = percent / 100f)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "$learned из ${questions.size} " +
                pluralRu(questions.size, "вопроса", "вопросов", "вопросов"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MistakeReviewCard(mistakeCount: Int, onClick: () -> Unit) {
    val enabled = mistakeCount > 0
    val caption = if (enabled) {
        "$mistakeCount ${pluralRu(mistakeCount, "ошибка", "ошибки", "ошибок")} в работе"
    } else {
        "Пока нет ошибок"
    }
    AppCard(
        onClick = if (enabled) onClick else null,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Работа над ошибками",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (enabled) {
                Text(
                    text = "›",
                    style = MaterialTheme.typography.titleLarge,
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
    val percent = StatsCalculator.sectionProgress(progress, sectionRules, questions)
    val questionCount = sectionRules.sumOf { rule -> questionsByRule[rule.id] ?: 0 }
    val countsText = "${sectionRules.size} ${pluralRu(sectionRules.size, "тема", "темы", "тем")} · " +
        questionsLabel(questionCount)

    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonogramBadge(text = section.title.localized(), size = 34.dp)
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
            Text(
                text = formatPercent(percent),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        AccuracyBar(accuracy = percent / 100f)
        Spacer(Modifier.height(6.dp))
        Text(
            text = countsText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickPracticePickerSheet(
    sections: List<Section>,
    rules: List<Rule>,
    questionsByRule: Map<String, Int>,
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
        "choice" to "Выбор",
        "match" to "Соответствие",
        "build" to "Сборка",
    )
    val selectedQuestionCount = remember(selectedRuleIds, questionsByRule) {
        selectedRuleIds.sumOf { ruleId -> questionsByRule[ruleId] ?: 0 }
    }
    val selectedCount = selectedRuleIds.size
    val allTypesSelected = selectedTypes.size == ALL_EXERCISE_TYPES.size
    val selectedSummary = "Выбрано: $selectedCount " +
        pluralRu(selectedCount, "тема", "темы", "тем") + " · " + questionsLabel(selectedQuestionCount)
    val typesSummary = if (!allTypesSelected) {
        val labels = typeOptions.filter { (type, _) -> type in selectedTypes }.map { it.second }
        if (labels.isEmpty()) "Тип заданий не выбран" else "Типы: ${labels.joinToString(", ")}"
    } else {
        null
    }
    val canStart = selectedCount > 0 && selectedTypes.isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Text(
                text = "Тренировка",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Тип заданий",
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
                    TypeChip(label = label, selected = type in selectedTypes) {
                        onToggleType(type)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSelectAll) {
                    Text("Выбрать все")
                }
                TextButton(onClick = onClearAll) {
                    Text("Снять все")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
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
                                    .padding(vertical = 4.dp),
                            ) {
                                Checkbox(
                                    checked = allSelected,
                                    onCheckedChange = { onToggleSection(section.id) },
                                )
                                Text(
                                    text = section.title.localized(),
                                    style = MaterialTheme.typography.labelLarge,
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
                                    style = MaterialTheme.typography.labelLarge,
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(10.dp))
            Text(
                text = selectedSummary,
                style = MaterialTheme.typography.titleMedium,
            )
            if (typesSummary != null) {
                Text(
                    text = typesSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onStart,
                enabled = canStart,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
            ) {
                Text("Начать тренировку")
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
            .clickable(onClick = onToggle)
            .padding(start = 30.dp, top = 2.dp, bottom = 2.dp),
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
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ContentWarningCard(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = "Контент загрузился не полностью",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
