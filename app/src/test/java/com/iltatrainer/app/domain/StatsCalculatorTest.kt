package com.iltatrainer.app.domain

import com.iltatrainer.app.data.Bilingual
import com.iltatrainer.app.data.DayStat
import com.iltatrainer.app.data.ProgressState
import com.iltatrainer.app.data.Question
import com.iltatrainer.app.data.QuestionStat
import com.iltatrainer.app.data.RuleStat
import com.iltatrainer.app.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StatsCalculatorTest {

    private fun question(id: String, level: String) = Question(
        id = id,
        type = "choice",
        ruleId = "rule",
        level = level,
        sentence = "Sentence ___ for $id.",
        options = listOf("a", "an", "the", "—"),
        answer = "a",
        explanation = Bilingual(ru = "тест", en = "test"),
    )

    @Test
    fun `accuracy is zero for an empty history and clamps to one`() {
        assertEquals(0f, StatsCalculator.accuracy(0, 0), 0.0001f)
        assertEquals(0.5f, StatsCalculator.accuracy(1, 2), 0.0001f)
        assertEquals(1f, StatsCalculator.accuracy(7, 5), 0.0001f)
    }

    @Test
    fun `streak counts consecutive days ending today`() {
        val today = LocalDate.of(2026, 10, 31)
        val state = ProgressState(
            days = mapOf(
                "2026-10-31" to DayStat(answered = 3),
                "2026-10-30" to DayStat(answered = 1),
                "2026-10-28" to DayStat(answered = 5), // gap on the 29th
            ),
        )
        assertEquals(2, StatsCalculator.streak(state, today))
        assertEquals(3, StatsCalculator.answeredOn(state, today))
    }

    @Test
    fun `streak survives until yesterday was active`() {
        val today = LocalDate.of(2026, 10, 31)
        val state = ProgressState(
            days = mapOf(
                "2026-10-30" to DayStat(answered = 2),
                "2026-10-29" to DayStat(answered = 2),
            ),
        )
        assertEquals(2, StatsCalculator.streak(state, today))
    }

    @Test
    fun `streak is zero with no history`() {
        assertEquals(0, StatsCalculator.streak(ProgressState(), LocalDate.of(2026, 10, 31)))
    }

    @Test
    fun `daily goal uses settings`() {
        val today = LocalDate.of(2026, 10, 31)
        val state = ProgressState(
            days = mapOf("2026-10-31" to DayStat(answered = 12)),
            settings = Settings(dailyGoal = 10),
        )
        assertTrue(StatsCalculator.dailyGoalMet(state, today))
        assertEquals(12, StatsCalculator.todayProgress(state, today))
    }


    @Test
    fun `idle theme loses three percentage points per day`() {
        val lastDay = LocalDate.of(2026, 10, 30)
        val lastSeen = lastDay.atTime(12, 0).atZone(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val q = question("idle", "A1")
        val state = ProgressState(
            questionStats = mapOf("idle" to QuestionStat(attempts = 1, correct = 1, lastSeen = lastSeen)),
            ruleStats = mapOf("rule" to RuleStat(attempts = 1, correct = 1, lastSeen = lastSeen)),
        )
        assertEquals(100, StatsCalculator.ruleProgress(state, listOf(q), sessionSize = 1, today = lastDay))
        assertEquals(94, StatsCalculator.ruleProgress(state, listOf(q), sessionSize = 1, today = lastDay.plusDays(2)))
    }

    @Test
    fun `speed score ignores untouched questions and maps time to a fraction`() {
        val state = ProgressState(
            questionStats = mapOf(
                "timed" to QuestionStat(timedAttempts = 1, totalResponseMs = 6_000),
                "untimed" to QuestionStat(attempts = 2, correct = 1),
            ),
        )
        assertEquals(0.5f, StatsCalculator.speedScore(state), 0.0001f)
    }

    @Test
    fun `mastered questions need three attempts and eighty percent accuracy`() {
        val state = ProgressState(
            questionStats = mapOf(
                "a" to QuestionStat(attempts = 4, correct = 4),
                "b" to QuestionStat(attempts = 3, correct = 3),
                "c" to QuestionStat(attempts = 2, correct = 2),
                "d" to QuestionStat(attempts = 4, correct = 2),
                "e" to QuestionStat(attempts = 5, correct = 3),
            ),
        )
        assertEquals(2, StatsCalculator.masteredCount(state))
    }

    @Test
    fun `level stats aggregate question stats by level`() {
        val questions = listOf(
            question("q1", "A1"),
            question("q2", "A1"),
            question("q3", "B2"),
        )
        val state = ProgressState(
            questionStats = mapOf(
                "q1" to QuestionStat(attempts = 2, correct = 2),
                "q2" to QuestionStat(attempts = 2, correct = 0),
                "q3" to QuestionStat(attempts = 4, correct = 3),
            ),
        )
        val stats = StatsCalculator.levelStats(state, questions)
        assertEquals(4, stats.getValue("A1").attempts)
        assertEquals(2, stats.getValue("A1").correct)
        assertEquals(0.5f, stats.getValue("A1").accuracy, 0.0001f)
        assertEquals(0.75f, stats.getValue("B2").accuracy, 0.0001f)
    }

    @Test
    fun `question and rule accuracy are null for unseen items`() {
        val state = ProgressState(
            questionStats = mapOf("q1" to QuestionStat(attempts = 2, correct = 1)),
            ruleStats = mapOf("rule" to RuleStat(attempts = 2, correct = 1)),
        )
        assertEquals(0.5f, StatsCalculator.questionAccuracy(state, "q1")!!, 0.0001f)
        assertNull(StatsCalculator.questionAccuracy(state, "q2"))
        assertEquals(0.5f, StatsCalculator.ruleAccuracy(state, "rule")!!, 0.0001f)
        assertNull(StatsCalculator.ruleAccuracy(state, "unknown-rule"))
    }

    private fun nineteenCorrect() = ProgressState(
        questionStats = (1..19).associate { index ->
            "q$index" to QuestionStat(attempts = 1, correct = 1)
        },
    )

    @Test
    fun `theme progress gives five percent per correct of twenty and fines mistakes twice`() {
        val questions = (1..20).map { index -> question("q$index", "A1") }

        val perfect = ProgressState(
            questionStats = questions.associate { q -> q.id to QuestionStat(attempts = 1, correct = 1) },
        )
        assertEquals(100, StatsCalculator.ruleProgress(perfect, questions, sessionSize = 20))

        val clean = nineteenCorrect()
        assertEquals(95, StatsCalculator.ruleProgress(clean, questions, sessionSize = 20))

        val oneMistake = clean.copy(
            questionStats = clean.questionStats + ("q20" to QuestionStat(attempts = 1, correct = 0)),
        )
        assertEquals(85, StatsCalculator.ruleProgress(oneMistake, questions, sessionSize = 20))

        val correctedAfterMistake = perfect.copy(
            questionStats = perfect.questionStats + ("q1" to QuestionStat(attempts = 2, correct = 1)),
        )
        assertEquals(90, StatsCalculator.ruleProgress(correctedAfterMistake, questions, sessionSize = 20))

        assertEquals(0, StatsCalculator.ruleProgress(ProgressState(), questions, sessionSize = 20))
    }

    @Test
    fun `learning progress averages theme progress`() {
        val ruleA = com.iltatrainer.app.data.Rule(id = "rule-a", sectionId = "articles", order = 1, level = "A1")
        val ruleB = com.iltatrainer.app.data.Rule(id = "rule-b", sectionId = "articles", order = 2, level = "A1")
        val questions = (1..30).map { question("a$it", "A1").copy(ruleId = "rule-a") } +
            (1..30).map { question("b$it", "A1").copy(ruleId = "rule-b") }
        val state = ProgressState(
            questionStats = (1..30).associate { "a$it" to QuestionStat(attempts = 1, correct = 1) },
        )
        assertEquals(50, StatsCalculator.learningProgress(state, listOf(ruleA, ruleB), questions))
        assertEquals(50, StatsCalculator.sectionProgress(state, listOf(ruleA, ruleB), questions))
        assertEquals(0, StatsCalculator.learningProgress(ProgressState(), emptyList(), questions))
    }


    @Test
    fun `repeated correct answers keep raising the theme progress`() {
        val questions = (1..30).map { index -> question("r$index", "A1") }
        val tenCorrect = ProgressState(
            questionStats = (1..10).associate { index ->
                "r$index" to QuestionStat(attempts = 1, correct = 1)
            },
        )
        val before = StatsCalculator.ruleProgress(tenCorrect, questions)
        val after = StatsCalculator.ruleProgress(
            tenCorrect.copy(
                questionStats = tenCorrect.questionStats + ("r1" to QuestionStat(attempts = 2, correct = 2)),
            ),
            questions,
        )
        assertTrue("expected progress to grow: $before -> $after", after > before)
        assertEquals(33, before)
        assertEquals(37, after)

        val perfect = ProgressState(
            questionStats = questions.associate { q -> q.id to QuestionStat(attempts = 1, correct = 1) },
        )
        assertEquals(100, StatsCalculator.ruleProgress(perfect, questions, sessionSize = 20))
    }

}
