package com.iltatrainer.app.domain

import com.iltatrainer.app.data.DayStat
import com.iltatrainer.app.data.ProgressState
import com.iltatrainer.app.data.Question
import com.iltatrainer.app.data.Rule
import java.time.LocalDate
import kotlin.math.roundToInt

data class LevelStat(
    val attempts: Int = 0,
    val correct: Int = 0,
) {
    val accuracy: Float get() = StatsCalculator.accuracy(correct, attempts)
}

/** Pure statistics helpers shared by the Home and Stats screens. */
object StatsCalculator {

    fun accuracy(correct: Int, attempts: Int): Float =
        if (attempts <= 0) 0f else (correct.toFloat() / attempts).coerceIn(0f, 1f)

    fun overallAccuracy(state: ProgressState): Float =
        accuracy(state.totalCorrect, state.totalAnswered)

    /**
     * Progress of one theme (0..100).
     *
     * Every correct answer adds `100 / N` points (so repeated correct answers raise the progress
     * too, and replaying a theme raises it), every wrong attempt costs twice as much
     * (`2 * 100 / N`). The result is clamped to 0..100.
     */
    fun ruleProgress(state: ProgressState, ruleQuestions: List<Question>): Int {
        if (ruleQuestions.isEmpty()) return 0
        val step = 100.0 / ruleQuestions.size
        var score = 0.0
        ruleQuestions.forEach { question ->
            val stat = state.questionStats[question.id] ?: return@forEach
            val wrongAttempts = (stat.attempts - stat.correct).coerceAtLeast(0)
            score += stat.correct * step
            score -= wrongAttempts * step * 2.0
        }
        return score.roundToInt().coerceIn(0, 100)
    }

    /** Average progress of all themes in a section (0..100). */
    fun sectionProgress(
        state: ProgressState,
        sectionRules: List<Rule>,
        questions: List<Question>,
    ): Int {
        if (sectionRules.isEmpty()) return 0
        val byRule = questions.groupBy { it.ruleId }
        val sum = sectionRules.sumOf { rule ->
            ruleProgress(state, byRule[rule.id].orEmpty()).toDouble()
        }
        return (sum / sectionRules.size).roundToInt().coerceIn(0, 100)
    }

    /** Average progress across all themes — the “you have learned X% of the grammar” metric. */
    fun learningProgress(
        state: ProgressState,
        rules: List<Rule>,
        questions: List<Question>,
    ): Int {
        if (rules.isEmpty()) return 0
        val byRule = questions.groupBy { it.ruleId }
        val sum = rules.sumOf { rule ->
            ruleProgress(state, byRule[rule.id].orEmpty()).toDouble()
        }
        return (sum / rules.size).roundToInt().coerceIn(0, 100)
    }

    fun todayKey(today: LocalDate = LocalDate.now()): String = today.toString()

    fun answeredOn(state: ProgressState, date: LocalDate): Int =
        state.days[date.toString()]?.answered ?: 0

    fun correctOn(state: ProgressState, date: LocalDate): Int =
        state.days[date.toString()]?.correct ?: 0

    /** Consecutive days with at least one answer, ending today or yesterday. */
    fun streak(state: ProgressState, today: LocalDate = LocalDate.now()): Int {
        var day = today
        if (answeredOn(state, day) == 0) {
            day = day.minusDays(1)
        }
        var count = 0
        while (answeredOn(state, day) > 0) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    fun bestStreak(state: ProgressState, today: LocalDate = LocalDate.now()): Int =
        maxOf(state.bestStreak, streak(state, today))

    fun dailyGoalMet(state: ProgressState, today: LocalDate = LocalDate.now()): Boolean {
        val day = state.days[today.toString()] ?: DayStat()
        return day.answered >= state.settings.dailyGoal.coerceAtLeast(1)
    }

    fun todayProgress(state: ProgressState, today: LocalDate = LocalDate.now()): Int =
        answeredOn(state, today)

    /** Question is considered mastered after 3+ attempts with at least 80% accuracy. */
    fun masteredCount(state: ProgressState): Int = state.questionStats.values.count { stat ->
        stat.attempts >= 3 && accuracy(stat.correct, stat.attempts) >= 0.8f
    }

    fun questionAccuracy(state: ProgressState, questionId: String): Float? {
        val stat = state.questionStats[questionId] ?: return null
        return if (stat.attempts <= 0) null else accuracy(stat.correct, stat.attempts)
    }

    fun levelStats(state: ProgressState, questions: List<Question>): Map<String, LevelStat> {
        val levelByQuestion = questions.associate { it.id to it.level }
        val result = linkedMapOf<String, LevelStat>()
        state.questionStats.forEach { (questionId, stat) ->
            val level = levelByQuestion[questionId] ?: return@forEach
            val current = result[level] ?: LevelStat()
            result[level] = LevelStat(
                attempts = current.attempts + stat.attempts,
                correct = current.correct + stat.correct,
            )
        }
        return result.toSortedMap()
    }

    fun ruleAccuracy(state: ProgressState, ruleId: String): Float? {
        val stat = state.ruleStats[ruleId] ?: return null
        return if (stat.attempts <= 0) null else accuracy(stat.correct, stat.attempts)
    }

    /** Open mistake questions, oldest first (for the stats screen). */
    fun mistakeQuestions(state: ProgressState, questions: List<Question>): List<Question> {
        val byId = questions.associateBy { it.id }
        return SessionBuilder.openMistakes(state).mapNotNull { (questionId, _) -> byId[questionId] }
    }

    fun hardsetQuestions(state: ProgressState, questions: List<Question>, limit: Int = 5): List<Question> =
        mistakeQuestions(state, questions).take(limit)

    fun favoritesQuestions(state: ProgressState, questions: List<Question>): List<Question> =
        questions.filter { it.id in state.favorites }
}
