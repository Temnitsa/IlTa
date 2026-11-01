package com.iltatrainer.app.domain

import com.iltatrainer.app.data.Bilingual
import com.iltatrainer.app.data.MatchPair
import com.iltatrainer.app.data.Question
import com.iltatrainer.app.data.Translation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerCheckerTest {

    @Test
    fun `build answers accept any accepted order`() {
        val question = Question(
            id = "b1", type = "build", ruleId = "tense-present-simple",
            words = listOf("She", "walks", "to", "school"),
            answers = listOf(
                listOf("She", "walks", "to", "school"),
                listOf("To", "school", "she", "walks"),
            ),
        )
        assertTrue(AnswerChecker.isBuildCorrect(question, listOf("She", "walks", "to", "school")))
        assertTrue(AnswerChecker.isBuildCorrect(question, listOf("To", "school", "she", "walks")))
        assertFalse(AnswerChecker.isBuildCorrect(question, listOf("walks", "She", "to", "school")))
        assertEquals("She walks to school.", AnswerChecker.correctText(question))
    }


    private fun choice(
        answer: String,
        options: List<String>,
        sentence: String = "I saw ___ dog.",
        ruleId: String = "a-an-basics",
    ) = Question(
        id = "q1",
        type = "choice",
        ruleId = ruleId,
        level = "A1",
        sentence = sentence,
        translation = Translation(ru = "Я увидел собаку."),
        options = options,
        answer = answer,
        explanation = Bilingual(ru = "тест", en = "test"),
    )

    @Test
    fun `normalize trims, lowercases and collapses spaces`() {
        assertEquals("a", AnswerChecker.normalize(" A "))
        assertEquals("the", AnswerChecker.normalize("The"))
        assertEquals("in front of", AnswerChecker.normalize("  in   front \t of "))
        assertEquals("\u2014", AnswerChecker.normalize(" \u2014 "))
    }

    @Test
    fun `article choice is checked exactly`() {
        val question = choice("a", listOf("a", "an", "the", "\u2014"))
        assertTrue(AnswerChecker.isCorrect(question, "a"))
        assertTrue(AnswerChecker.isCorrect(question, " A "))
        assertFalse(AnswerChecker.isCorrect(question, "an"))
        assertFalse(AnswerChecker.isCorrect(question, "the"))
        assertFalse(AnswerChecker.isCorrect(question, "\u2014"))
    }

    @Test
    fun `zero article choice accepts the em dash option`() {
        val question = choice("\u2014", listOf("a", "an", "the", "\u2014"))
        assertTrue(AnswerChecker.isCorrect(question, "\u2014"))
        assertFalse(AnswerChecker.isCorrect(question, "the"))
    }

    @Test
    fun `preposition choice is checked case-insensitively`() {
        val question = choice(
            answer = "of",
            options = listOf("of", "from", "at", "with"),
            sentence = "My sister is afraid ___ the dark.",
            ruleId = "prep-adjectives",
        )
        assertTrue(AnswerChecker.isCorrect(question, "of"))
        assertTrue(AnswerChecker.isCorrect(question, " Of "))
        assertFalse(AnswerChecker.isCorrect(question, "from"))
        assertFalse(AnswerChecker.isCorrect(question, ""))
    }

    @Test
    fun `zero preposition option is supported`() {
        val question = choice(
            answer = "\u2014",
            options = listOf("on", "about", "\u2014", "for"),
            sentence = "We discussed ___ the problem for an hour.",
            ruleId = "prep-verbs",
        )
        assertTrue(AnswerChecker.isCorrect(question, "\u2014"))
        assertFalse(AnswerChecker.isCorrect(question, "about"))
    }

    @Test
    fun `match questions are never auto checked`() {
        val match = Question(
            id = "q3",
            type = "match",
            ruleId = "the-unique",
            level = "A2",
            pairs = listOf(MatchPair(left = Bilingual("правило", "rule"), right = "The sun is hot.")),
            explanation = Bilingual(ru = "тест", en = "test"),
        )
        assertFalse(AnswerChecker.isCorrect(match, "a"))
        assertFalse(AnswerChecker.isCorrect(match, ""))
    }

    @Test
    fun `displayAnswer normalizes input`() {
        assertEquals("the", AnswerChecker.displayAnswer(" THE "))
        assertEquals("\u2014", AnswerChecker.displayAnswer("\u2014"))
    }
}
