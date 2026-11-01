package com.iltatrainer.app.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    @Test
    fun `progress state round trips through json`() {
        val state = ProgressState(
            totalAnswered = 10,
            totalCorrect = 7,
            questionStats = mapOf("q1" to QuestionStat(attempts = 3, correct = 2, lastSeen = 42L)),
            ruleStats = mapOf("rule-a" to RuleStat(attempts = 3, correct = 2)),
            days = mapOf("2026-10-31" to DayStat(answered = 5, correct = 4)),
            favorites = setOf("q1"),
            bestStreak = 4,
            settings = Settings(
                language = "en",
                theme = "dark",
                dynamicColor = false,
                dailyGoal = 30,
                sessionSize = 15,
            ),
        )
        val text = json.encodeToString(ProgressState.serializer(), state)
        val decoded = json.decodeFromString(ProgressState.serializer(), text)
        assertEquals(state, decoded)
    }

    @Test
    fun `default progress state is empty and in sync with schema version`() {
        val state = ProgressState()
        assertEquals(ProgressState.CURRENT_VERSION, state.version)
        assertEquals(0, state.totalAnswered)
        assertTrue(state.questionStats.isEmpty())
        assertTrue(state.favorites.isEmpty())
        assertEquals(listOf("ru", "system"), listOf(state.settings.language, state.settings.theme))
    }

    @Test
    fun `export wrapper decodes schema and state`() {
        val state = ProgressState(totalAnswered = 3, totalCorrect = 2)
        val payload = ProgressExport(exportedAt = "2026-10-31T12:00:00Z", state = state)
        val text = json.encodeToString(ProgressExport.serializer(), payload)
        val decoded = json.decodeFromString(ProgressExport.serializer(), text)
        assertEquals(ProgressExport.APP_ID, decoded.app)
        assertEquals(ProgressExport.SCHEMA, decoded.schema)
        assertEquals(state, decoded.state)
    }

    @Test
    fun `unknown fields are ignored for forward compatibility`() {
        val raw = """{"version":1,"totalAnswered":2,"totalCorrect":1,"futureField":"x","settings":{"language":"ru","theme":"light"}}"""
        val decoded = json.decodeFromString(ProgressState.serializer(), raw)
        assertEquals(2, decoded.totalAnswered)
        assertEquals("light", decoded.settings.theme)
    }
}
