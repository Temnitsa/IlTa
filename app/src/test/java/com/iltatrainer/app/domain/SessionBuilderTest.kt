package com.iltatrainer.app.domain

import com.iltatrainer.app.data.Bilingual
import com.iltatrainer.app.data.MistakeEntry
import com.iltatrainer.app.data.ProgressState
import com.iltatrainer.app.data.Question
import com.iltatrainer.app.data.QuestionStat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SessionBuilderTest {

    private fun question(id: String, ruleId: String, level: String = "A1") = Question(
        id = id,
        type = "choice",
        ruleId = ruleId,
        level = level,
        sentence = "Sentence ___ for $id.",
        options = listOf("a", "an", "the", "—"),
        answer = "a",
        explanation = Bilingual(ru = "тест", en = "test"),
    )

    private val questions = listOf(
        question("q1", "rule-a"),
        question("q2", "rule-a"),
        question("q3", "rule-a"),
        question("q4", "rule-b", level = "A2"),
        question("q5", "rule-b", level = "A2"),
    )

    @Test
    fun `quick session returns requested number of unique questions`() {
        val result = SessionBuilder.build(
            config = SessionConfig(PracticeMode.QUICK, size = 3),
            questions = questions,
            state = ProgressState(),
            random = Random(42),
        )
        assertEquals(3, result.size)
        assertEquals(3, result.map { it.question.id }.toSet().size)
    }

    @Test
    fun `quick session returns all questions when pool is smaller than size`() {
        val result = SessionBuilder.build(
            config = SessionConfig(PracticeMode.QUICK, size = 20),
            questions = questions,
            state = ProgressState(),
            random = Random(7),
        )
        assertEquals(questions.size, result.size)
    }

    @Test
    fun `topic session filters by rule`() {
        val result = SessionBuilder.build(
            config = SessionConfig(PracticeMode.TOPIC, ruleId = "rule-b", size = 10),
            questions = questions,
            state = ProgressState(),
            random = Random(1),
        )
        assertEquals(2, result.size)
        assertTrue(result.all { it.question.ruleId == "rule-b" })
    }

    @Test
    fun `mistake review session contains the original plus three similar questions`() {
        val reviewQuestions = (1..8).map { index -> question("m$index", "rule-a") }
        val state = ProgressState(
            mistakes = mapOf(
                "m1" to MistakeEntry(ruleId = "rule-a", openedAt = 1L),
            ),
        )
        val result = SessionBuilder.build(
            config = SessionConfig(PracticeMode.MISTAKES, size = 10),
            questions = reviewQuestions,
            state = state,
            random = Random(5),
        )
        assertEquals(4, result.size)
        assertTrue(result.any { it.question.id == "m1" })
        assertEquals(3, result.count { it.question.id != "m1" })
        assertTrue(result.filter { it.question.id != "m1" }.all { it.question.ruleId == "rule-a" })
        assertEquals(1, SessionBuilder.openMistakeCount(state))
        assertFalse(SessionBuilder.openMistakes(state).isEmpty())
    }

    @Test
    fun `mistake review prefers questions with similar wording`() {
        val similarQuestions = listOf(
            Question(
                id = "nurse-original", type = "choice", ruleId = "rule-a",
                sentence = "The nurse walked into the room and sat by the bed.",
                options = listOf("a", "an", "the", "—"), answer = "the",
                explanation = Bilingual(en = "the room is specific"),
            ),
            Question(
                id = "nurse-similar", type = "choice", ruleId = "rule-a",
                sentence = "The nurse walked into the ward and checked the bed.",
                options = listOf("a", "an", "the", "—"), answer = "the",
                explanation = Bilingual(en = "the ward is specific"),
            ),
            Question(
                id = "nurse-door", type = "choice", ruleId = "rule-a",
                sentence = "The nurse opened the door and left the room.",
                options = listOf("a", "an", "the", "—"), answer = "the",
                explanation = Bilingual(en = "the door is specific"),
            ),
            Question(
                id = "cat", type = "choice", ruleId = "rule-a",
                sentence = "A cat sat on the mat in the kitchen.",
                options = listOf("a", "an", "the", "—"), answer = "a",
                explanation = Bilingual(en = "first mention"),
            ),
            Question(
                id = "tea", type = "choice", ruleId = "rule-a",
                sentence = "My brother drinks tea every morning.",
                options = listOf("a", "an", "the", "—"), answer = "—",
                explanation = Bilingual(en = "general meaning"),
            ),
        )
        val state = ProgressState(
            mistakes = mapOf("nurse-original" to MistakeEntry(ruleId = "rule-a", openedAt = 1L)),
        )
        val result = SessionBuilder.build(
            config = SessionConfig(PracticeMode.MISTAKES, size = 10),
            questions = similarQuestions,
            state = state,
            random = Random(3),
        )
        assertEquals(4, result.size)
        assertTrue(result.any { it.question.id == "nurse-original" })
        assertTrue("similar nurse questions expected, got ${result.map { it.question.id }}",
            result.any { it.question.id == "nurse-similar" } && result.any { it.question.id == "nurse-door" })
    }

    @Test
    fun `mistake review session is empty without open mistakes`() {
        val result = SessionBuilder.build(
            config = SessionConfig(PracticeMode.MISTAKES, size = 10),
            questions = questions,
            state = ProgressState(),
            random = Random(9),
        )
        assertTrue(result.isEmpty())
        assertEquals(0, SessionBuilder.openMistakeCount(ProgressState()))
    }

    @Test
    fun `retry session follows the explicit id order`() {
        val result = SessionBuilder.build(
            config = SessionConfig(PracticeMode.RETRY, size = 3, questionIds = listOf("q5", "q1", "unknown")),
            questions = questions,
            state = ProgressState(),
            random = Random(11),
        )
        assertEquals(listOf("q5", "q1"), result.map { it.question.id })
    }

    @Test
    fun `tense topic session contains fifteen choice and fifteen build questions`() {
        val pool = (1..20).map { i -> question("c$i", "tense-pool") } +
            (1..60).map { i ->
                Question(
                    id = "b$i", type = "build", ruleId = "tense-pool",
                    words = listOf("I", "work", "here"),
                    answers = listOf(listOf("I", "work", "here")),
                    buildKind = "affirmative",
                    explanation = Bilingual(ru = "тест", en = "test"),
                )
            }
        val result = SessionBuilder.build(
            config = SessionConfig(PracticeMode.TOPIC, ruleId = "tense-pool", size = 30),
            questions = pool,
            state = ProgressState(),
            random = Random(6),
        )
        assertEquals(30, result.size)
        assertEquals(15, result.count { it.question.type == "choice" })
        assertEquals(15, result.count { it.question.type == "build" })
    }

    @Test
    fun `article topic session contains thirty choice questions from a pool`() {
        val pool = (1..55).map { index -> question("c$index", "art-pool") }
        val result = SessionBuilder.build(
            config = SessionConfig(PracticeMode.TOPIC, ruleId = "art-pool", size = 30),
            questions = pool,
            state = ProgressState(),
            random = Random(4),
        )
        assertEquals(30, result.size)
        assertEquals(30, result.count { it.question.type == "choice" })
        assertEquals(30, result.map { it.question.id }.toSet().size)
    }

    @Test
    fun `write only session types the answers of every choice question`() {
        val pool = questions + (1..3).map { index ->
            Question(
                id = "b$index", type = "build", ruleId = "rule-a",
                words = listOf("I", "work", "here"),
                answers = listOf(listOf("I", "work", "here")),
                explanation = Bilingual(ru = "тест", en = "test"),
            )
        }
        val result = SessionBuilder.build(
            config = SessionConfig(PracticeMode.QUICK, size = 20, types = setOf("write")),
            questions = pool,
            state = ProgressState(),
            random = Random(9),
        )
        assertEquals(pool.count { it.isChoice }, result.size)
        assertTrue(result.all { it.question.isChoice })
        assertTrue(result.all { it.typed })
    }

    @Test
    fun `build only session never asks for typed answers`() {
        val pool = questions + (1..3).map { index ->
            Question(
                id = "b$index", type = "build", ruleId = "rule-a",
                words = listOf("I", "work", "here"),
                answers = listOf(listOf("I", "work", "here")),
                explanation = Bilingual(ru = "тест", en = "test"),
            )
        }
        val result = SessionBuilder.build(
            config = SessionConfig(PracticeMode.QUICK, size = 20, types = setOf("build")),
            questions = pool,
            state = ProgressState(),
            random = Random(12),
        )
        assertEquals(3, result.size)
        assertTrue(result.all { it.question.isBuild })
        assertTrue(result.none { it.typed })
    }

    @Test
    fun `choice and write split choice questions between options and typing`() {
        val pool = (1..6).map { index -> question("c$index", "rule-a") }
        val result = SessionBuilder.build(
            config = SessionConfig(PracticeMode.QUICK, size = 6, types = setOf("choice", "write")),
            questions = pool,
            state = ProgressState(),
            random = Random(8),
        )
        assertEquals(6, result.size)
        assertEquals(3, result.count { it.typed })
        assertEquals(3, result.count { !it.typed })
    }

    @Test
    fun `all three types can be selected together`() {
        val pool = (1..10).map { index -> question("c$index", "rule-a") } +
            (1..10).map { index ->
                Question(
                    id = "b$index", type = "build", ruleId = "rule-a",
                    words = listOf("I", "work", "here"),
                    answers = listOf(listOf("I", "work", "here")),
                    explanation = Bilingual(ru = "тест", en = "test"),
                )
            }
        val result = SessionBuilder.build(
            config = SessionConfig(
                PracticeMode.QUICK,
                size = 20,
                types = setOf("choice", "write", "build"),
            ),
            questions = pool,
            state = ProgressState(),
            random = Random(2),
        )
        assertEquals(20, result.size)
        assertEquals(10, result.count { it.question.isBuild })
        assertEquals(10, result.count { it.question.isChoice })
        assertEquals(5, result.count { it.typed })
    }

    @Test
    fun `write session respects selected topics`() {
        val result = SessionBuilder.build(
            config = SessionConfig(
                PracticeMode.QUICK,
                size = 10,
                ruleIds = listOf("rule-b"),
                types = setOf("write"),
            ),
            questions = questions,
            state = ProgressState(),
            random = Random(10),
        )
        assertEquals(2, result.size)
        assertTrue(result.all { it.question.ruleId == "rule-b" })
        assertTrue(result.all { it.typed })
    }

    @Test
    fun `unseen questions are preferred for a topic`() {
        val state = ProgressState(
            questionStats = mapOf("q1" to QuestionStat(attempts = 3, correct = 3)),
        )
        val result = SessionBuilder.build(
            config = SessionConfig(PracticeMode.TOPIC, ruleId = "rule-a", size = 2),
            questions = questions,
            state = state,
            random = Random(13),
        )
        assertEquals(2, result.size)
        assertFalse(result.any { it.question.id == "q1" })
    }

    @Test
    fun `quick session can be limited to selected topics`() {
        val result = SessionBuilder.build(
            config = SessionConfig(PracticeMode.QUICK, size = 10, ruleIds = listOf("rule-b")),
            questions = questions,
            state = ProgressState(),
            random = Random(21),
        )
        assertEquals(2, result.size)
        assertTrue(result.all { it.question.ruleId == "rule-b" })
    }

    @Test
    fun `quick session with empty ruleIds keeps all topics`() {
        val result = SessionBuilder.build(
            config = SessionConfig(PracticeMode.QUICK, size = 20, ruleIds = emptyList()),
            questions = questions,
            state = ProgressState(),
            random = Random(22),
        )
        assertEquals(questions.size, result.size)
    }

}
