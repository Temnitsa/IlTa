package com.iltatrainer.app.data

import android.content.Context
import android.net.Uri
import com.iltatrainer.app.core.AppLanguage
import com.iltatrainer.app.core.ThemeMode
import com.iltatrainer.app.domain.StatsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

@Serializable
data class QuestionStat(
    val attempts: Int = 0,
    val correct: Int = 0,
    val lastSeen: Long = 0L,
    /** Valid (non-outlier) timed attempts and their total duration. */
    val timedAttempts: Int = 0,
    val totalResponseMs: Long = 0L,
)

@Serializable
data class RuleStat(
    val attempts: Int = 0,
    val correct: Int = 0,
    val lastSeen: Long = 0L,
)

@Serializable
data class MistakeEntry(
    val ruleId: String = "",
    val originalCorrect: Boolean = false,
    val similarCorrect: Int = 0,
    val openedAt: Long = 0L,
)

@Serializable
data class DayStat(
    val answered: Int = 0,
    val correct: Int = 0,
)

@Serializable
data class Settings(
    val language: String = AppLanguage.RU.code,
    val theme: String = ThemeMode.SYSTEM.code,
    val dynamicColor: Boolean = true,
    val dailyGoal: Int = 20,
    val sessionSize: Int = 10,
) {
    val appLanguage: AppLanguage get() = AppLanguage.fromCode(language)
    val themeMode: ThemeMode get() = ThemeMode.fromCode(theme)
}

@Serializable
data class ProgressState(
    val version: Int = CURRENT_VERSION,
    val totalAnswered: Int = 0,
    val totalCorrect: Int = 0,
    val questionStats: Map<String, QuestionStat> = emptyMap(),
    val ruleStats: Map<String, RuleStat> = emptyMap(),
    val days: Map<String, DayStat> = emptyMap(),
    /** Kept for backward compatibility with old exported configs; the UI no longer uses favourites. */
    val favorites: Set<String> = emptySet(),
    /** Open mistakes: question id -> closure progress (original correction + 3 similar questions). */
    val mistakes: Map<String, MistakeEntry> = emptyMap(),
    val closedMistakes: Int = 0,
    val bestStreak: Int = 0,
    val settings: Settings = Settings(),
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

/**
 * Single source of truth for the learner's progress. Persisted as JSON in SharedPreferences,
 * so everything works fully offline; export/import goes through Android's Storage Access Framework.
 */
class ProgressStore(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val _state = MutableStateFlow(load())
    val state: StateFlow<ProgressState> = _state.asStateFlow()

    /** Set by the app once content is loaded; stored inside exported configs. */
    var contentInfo: ContentInfo = ContentInfo()

    /** Called after every checked answer. */
    fun recordAnswer(
        question: Question,
        correct: Boolean,
        now: Long = System.currentTimeMillis(),
        today: LocalDate = LocalDate.now(),
        responseTimeMs: Long? = null,
    ) {
        synchronized(this) {
            val s = _state.value
            val questionStat = s.questionStats[question.id] ?: QuestionStat()
            val validResponseMs = responseTimeMs?.takeIf { it in 1..MAX_RESPONSE_MS }
            val ruleStat = s.ruleStats[question.ruleId] ?: RuleStat()
            val dayKey = today.toString()
            val dayStat = s.days[dayKey] ?: DayStat()
            val (mistakes, closedMistakes) = updateMistakes(s.mistakes, s.closedMistakes, question, correct, now)
            var updated = s.copy(
                totalAnswered = s.totalAnswered + 1,
                totalCorrect = s.totalCorrect + if (correct) 1 else 0,
                questionStats = s.questionStats + (question.id to questionStat.copy(
                    attempts = questionStat.attempts + 1,
                    correct = questionStat.correct + if (correct) 1 else 0,
                    lastSeen = now,
                    timedAttempts = questionStat.timedAttempts + if (validResponseMs != null) 1 else 0,
                    totalResponseMs = questionStat.totalResponseMs + (validResponseMs ?: 0L),
                )),
                ruleStats = s.ruleStats + (question.ruleId to ruleStat.copy(
                    attempts = ruleStat.attempts + 1,
                    correct = ruleStat.correct + if (correct) 1 else 0,
                    lastSeen = now,
                )),
                days = s.days + (dayKey to dayStat.copy(
                    answered = dayStat.answered + 1,
                    correct = dayStat.correct + if (correct) 1 else 0,
                )),
                mistakes = mistakes,
                closedMistakes = closedMistakes,
            )
            val streak = StatsCalculator.streak(updated, today)
            if (streak > updated.bestStreak) {
                updated = updated.copy(bestStreak = streak)
            }
            save(updated)
        }
    }

    /**
     * Mistake closure rules: the original question must be answered correctly,
     * and 3 different similar questions of the same theme must be answered correctly.
     */
    private fun updateMistakes(
        current: Map<String, MistakeEntry>,
        closedCount: Int,
        question: Question,
        correct: Boolean,
        now: Long,
    ): Pair<Map<String, MistakeEntry>, Int> {
        var mistakes = current
        var closed = closedCount
        val entry = mistakes[question.id]
        if (!correct) {
            mistakes = if (entry == null) {
                mistakes + (question.id to MistakeEntry(ruleId = question.ruleId, openedAt = now))
            } else {
                mistakes + (question.id to entry.copy(originalCorrect = false))
            }
        } else {
            if (entry != null) {
                mistakes = mistakes + (question.id to entry.copy(originalCorrect = true))
            }
            mistakes = mistakes.mapValues { (id, mistake) ->
                if (id != question.id && mistake.ruleId == question.ruleId) {
                    mistake.copy(similarCorrect = (mistake.similarCorrect + 1).coerceAtMost(3))
                } else {
                    mistake
                }
            }
            val toClose = mistakes.filterValues { it.originalCorrect && it.similarCorrect >= 3 }.keys
            if (toClose.isNotEmpty()) {
                mistakes = mistakes - toClose
                closed += toClose.size
            }
        }
        return mistakes to closed
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        synchronized(this) {
            val s = _state.value
            save(s.copy(settings = transform(s.settings)))
        }
    }

    fun resetAll() {
        synchronized(this) {
            val settings = _state.value.settings
            save(ProgressState(settings = settings))
        }
    }

    fun exportTo(uri: Uri): Result<Unit> = runCatching {
        val text = ProgressConfigCodec.encode(
            state = _state.value,
            appVersion = EXPORT_APP_VERSION,
            content = contentInfo,
            exportedAt = Instant.now().toString(),
        )
        val stream = context.contentResolver.openOutputStream(uri)
            ?: error("Cannot open the selected file for writing")
        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    fun importFrom(uri: Uri): Result<ImportedProgress> = runCatching {
        val text = context.contentResolver.openInputStream(uri)?.use {
            it.bufferedReader(Charsets.UTF_8).readText()
        } ?: error("Cannot read the selected file")

        val imported = ProgressConfigCodec.decode(text)
        val sanitized = sanitize(imported.state)
        synchronized(this) { save(sanitized) }
        imported.copy(state = sanitized)
    }

    private fun sanitize(state: ProgressState): ProgressState {
        val settings = state.settings
        return state.copy(
            version = ProgressState.CURRENT_VERSION,
            totalAnswered = state.totalAnswered.coerceAtLeast(0),
            totalCorrect = state.totalCorrect.coerceIn(0, state.totalAnswered),
            bestStreak = state.bestStreak.coerceAtLeast(0),
            closedMistakes = state.closedMistakes.coerceAtLeast(0),
            mistakes = state.mistakes
                .filterValues { it.ruleId.isNotBlank() }
                .mapValues { (_, mistake) -> mistake.copy(similarCorrect = mistake.similarCorrect.coerceIn(0, 3)) },
            settings = settings.copy(
                language = AppLanguage.fromCode(settings.language).code,
                theme = ThemeMode.fromCode(settings.theme).code,
                dailyGoal = settings.dailyGoal.coerceIn(5, 200),
                sessionSize = settings.sessionSize.coerceIn(5, 50),
            ),
        )
    }

    private fun save(newState: ProgressState) {
        _state.value = newState
        prefs.edit()
            .putString(KEY_PROGRESS, json.encodeToString(ProgressState.serializer(), newState))
            .apply()
    }

    private fun load(): ProgressState {
        val raw = prefs.getString(KEY_PROGRESS, null) ?: return ProgressState()
        return runCatching {
            sanitize(json.decodeFromString(ProgressState.serializer(), raw))
        }.getOrElse { ProgressState() }
    }

    companion object {
        const val EXPORT_APP_VERSION = "2.3.0"
        /** Answers slower than this are treated as "left the screen" outliers for speed stats. */
        const val MAX_RESPONSE_MS = 120_000L
        private const val PREFS = "ilta_progress"
        private const val KEY_PROGRESS = "progress_state_v1"
    }
}
