package com.iltatrainer.app.content

import com.iltatrainer.app.data.ContentPackage
import com.iltatrainer.app.data.Question
import com.iltatrainer.app.data.Section
import com.iltatrainer.app.data.SectionsFile
import com.iltatrainer.app.domain.AnswerChecker
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Validates the bundled IlTa content: 27 themes × 20 questions (15 choice + 5 match),
 * clean rule texts (no examples repeated in bodies) and consistent question shape.
 */
class ContentValidationTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val dataDir = File("src/main/assets/data")

    companion object {
        private val ARTICLE_OPTIONS = setOf("a", "an", "the", "\u2014")
        private val LEVELS = setOf("A1", "A2", "B1", "B2")
    }

    private fun loadSections(): List<Section> {
        val file = File(dataDir, "sections.json")
        assertTrue("Missing sections.json: ${file.absolutePath}", file.isFile)
        return json.decodeFromString<SectionsFile>(file.readText(Charsets.UTF_8)).sections
    }

    private fun loadPackages(): List<ContentPackage> {
        val dir = File(dataDir, "content")
        assertTrue("Content dir not found: ${dir.absolutePath}", dir.isDirectory)
        val files = dir.listFiles { file -> file.extension == "json" }?.sortedBy { it.name }.orEmpty()
        assertTrue("No JSON content files in ${dir.absolutePath}", files.isNotEmpty())
        return files.map { file ->
            json.decodeFromString<ContentPackage>(file.readText(Charsets.UTF_8))
        }
    }

    @Test
    fun `section catalogue contains articles and prepositions`() {
        val sections = loadSections()
        assertEquals(listOf("articles", "prepositions"), sections.map { it.id })
        sections.forEach { section ->
            assertTrue("section ${section.id} has blank title", section.title.ru.isNotBlank() && section.title.en.isNotBlank())
            assertTrue("section ${section.id} has blank short", section.short.ru.isNotBlank() && section.short.en.isNotBlank())
            assertTrue(
                "section ${section.id} has blank description",
                section.description.ru.isNotBlank() && section.description.en.isNotBlank(),
            )
            assertTrue("section ${section.id} has no emoji", section.emoji.isNotBlank())
        }
    }

    @Test
    fun `every theme has exactly thirty questions twenty five choice and five match`() {
        val packages = loadPackages()
        val rules = packages.flatMap { it.rules }
        val questions = packages.flatMap { it.questions }
        val ruleIds = rules.map { it.id }.toSet()

        assertEquals(27, rules.size)
        assertEquals(20, rules.count { it.sectionId == "articles" })
        assertEquals(7, rules.count { it.sectionId == "prepositions" })
        assertEquals(810, questions.size)
        assertEquals(675, questions.count { it.type == "choice" })
        assertEquals(135, questions.count { it.type == "match" })
        assertEquals(600, questions.count { rule -> rules.firstOrNull { it.id == rule.ruleId }?.sectionId == "articles" })
        assertEquals(210, questions.count { rule -> rules.firstOrNull { it.id == rule.ruleId }?.sectionId == "prepositions" })

        assertEquals("rule ids must be unique", rules.size, ruleIds.size)
        assertEquals("question ids must be unique", questions.size, questions.map { it.id }.toSet().size)
        assertEquals("no fill questions are allowed", 0, questions.count { it.type == "fill" })

        val sectionIds = loadSections().map { it.id }.toSet()
        assertTrue("every rule must reference a known section", rules.all { it.sectionId in sectionIds })
        questions.forEach { question ->
            assertNotNull(
                "question ${question.id} references unknown rule ${question.ruleId}",
                rules.firstOrNull { it.id == question.ruleId },
            )
        }

        rules.forEach { rule ->
            val ofRule = questions.filter { it.ruleId == rule.id }
            assertEquals("theme ${rule.id} must have exactly 30 questions", 30, ofRule.size)
            assertEquals("theme ${rule.id} must have 25 choice questions", 25, ofRule.count { it.type == "choice" })
            assertEquals("theme ${rule.id} must have 5 match questions", 5, ofRule.count { it.type == "match" })
        }
    }

    @Test
    fun `rule texts explain the rule without repeating examples`() {
        loadPackages().flatMap { it.rules }.forEach { rule ->
            assertTrue("rule ${rule.id} needs 2..5 sections", rule.sections.size in 2..5)
            rule.sections.forEachIndexed { index, section ->
                val where = "${rule.id} section[$index]"
                assertTrue("$where body.ru too short", section.body.ru.length >= 60)
                assertTrue("$where body.en too short", section.body.en.length >= 60)
                assertTrue("$where needs 3..5 examples", section.examples.size in 3..5)
                val bodyRu = section.body.ru.lowercase()
                val bodyEn = section.body.en.lowercase()
                section.examples.forEach { example ->
                    val probe = example.trim().lowercase()
                    if (probe.length >= 4) {
                        assertFalse(
                            "$where body repeats example '$example'",
                            probe in bodyRu || probe in bodyEn,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `choice questions are well formed`() {
        val packages = loadPackages()
        val rulesById = packages.flatMap { it.rules }.associateBy { it.id }
        packages.flatMap { it.questions }.filter { it.type == "choice" }.forEach { question ->
            val sentence = question.sentence.orEmpty()
            assertEquals("${question.id} must have exactly one blank", 1, sentence.split("___").size - 1)
            assertTrue("${question.id} options must be 3..4", question.options.size in 3..4)
            assertEquals("${question.id} options must be distinct", question.options.size, question.options.toSet().size)
            assertTrue("${question.id} answer must be in options", question.answer in question.options)
            assertTrue("${question.id} must have a Russian translation", !question.translation?.ru.isNullOrBlank())
            assertTrue("${question.id} needs a level", question.level in LEVELS)
            assertTrue("${question.id} needs an English explanation", question.explanation.en.isNotBlank())
            assertTrue("${question.id} needs a Russian explanation", question.explanation.ru.isNotBlank())
            val ruleSection = rulesById[question.ruleId]?.sectionId
            if (ruleSection == "articles") {
                assertEquals(
                    "${question.id} article question must use the four article options",
                    ARTICLE_OPTIONS,
                    question.options.toSet(),
                )
            }
            assertTrue(
                "${question.id} canonical answer must match",
                AnswerChecker.normalize(question.answer.orEmpty()) == AnswerChecker.canonicalAnswer(question),
            )
        }
    }

    @Test
    fun `match questions are well formed`() {
        loadPackages().flatMap { it.questions }.filter { it.type == "match" }.forEach { question ->
            assertEquals("${question.id} must have exactly 4 pairs", 4, question.pairs.size)
            val lefts = question.pairs.map { it.left.en.ifBlank { it.left.ru } }
            val rights = question.pairs.map { it.right }
            assertEquals("${question.id}: duplicate left values", lefts.size, lefts.toSet().size)
            assertEquals("${question.id}: duplicate right values", rights.size, rights.toSet().size)
            assertTrue("${question.id}: match texts must be filled", lefts.all { it.isNotBlank() } && rights.all { it.isNotBlank() })
            assertFalse("${question.id}: blank in match text", rights.any { it.contains("___") })
            assertTrue("${question.id}: match needs an explanation", question.explanation.ru.isNotBlank())
        }
    }

    @Test
    fun `sentences and match examples are unique across the whole content set`() {
        val seenSentences = mutableMapOf<String, String>()
        val seenRights = mutableMapOf<String, String>()
        loadPackages().flatMap { it.questions }.forEach { question ->
            if (question.type == "choice") {
                val key = question.sentence.orEmpty().trim().lowercase().replace(Regex("\\s+"), " ")
                val previous = seenSentences.put(key, question.id)
                assertTrue("duplicate sentence in ${question.id} (also used by $previous)", previous == null)
            } else {
                question.pairs.forEach { pair ->
                    val key = pair.right.trim().lowercase().replace(Regex("\\s+"), " ")
                    val previous = seenRights.put(key, question.id)
                    assertTrue("duplicate match example in ${question.id} (also used by $previous)", previous == null)
                }
            }
        }
    }
}
