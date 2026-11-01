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
        assertEquals(3, result.map { it.id }.toSet().size)
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
        assertTrue(result.all { it.ruleId == "rule-b" })
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
        assertTrue(result.any { it.id == "m1" })
        assertEquals(3, result.count { it.id != "m1" })
        assertTrue(result.filter { it.id != "m1" }.all { it.ruleId == "rule-a" })
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
        assertTrue(result.any { it.id == "nurse-original" })
        assertTrue("similar nurse questions expected, got ${result.map { it.id }}",
            result.any { it.id == "nurse-similar" } && result.any { it.id == "nurse-door" })
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
        assertEquals(listOf("q5", "q1"), result.map { it.id })
    }

    @Test
    fun `topic session contains five matches and twenty five choice questions from a pool`() {
        val pool = (1..5).map { index ->
            Question(
                id = "m$index",
                type = "match",
                ruleId = "art-pool",
                sentence = null,
                prompt = Bilingual(ru = "Сопоставьте $index", en = "Match $index"),
                pairs = (1..4).map { pair ->
                    com.iltatrainer.app.data.MatchPair(
                        left = Bilingual(ru = "левая $pair", en = "left $pair"),
                        right = "right $pair",
                    )
                },
                explanation = Bilingual(ru = "тест", en = "test"),
            )
        } + (1..65).map { index -> question("c$index", "art-pool") }
        val result = SessionBuilder.build(
            config = SessionConfig(PracticeMode.TOPIC, ruleId = "art-pool", size = 30),
            questions = pool,
            state = ProgressState(),
            random = Random(4),
        )
        assertEquals(30, result.size)
        assertEquals(5, result.count { it.type == "match" })
        assertEquals(25, result.count { it.type == "choice" })
        assertEquals(30, result.map { it.id }.toSet().size)
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
        assertFalse(result.any { it.id == "q1" })
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
        assertTrue(result.all { it.ruleId == "rule-b" })
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
