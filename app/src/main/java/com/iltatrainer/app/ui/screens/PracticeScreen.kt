package com.iltatrainer.app.ui.screens

import android.os.SystemClock

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
    onAnswer: (Question, Boolean, Long) -> Unit,
    onExit: () -> Unit,
    onFinish: (SessionResult) -> Unit,
) {
    val items = remember(config) {
        SessionBuilder.build(config, allQuestions, progress, Random.Default)
    }

    if (items.isEmpty()) {
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

    val currentItem = items[index]
    val current = currentItem.question
    val questionStartedAt = remember(current.id) { SystemClock.elapsedRealtime() }
    val isLast = index == items.lastIndex

    BackHandler { showExitDialog = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        PracticeTopBar(
            title = title,
            counter = "${index + 1}/${items.size}",
            fraction = (index + 1).toFloat() / items.size,
            onClose = { showExitDialog = true },
        )
        key(current.id) {
            QuestionBody(
                question = current,
                rule = rulesById[current.ruleId],
                language = language,
                typed = currentItem.typed,
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
                        onAnswer(
                            current,
                            correct,
                            (SystemClock.elapsedRealtime() - questionStartedAt).coerceAtLeast(0L),
                        )
                    }
                },
                onNext = {
                    if (isLast) {
                        onFinish(
                            SessionResult(
                                total = items.size,
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
    typed: Boolean,
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
        Text(
            text = when {
                question.isChoice && typed && sectionId == "articles" ->
                    tr("Введите правильный артикль", "Type the correct article")
                question.isChoice && typed && sectionId == "prepositions" ->
                    tr("Введите правильный предлог", "Type the correct preposition")
                question.isChoice && typed ->
                    tr("Введите пропущенное слово", "Type the missing word")
                question.isChoice && sectionId == "articles" ->
                    tr("Выберите правильный артикль", "Choose the correct article")
                question.isChoice && sectionId == "prepositions" ->
                    tr("Выберите правильный предлог", "Choose the correct preposition")
                question.isChoice ->
                    tr("Выберите правильный вариант", "Choose the correct option")
                else ->
                    tr("Переведите предложение на английский", "Translate the sentence into English")
            },
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(12.dp))

        when {
            question.isChoice && typed -> WritingBody(
                question = question,
                sectionId = sectionId,
                answered = answered,
                userInput = userInput,
                onSubmit = submit,
            )

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

            else -> Unit
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

@Composable
private fun WritingBody(
    question: Question,
    sectionId: String,
    answered: Boolean,
    userInput: String,
    onSubmit: (Boolean, String) -> Unit,
) {
    var input by remember(question.id) { mutableStateOf("") }
    val canonical = AnswerChecker.canonicalAnswer(question)
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val isWrong = answered && AnswerChecker.normalize(userInput) != canonical

    LaunchedEffect(question.id) { focusRequester.requestFocus() }

    val submitAnswer: () -> Unit = {
        if (!answered && input.isNotBlank()) {
            keyboard?.hide()
            onSubmit(AnswerChecker.isCorrect(question, input), input.trim())
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SentenceCard(
            sentence = question.sentence.orEmpty(),
            filled = if (answered) canonical else null,
        )
        OutlinedTextField(
            value = input,
            onValueChange = { if (!answered) input = it },
            enabled = !answered,
            singleLine = true,
            isError = isWrong,
            label = {
                Text(tr("Пропущенное слово", "Missing word"))
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { submitAnswer() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )
        if (answered && isWrong) {
            Text(
                text = tr(
                    "Вы ввели: $userInput",
                    "You typed: $userInput",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (!answered) {
            Button(
                onClick = submitAnswer,
                enabled = input.isNotBlank(),
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
                    text = tr(
                        "Правильный ответ: ${itemLabel(AnswerChecker.canonicalAnswer(question), sectionId)}",
                        "Correct answer: ${itemLabel(AnswerChecker.canonicalAnswer(question), sectionId)}",
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = content.copy(alpha = 0.9f),
                )
            }
            Text(
                text = question.explanation.text(language),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    lineBreak = LineBreak.Heading,
                ),
            )
            val translation = question.translation?.ru.orEmpty()
            if (!question.isBuild && language == AppLanguage.RU && translation.isNotBlank()) {
                HorizontalDivider(color = content.copy(alpha = 0.2f))
                Text(
                    text = translation,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        lineBreak = LineBreak.Heading,
                    ),
                )
            }
        }
    }
}
