package com.iltatrainer.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressConfigCodecTest {

    private fun sampleState() = ProgressState(
        totalAnswered = 12,
        totalCorrect = 9,
        questionStats = mapOf(
            "prep-time-0001" to QuestionStat(attempts = 2, correct = 1, lastSeen = 42L),
            "removed-question-id" to QuestionStat(attempts = 1, correct = 0),
        ),
        ruleStats = mapOf("prep-time" to RuleStat(attempts = 2, correct = 1)),
        days = mapOf("2026-11-01" to DayStat(answered = 12, correct = 9)),
        favorites = setOf("prep-time-0001", "removed-question-id"),
        mistakes = mapOf(
            "prep-time-0002" to MistakeEntry(ruleId = "prep-time", originalCorrect = false, similarCorrect = 2),
        ),
        closedMistakes = 4,
        bestStreak = 3,
        settings = Settings(language = "en", theme = "dark", dynamicColor = false, dailyGoal = 30, sessionSize = 15),
    )

    @Test
    fun `export and import are lossless and keep unknown history ids`() {
        val state = sampleState()
        val text = ProgressConfigCodec.encode(
            state = state,
            appVersion = "2.2.0",
            content = ContentInfo(sections = 2, rules = 27, questions = 540),
            exportedAt = "2026-11-01T10:00:00Z",
        )
        val imported = ProgressConfigCodec.decode(text)
        assertEquals(ProgressExport.SCHEMA, imported.sourceSchema)
        assertFalse(imported.migratedFromLegacy)
        assertTrue(imported.warnings.isEmpty())
        assertEquals(state.totalAnswered, imported.state.totalAnswered)
        assertEquals(state.questionStats, imported.state.questionStats)
        assertEquals(state.ruleStats, imported.state.ruleStats)
        assertEquals(state.days, imported.state.days)
        assertEquals(state.favorites, imported.state.favorites)
        assertEquals(state.settings, imported.state.settings)
        assertEquals(state.mistakes, imported.state.mistakes)
        assertEquals(state.closedMistakes, imported.state.closedMistakes)
        assertTrue("removed-question-id must survive in the stats", "removed-question-id" in imported.state.questionStats)
    }

    @Test
    fun `legacy v1 wrapper from the ArticleWise era is imported`() {
        val legacy = """
            {
              "app": "ArticleWise",
              "schema": 1,
              "exportedAt": "2026-10-31T12:00:00Z",
              "state": {
                "version": 1,
                "totalAnswered": 5,
                "totalCorrect": 4,
                "questionStats": {"a-an-basics-0001": {"attempts": 2, "correct": 2}},
                "favorites": ["a-an-basics-0001"],
                "settings": {"language": "ru", "theme": "system", "dailyGoal": 20, "sessionSize": 10}
              }
            }
        """.trimIndent()
        val imported = ProgressConfigCodec.decode(legacy)
        assertEquals(1, imported.sourceSchema)
        assertTrue(imported.migratedFromLegacy)
        assertEquals(5, imported.state.totalAnswered)
        assertEquals(2, imported.state.questionStats.getValue("a-an-basics-0001").correct)
        assertEquals(setOf("a-an-basics-0001"), imported.state.favorites)
    }

    @Test
    fun `future schema and unknown fields are accepted`() {
        val future = """
            {
              "app": "IlTa",
              "schema": 9,
              "appVersion": "9.0.0",
              "brandNewField": {"nested": true},
              "state": {
                "version": 7,
                "totalAnswered": 3,
                "totalCorrect": 2,
                "questionStats": {"q1": {"attempts": 1, "correct": 1, "streak": 4}},
                "newStatsKind": [1, 2, 3],
                "settings": {"language": "en", "theme": "light", "newSetting": "yes"}
              }
            }
        """.trimIndent()
        val imported = ProgressConfigCodec.decode(future)
        assertEquals(9, imported.sourceSchema)
        assertEquals(3, imported.state.totalAnswered)
        assertEquals(1, imported.state.questionStats.getValue("q1").correct)
        assertEquals("en", imported.state.settings.language)
    }

    @Test
    fun `raw progress state without wrapper is imported`() {
        val raw = """
            {"version": 1, "totalAnswered": 2, "totalCorrect": 2, "favorites": ["x"]}
        """.trimIndent()
        val imported = ProgressConfigCodec.decode(raw)
        assertEquals(2, imported.state.totalAnswered)
        assertEquals(setOf("x"), imported.state.favorites)
    }

    @Test
    fun `broken entries are salvaged and totals keep going`() {
        val broken = """
            {
              "schema": 1,
              "state": {
                "totalAnswered": 4,
                "totalCorrect": 3,
                "questionStats": {
                  "ok": {"attempts": 2, "correct": 2},
                  "broken": "not-an-object"
                },
                "ruleStats": {"rule-a": {"attempts": 2, "correct": 2}},
                "days": {"2026-11-01": {"answered": 4, "correct": 3}}
              }
            }
        """.trimIndent()
        val imported = ProgressConfigCodec.decode(broken)
        assertEquals(4, imported.state.totalAnswered)
        assertEquals(2, imported.state.questionStats.getValue("ok").correct)
        assertFalse("broken entry must be skipped", "broken" in imported.state.questionStats)
        assertEquals(1, imported.state.ruleStats.size)
        assertTrue(imported.warnings.isNotEmpty())
    }

    @Test
    fun `invalid json is rejected with a readable error`() {
        val failure = runCatching { ProgressConfigCodec.decode("not json at all") }
        assertTrue(failure.isFailure)
        assertTrue(
            failure.exceptionOrNull()?.message?.contains("JSON") == true,
        )
    }
}
