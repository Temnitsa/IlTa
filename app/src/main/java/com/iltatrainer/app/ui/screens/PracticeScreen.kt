package com.iltatrainer.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iltatrainer.app.core.AppLanguage
import com.iltatrainer.app.data.ProgressState
import com.iltatrainer.app.data.Question
import com.iltatrainer.app.data.Rule
import com.iltatrainer.app.domain.AnswerChecker
import com.iltatrainer.app.domain.SessionBuilder
import com.iltatrainer.app.domain.SessionConfig
import com.iltatrainer.app.domain.SessionMistake
import com.iltatrainer.app.domain.SessionResult
import com.iltatrainer.app.ui.components.EmptyState
import com.iltatrainer.app.ui.components.LevelChip
import com.iltatrainer.app.ui.components.SentenceText
import com.iltatrainer.app.ui.itemLabel
import com.iltatrainer.app.ui.tr
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun PracticeScreen(
    config: SessionConfig,
    title: String,
    allQuestions: List<Question>,
    rulesById: Map<String, Rule>,
    progress: ProgressState,
    language: AppLanguage,
    onAnswer: (Question, Boolean) -> Unit,
    onExit: () -> Unit,
    onFinish: (SessionResult) -> Unit,
) {
    val questions = remember(config) {
        SessionBuilder.build(config, allQuestions, progress, Random.Default)
    }

    if (questions.isEmpty()) {
        BackHandler { onExit() }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            EmptyState(
                title = tr("Пока здесь пусто", "Nothing here yet"),
                message = tr(
                    "Ответьте на несколько вопросов, и этот режим наполнится.",
                    "Answer a few questions and this mode will fill up.",
                ),
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onExit) {
                Text(tr("На главную", "Go home"))
            }
        }
        return
    }

    var index by rememberSaveable { mutableIntStateOf(0) }
    var correctCount by rememberSaveable { mutableIntStateOf(0) }
    var answeredCount by rememberSaveable { mutableIntStateOf(0) }
    val mistakes = remember { mutableStateListOf<SessionMistake>() }
    var showExitDialog by remember { mutableStateOf(false) }

    val current = questions[index]
    val isLast = index == questions.lastIndex

    BackHandler { showExitDialog = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        PracticeTopBar(
            title = title,
            counter = "${index + 1}/${questions.size}",
            fraction = (index + 1).toFloat() / questions.size,
            onClose = { showExitDialog = true },
        )
        key(current.id) {
            QuestionBody(
                question = current,
                rule = rulesById[current.ruleId],
                language = language,
                isLast = isLast,
                modifier = Modifier.weight(1f),
                onAnswered = { correct, userInput ->
                    if (answeredCount == index) {
                        answeredCount += 1
                        if (correct) {
                            correctCount += 1
                        } else {
                            mistakes.add(SessionMistake(current, userInput))
                        }
                        onAnswer(current, correct)
                    }
                },
                onNext = {
                    if (isLast) {
                        onFinish(
                            SessionResult(
                                total = questions.size,
                                correct = correctCount,
                                mistakes = mistakes.toList(),
                            ),
                        )
                    } else {
                        index += 1
                    }
                },
            )
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(tr("Выйти из тренировки?", "Leave practice?")) },
            text = {
                Text(
                    tr(
                        "Незавершённая тренировка не попадёт в результаты, но уже полученные ответы будут учтены в статистике.",
                        "The unfinished session won't be added to your results, but answers already given are saved in stats.",
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onExit()
                    },
                ) {
                    Text(tr("Выйти", "Leave"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(tr("Продолжить", "Continue"))
                }
            },
        )
    }
}

@Composable
private fun PracticeTopBar(
    title: String,
    counter: String,
    fraction: Float,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = tr("Выйти", "Exit"))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = counter,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 16.dp),
            )
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(7.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun QuestionBody(
    question: Question,
    rule: Rule?,
    language: AppLanguage,
    isLast: Boolean,
    modifier: Modifier,
    onAnswered: (Boolean, String) -> Unit,
    onNext: () -> Unit,
) {
    var answered by remember { mutableStateOf(false) }
    var correct by remember { mutableStateOf(false) }
    var userInput by remember { mutableStateOf("") }
    val sectionId = rule?.sectionId ?: "articles"

    val submit: (Boolean, String) -> Unit = { isCorrect, input ->
        if (!answered) {
            answered = true
            correct = isCorrect
            userInput = input
            onAnswered(isCorrect, input)
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LevelChip(level = question.level)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = when {
                question.isChoice && sectionId == "articles" ->
                    tr("Выберите правильный артикль", "Choose the correct article")
                question.isChoice ->
                    tr("Выберите правильный предлог", "Choose the correct preposition")
                question.isBuild ->
                    tr("Переведите предложение на английский", "Translate the sentence into English")
                else -> question.prompt?.text(language)
                    ?: tr("Сопоставьте правило и пример", "Match each rule with its example")
            },
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(12.dp))

        when {
            question.isChoice -> ChoiceBody(
                question = question,
                sectionId = sectionId,
                answered = answered,
                userInput = userInput,
                onSubmit = submit,
            )

            question.isBuild -> BuildBody(
                question = question,
                answered = answered,
                onSubmit = submit,
            )

            else -> MatchBody(
                question = question,
                language = language,
                answered = answered,
                onSubmit = submit,
            )
        }

        if (answered) {
            Spacer(Modifier.height(16.dp))
            FeedbackCard(
                question = question,
                sectionId = sectionId,
                correct = correct,
                userInput = userInput,
                language = language,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = if (isLast) {
                        tr("Показать результат", "Show result")
                    } else {
                        tr("Далее", "Next")
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

private enum class OptionState { Idle, Correct, Wrong }

@Composable
private fun ChoiceBody(
    question: Question,
    sectionId: String,
    answered: Boolean,
    userInput: String,
    onSubmit: (Boolean, String) -> Unit,
) {
    val canonical = AnswerChecker.canonicalAnswer(question)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SentenceCard(
            sentence = question.sentence.orEmpty(),
            filled = if (answered) canonical else null,
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            question.options.chunked(2).forEach { rowOptions ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowOptions.forEach { option ->
                        val selected = answered && AnswerChecker.normalize(option) == AnswerChecker.normalize(userInput)
                        val isRight = AnswerChecker.normalize(option) == canonical
                        AnswerOption(
                            label = itemLabel(option, sectionId),
                            state = when {
                                answered && isRight -> OptionState.Correct
                                answered && selected -> OptionState.Wrong
                                else -> OptionState.Idle
                            },
                            enabled = !answered,
                            onClick = { onSubmit(AnswerChecker.isCorrect(question, option), option) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowOptions.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AnswerOption(
    label: String,
    state: OptionState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = when (state) {
        OptionState.Correct -> MaterialTheme.colorScheme.primary
        OptionState.Wrong -> MaterialTheme.colorScheme.error
        OptionState.Idle -> MaterialTheme.colorScheme.outlineVariant
    }
    val containerColor = when (state) {
        OptionState.Correct -> MaterialTheme.colorScheme.primaryContainer
        OptionState.Wrong -> MaterialTheme.colorScheme.errorContainer
        OptionState.Idle -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when (state) {
        OptionState.Correct -> MaterialTheme.colorScheme.onPrimaryContainer
        OptionState.Wrong -> MaterialTheme.colorScheme.onErrorContainer
        OptionState.Idle -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(if (state == OptionState.Idle) 1.dp else 2.dp, borderColor),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private enum class MatchState { Idle, Selected, Matched, Flash }

@Composable
private fun MatchBody(
    question: Question,
    language: AppLanguage,
    answered: Boolean,
    onSubmit: (Boolean, String) -> Unit,
) {
    val pairs = question.pairs
    val leftOrder = remember(question.id) { pairs.indices.shuffled() }
    val rightOrder = remember(question.id) { pairs.indices.shuffled() }
    var selectedLeft by remember { mutableStateOf<Int?>(null) }
    var matched by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var wrongAttempts by remember { mutableIntStateOf(0) }
    var flashPair by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val scope = rememberCoroutineScope()

    val handleRight: (Int) -> Unit = { rightOriginal ->
        val left = selectedLeft
        if (left != null && !answered && rightOriginal !in matched) {
            if (left == rightOriginal) {
                val next = matched + rightOriginal
                matched = next
                selectedLeft = null
                if (next.size == pairs.size) {
                    onSubmit(wrongAttempts == 0, "")
                }
            } else {
                wrongAttempts += 1
                flashPair = left to rightOriginal
                selectedLeft = null
                scope.launch {
                    delay(450)
                    flashPair = null
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    tr("Правило", "Rule"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                leftOrder.forEach { pairIndex ->
                    MatchCard(
                        text = pairs[pairIndex].left.text(language),
                        state = when {
                            pairIndex in matched -> MatchState.Matched
                            flashPair?.first == pairIndex -> MatchState.Flash
                            selectedLeft == pairIndex -> MatchState.Selected
                            else -> MatchState.Idle
                        },
                        enabled = !answered && pairIndex !in matched,
                        onClick = {
                            if (!answered && pairIndex !in matched) {
                                selectedLeft = pairIndex
                            }
                        },
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    tr("Пример", "Example"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                rightOrder.forEach { pairIndex ->
                    MatchCard(
                        text = pairs[pairIndex].right,
                        state = when {
                            pairIndex in matched -> MatchState.Matched
                            flashPair?.second == pairIndex -> MatchState.Flash
                            else -> MatchState.Idle
                        },
                        enabled = !answered && pairIndex !in matched,
                        onClick = { handleRight(pairIndex) },
                    )
                }
            }
        }
        if (!answered) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tr(
                        "Нажмите правило слева, затем пример справа.",
                        "Tap a rule on the left, then its example on the right.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (wrongAttempts > 0) {
                    Text(
                        tr("Ошибок: $wrongAttempts", "Mistakes: $wrongAttempts"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchCard(
    text: String,
    state: MatchState,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = when (state) {
        MatchState.Selected, MatchState.Matched -> MaterialTheme.colorScheme.primary
        MatchState.Flash -> MaterialTheme.colorScheme.error
        MatchState.Idle -> MaterialTheme.colorScheme.outlineVariant
    }
    val containerColor = when (state) {
        MatchState.Selected -> MaterialTheme.colorScheme.secondaryContainer
        MatchState.Matched -> MaterialTheme.colorScheme.primaryContainer
        MatchState.Flash -> MaterialTheme.colorScheme.errorContainer
        MatchState.Idle -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when (state) {
        MatchState.Selected -> MaterialTheme.colorScheme.onSecondaryContainer
        MatchState.Matched -> MaterialTheme.colorScheme.onPrimaryContainer
        MatchState.Flash -> MaterialTheme.colorScheme.onErrorContainer
        MatchState.Idle -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(if (state == MatchState.Idle) 1.dp else 2.dp, borderColor),
    ) {
        Text(
            text = if (state == MatchState.Matched) "✓ $text" else text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(10.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BuildBody(
    question: Question,
    answered: Boolean,
    onSubmit: (Boolean, String) -> Unit,
) {
    val words = question.words
    val shuffledIndices = remember(question.id) { words.indices.shuffled() }
    var selected by remember(question.id) { mutableStateOf<List<Int>>(emptyList()) }

    val bankIndices = shuffledIndices.filterNot { it in selected }
    val allPlaced = words.isNotEmpty() && selected.size == words.size

    val toggle: (Int) -> Unit = { index ->
        if (!answered) {
            selected = if (index in selected) selected - index else selected + index
        }
    }

    val russianSentence = question.translation?.ru.orEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (russianSentence.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Text(
                    text = russianSentence,
                    style = MaterialTheme.typography.titleLarge.copy(lineHeight = 28.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                selected.forEach { wordIndex ->
                    WordTile(
                        word = words[wordIndex],
                        filled = true,
                        enabled = !answered,
                        onClick = { toggle(wordIndex) },
                    )
                }
            }
        }

        Text(
            text = tr("Слова", "Words"),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            bankIndices.forEach { wordIndex ->
                WordTile(
                    word = words[wordIndex],
                    filled = false,
                    enabled = !answered,
                    onClick = { toggle(wordIndex) },
                )
            }
        }

        if (!answered) {
            Button(
                onClick = {
                    val tokens = selected.map { words[it] }
                    onSubmit(
                        AnswerChecker.isBuildCorrect(question, tokens),
                        tokens.joinToString(" "),
                    )
                },
                enabled = allPlaced,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(tr("Проверить", "Check"), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun WordTile(
    word: String,
    filled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val container = if (filled) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (filled) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = container,
        contentColor = content,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = word,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SentenceCard(sentence: String, filled: String?) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        SentenceText(
            sentence = sentence,
            filled = filled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
        )
    }
}

@Composable
private fun FeedbackCard(
    question: Question,
    sectionId: String,
    correct: Boolean,
    userInput: String,
    language: AppLanguage,
) {
    val container = if (correct) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val content = if (correct) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (correct) "✓" else "✕",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (correct) tr("Верно!", "Correct!") else tr("Неверно", "Not quite"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (question.isBuild) {
                Text(
                    text = tr("Правильное предложение", "Correct sentence"),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = content.copy(alpha = 0.7f),
                )
                Text(
                    text = AnswerChecker.correctText(question),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (!correct && userInput.isNotBlank()) {
                    Text(
                        text = tr("Ваш ответ: $userInput", "Your answer: $userInput"),
                        style = MaterialTheme.typography.labelLarge,
                        color = content.copy(alpha = 0.9f),
                    )
                }
            } else if (!correct) {
                Text(
                    text = if (question.isMatch) {
                        tr(
                            "Не все пары были соединены правильно.",
                            "Some pairs were matched incorrectly.",
                        )
                    } else {
                        tr(
                            "Правильный ответ: ${itemLabel(AnswerChecker.canonicalAnswer(question), sectionId)}",
                            "Correct answer: ${itemLabel(AnswerChecker.canonicalAnswer(question), sectionId)}",
                        )
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = content.copy(alpha = 0.9f),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = tr("Почему", "Why"),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = content.copy(alpha = 0.7f),
                )
                Text(
                    text = question.explanation.text(language),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        lineBreak = LineBreak.Heading,
                    ),
                )
            }
            val translation = question.translation?.ru.orEmpty()
            if (!question.isBuild && language == AppLanguage.RU && translation.isNotBlank()) {
                HorizontalDivider(color = content.copy(alpha = 0.2f))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = tr("Перевод", "Translation"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = content.copy(alpha = 0.7f),
                    )
                    Text(
                        text = translation,
                        style = MaterialTheme.typography.bodySmall.copy(lineBreak = LineBreak.Heading),
                    )
                }
            }
        }
    }
}
