package com.iltatrainer.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.iltatrainer.app.core.AppLanguage
import com.iltatrainer.app.data.AppContainer
import com.iltatrainer.app.data.ContentLoadResult
import com.iltatrainer.app.domain.PracticeMode
import com.iltatrainer.app.data.UpdateManager
import com.iltatrainer.app.data.UpdateState
import com.iltatrainer.app.domain.SessionConfig
import com.iltatrainer.app.domain.TOPIC_SESSION_SIZE
import com.iltatrainer.app.ui.components.EmptyState
import com.iltatrainer.app.ui.components.StatsBarIcon
import com.iltatrainer.app.ui.screens.PracticeScreen
import com.iltatrainer.app.ui.screens.ResultScreen
import com.iltatrainer.app.ui.screens.RuleDetailScreen
import com.iltatrainer.app.ui.screens.SectionScreen
import com.iltatrainer.app.ui.screens.SettingsScreen
import com.iltatrainer.app.ui.screens.StatsScreen
import com.iltatrainer.app.ui.screens.TopicsScreen
import com.iltatrainer.app.ui.theme.IlTaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

@Composable
fun IlTaApp(container: AppContainer) {
    val progress by container.progressStore.state.collectAsState()
    val contentState by produceState<ContentLoadResult?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { container.contentRepository.load() }
    }

    CompositionLocalProvider(LocalLanguage provides progress.settings.appLanguage) {
        IlTaTheme(
            themeMode = progress.settings.themeMode,
            dynamicColor = progress.settings.dynamicColor,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                val content = contentState
                when {
                    content == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    content.sections.isEmpty() || content.rules.isEmpty() || content.questions.isEmpty() ->
                        EmptyState(
                            emoji = "📦",
                            title = tr("Контент не загрузился", "Content failed to load"),
                            message = (content.errors.firstOrNull() ?: "assets/data is empty")
                                .ifBlank { "assets/data is empty" },
                            modifier = Modifier.padding(24.dp),
                        )
                    else -> AppContent(container = container, content = content)
                }
            }
        }
    }
}

@Composable
private fun AppContent(container: AppContainer, content: ContentLoadResult) {
    val progress by container.progressStore.state.collectAsState()
    LaunchedEffect(content) {
        container.progressStore.contentInfo = com.iltatrainer.app.data.ContentInfo(
            sections = content.sections.size,
            rules = content.rules.size,
            questions = content.questions.size,
        )
    }
    val language = progress.settings.appLanguage
    val navigator = remember { AppNavigator() }
    val screen = navigator.current
    val context = LocalContext.current
    val updateManager = remember { UpdateManager(context) }
    val updateState by updateManager.state.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        updateManager.check(force = false)
    }

    val isTab = screen is Screen.Topics || screen is Screen.Stats || screen is Screen.Settings

    val startPractice: (SessionConfig, String) -> Unit = { config, title ->
        navigator.push(Screen.Practice(config, title))
    }

    val quickTitle = if (language == AppLanguage.RU) "Быстрая тренировка" else "Quick practice"
    val mistakesTitle = if (language == AppLanguage.RU) "Работа над ошибками" else "Mistake practice"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (isTab) {
                AppBottomBar(
                    current = screen,
                    onSelect = { target -> navigator.resetTo(target) },
                    hasUpdate = updateState is UpdateState.Available,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (screen) {
                Screen.Topics -> TopicsScreen(
                    sections = content.sections,
                    rules = content.rules,
                    questions = content.questions,
                    progress = progress,
                    contentErrors = content.errors,
                    onStartQuickPractice = { ruleIds, types ->
                        startPractice(
                            SessionConfig(
                                mode = PracticeMode.QUICK,
                                size = progress.settings.sessionSize,
                                ruleIds = ruleIds,
                                types = types,
                            ),
                            quickTitle,
                        )
                    },
                    onStartMistakes = {
                        startPractice(
                            SessionConfig(PracticeMode.MISTAKES, size = progress.settings.sessionSize),
                            mistakesTitle,
                        )
                    },
                    onOpenSection = { sectionId -> navigator.push(Screen.Section(sectionId)) },
                    modifier = Modifier.fillMaxSize(),
                )

                is Screen.Section -> {
                    val sectionId = screen.sectionId
                    val section = content.sectionsById[sectionId]
                    val sectionRules = content.rulesBySection[sectionId].orEmpty()
                    val sectionTitle = section?.title?.text(language).orEmpty()
                    val practiceTitle = if (language == AppLanguage.RU) {
                        "Тренировка: $sectionTitle"
                    } else {
                        "Practice: $sectionTitle"
                    }
                    SectionScreen(
                        section = section,
                        rules = sectionRules,
                        questions = content.questions,
                        questionsByRule = content.questionsByRule,
                        progress = progress,
                        onBack = { navigator.pop() },
                        onOpenRule = { ruleId -> navigator.push(Screen.RuleDetail(ruleId)) },
                        onPracticeSection = {
                            if (sectionId.isNotBlank()) {
                                val ids = content.questionsForSection(sectionId)
                                    .map { it.id }
                                    .shuffled(Random.Default)
                                    .take(progress.settings.sessionSize)
                                if (ids.isNotEmpty()) {
                                    startPractice(
                                        SessionConfig(
                                            mode = PracticeMode.RETRY,
                                            size = ids.size,
                                            questionIds = ids,
                                        ),
                                        practiceTitle,
                                    )
                                }
                            }
                        },
                    )
                }

                is Screen.RuleDetail -> {
                    val ruleId = screen.ruleId
                    val ruleTitle = content.rulesById[ruleId]?.title?.text(language).orEmpty()
                    RuleDetailScreen(
                        rule = content.rulesById[ruleId],
                        questions = content.questions.filter { it.ruleId == ruleId },
                        progress = progress,
                        onBack = { navigator.pop() },
                        onTrain = {
                            startPractice(
                                SessionConfig(
                                    mode = PracticeMode.TOPIC,
                                    ruleId = ruleId,
                                    size = TOPIC_SESSION_SIZE,
                                ),
                                ruleTitle,
                            )
                        },
                    )
                }

                Screen.Stats -> StatsScreen(
                    rules = content.rules,
                    questions = content.questions,
                    progress = progress,
                    onStartMistakes = {
                        startPractice(
                            SessionConfig(PracticeMode.MISTAKES, size = progress.settings.sessionSize),
                            mistakesTitle,
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                Screen.Settings -> SettingsScreen(
                    progress = progress,
                    store = container.progressStore,
                    questionCount = content.questions.size,
                    ruleCount = content.rules.size,
                    updateState = updateState,
                    onCheckUpdates = {
                        scope.launch { updateManager.check(force = true) }
                    },
                    onDownloadUpdate = {
                        val info = (updateState as? UpdateState.Available)?.info
                        if (info != null) {
                            scope.launch { updateManager.downloadAndInstall(info) }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                is Screen.Practice -> PracticeScreen(
                    config = screen.config,
                    title = screen.title,
                    allQuestions = content.questions,
                    rulesById = content.rulesById,
                    progress = progress,
                    language = language,
                    onAnswer = { question, correct ->
                        container.progressStore.recordAnswer(question, correct)
                    },
                    onExit = {
                        if (!navigator.pop()) navigator.resetTo(Screen.Topics)
                    },
                    onFinish = { result ->
                        navigator.replace(Screen.Result(result, screen.title))
                    },
                )

                is Screen.Result -> ResultScreen(
                    result = screen.result,
                    rulesById = content.rulesById,
                    language = language,
                    onReviewMistakes = {
                        startPractice(
                            SessionConfig(PracticeMode.MISTAKES, size = progress.settings.sessionSize),
                            mistakesTitle,
                        )
                    },
                    onDone = {
                        if (!navigator.pop()) navigator.resetTo(Screen.Topics)
                    },
                )
            }
        }
    }

    BackHandler(enabled = navigator.canGoBack && screen !is Screen.Practice) {
        navigator.pop()
    }
}

@Composable
private fun AppBottomBar(
    current: Screen,
    onSelect: (Screen) -> Unit,
    hasUpdate: Boolean,
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            selected = current is Screen.Topics || current is Screen.Section || current is Screen.RuleDetail,
            onClick = { onSelect(Screen.Topics) },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            label = { Text(tr("Темы", "Topics")) },
        )
        NavigationBarItem(
            selected = current is Screen.Stats,
            onClick = { onSelect(Screen.Stats) },
            icon = { StatsBarIcon() },
            label = { Text(tr("Статистика", "Stats")) },
        )
        NavigationBarItem(
            selected = current is Screen.Settings,
            onClick = { onSelect(Screen.Settings) },
            icon = {
                Box {
                    Icon(Icons.Filled.Settings, contentDescription = null)
                    if (hasUpdate) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 3.dp, y = (-3).dp)
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error),
                        )
                    }
                }
            },
            label = { Text(tr("Настройки", "Settings")) },
        )
    }
}
