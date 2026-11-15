package com.iltatrainer.app.content

import com.iltatrainer.app.data.ContentPackage
import com.iltatrainer.app.data.Question
import com.iltatrainer.app.data.Section
import com.iltatrainer.app.data.SectionsFile
import com.iltatrainer.app.domain.AnswerChecker
import com.iltatrainer.app.ui.components.PrepositionArt
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Validates the bundled IlTa content: 9 article themes (55..60 questions), 2 visual preposition
 * themes (45 questions each), 12 tense themes (20 choice + 60 build) and the preposition
 * reference dictionary shown in the visual dictionary screens.
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
        assertEquals(listOf("articles", "prepositions", "tenses"), sections.map { it.id })
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
    fun `article themes have seventy pool questions and prepositions have thirty`() {
        val packages = loadPackages()
        val rules = packages.flatMap { it.rules }
        val questions = packages.flatMap { it.questions }
        val ruleIds = rules.map { it.id }.toSet()

        assertEquals(23, rules.size)
        assertEquals(9, rules.count { it.sectionId == "articles" })
        assertEquals(2, rules.count { it.sectionId == "prepositions" })
        assertEquals(12, rules.count { it.sectionId == "tenses" })
        assertEquals(1580, questions.size)
        assertEquals(530, questions.count { rule -> rules.firstOrNull { it.id == rule.ruleId }?.sectionId == "articles" })
        assertEquals(90, questions.count { rule -> rules.firstOrNull { it.id == rule.ruleId }?.sectionId == "prepositions" })
        assertEquals(960, questions.count { rule -> rules.firstOrNull { it.id == rule.ruleId }?.sectionId == "tenses" })
        assertEquals("match questions were removed", 0, questions.count { it.type == "match" })

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
            if (rule.sectionId == "tenses") {
                assertEquals("tense theme ${rule.id} must have exactly 80 questions", 80, ofRule.size)
                assertEquals("tense theme ${rule.id} must have 20 choice questions", 20, ofRule.count { it.type == "choice" })
                assertEquals("tense theme ${rule.id} must have 60 build questions", 60, ofRule.count { it.type == "build" })
            } else if (rule.sectionId == "articles") {
                assertTrue("article theme ${rule.id} must have 55..60 questions", ofRule.size in 55..60)
                assertEquals("article theme ${rule.id} must contain only choice questions", ofRule.size, ofRule.count { it.type == "choice" })
            } else {
                assertEquals("preposition theme ${rule.id} must have exactly 45 questions", 45, ofRule.size)
                assertEquals("preposition theme ${rule.id} must contain only choice questions", 45, ofRule.count { it.type == "choice" })
            }
        }
    }

    @Test
    fun `rule texts explain the rule without repeating examples`() {
        loadPackages().flatMap { it.rules }.forEach { rule ->
            assertTrue("rule ${rule.id} needs 2..5 sections", rule.sections.size in 2..5)
            rule.sections.forEachIndexed { index, section ->
                val where = "${rule.id} section[$index]"
                assertTrue("$where body.ru too short", section.body.ru.length >= 20)
                assertTrue("$where body.en too short", section.body.en.length >= 20)
                assertTrue("$where needs 3..6 examples", section.examples.size in 3..6)
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
    fun `build questions are well formed`() {
        val packages = loadPackages()
        val builds = packages.flatMap { it.questions }.filter { it.type == "build" }
        assertTrue("at least one build question expected", builds.isNotEmpty())
        builds.forEach { question ->
            assertTrue("${question.id} needs 3..20 words", question.words.size in 3..20)
            assertTrue("${question.id} needs 1..4 accepted orders", question.answers.size in 1..4)
            question.answers.forEach { answer ->
                assertEquals("${question.id} answer must be a permutation of words", question.words.sorted(), answer.sorted())
            }
            assertTrue("${question.id} build question must not have a sentence", question.sentence.isNullOrBlank())
            assertTrue("${question.id} needs translation", !question.translation?.ru.isNullOrBlank())
            assertTrue("${question.id} needs explanations", question.explanation.ru.isNotBlank() && question.explanation.en.isNotBlank())
        }
    }

    @Test
    fun `sentences are unique across the whole content set`() {
        val seenSentences = mutableMapOf<String, String>()
        loadPackages().flatMap { it.questions }.filter { it.type == "choice" }.forEach { question ->
            val key = question.sentence.orEmpty().trim().lowercase().replace(Regex("\\s+"), " ")
            val previous = seenSentences.put(key, question.id)
            assertTrue("duplicate sentence in ${question.id} (also used by $previous)", previous == null)
        }
    }

    @Test
    fun `preposition reference covers both preposition themes`() {
        val file = File(dataDir, "preposition_reference.json")
        assertTrue("Missing preposition_reference.json: ${file.absolutePath}", file.isFile)
        val catalog = json.decodeFromString<com.iltatrainer.app.data.PrepositionCatalog>(file.readText(Charsets.UTF_8))
        val rules = loadPackages().flatMap { it.rules }.filter { it.sectionId == "prepositions" }
        assertEquals("two preposition topics expected", 2, catalog.topics.size)
        assertEquals(
            "reference topics must match preposition rules",
            rules.map { it.id }.toSet(),
            catalog.topics.map { it.ruleId }.toSet(),
        )
        assertEquals(
            "positional preposition list must match the product spec",
            listOf(
                "above", "across", "against", "among", "around", "behind", "below", "beneath",
                "beside", "between", "by", "in", "in front of", "inside", "near", "next to",
                "on", "outside", "over", "through", "under", "underneath",
            ),
            catalog.topics.first { it.ruleId == "prep-position" }.prepositions.map { it.label },
        )
        assertEquals(
            "directional preposition list must match the product spec",
            listOf(
                "across", "against", "along", "around", "away (from)", "down", "from", "in(to)",
                "off (of)", "on(to)", "out (of)", "over", "through", "to", "toward(s)", "under", "up",
            ),
            catalog.topics.first { it.ruleId == "prep-direction" }.prepositions.map { it.label },
        )
        catalog.topics.forEach { topic ->
            assertTrue("topic ${topic.ruleId} needs a title", topic.title.isNotBlank())
            assertTrue("topic ${topic.ruleId} needs a subtitle", topic.subtitle.isNotBlank())
            assertTrue("topic ${topic.ruleId} has too few entries", topic.prepositions.size >= 15)
            assertEquals(
                "topic ${topic.ruleId} has duplicate keys",
                topic.prepositions.size,
                topic.prepositions.map { it.key }.toSet().size,
            )
            topic.prepositions.forEach { entry ->
                assertTrue("${topic.ruleId}/${entry.key} needs a label", entry.label.isNotBlank())
                assertTrue("${topic.ruleId}/${entry.key} needs a translation", entry.translation.isNotBlank())
                assertTrue("${topic.ruleId}/${entry.key} needs a short hint", entry.short.isNotBlank())
                assertTrue("${topic.ruleId}/${entry.key} needs an explanation", entry.explanation.isNotBlank())
                assertTrue("${topic.ruleId}/${entry.key} needs 2..4 examples", entry.examples.size in 2..4)
                entry.examples.forEach { example ->
                    assertTrue("${topic.ruleId}/${entry.key} example needs English", example.en.isNotBlank())
                    assertTrue("${topic.ruleId}/${entry.key} example needs Russian", example.ru.isNotBlank())
                }
            }
        }
    }

    @Test
    fun `every dictionary entry has an illustration`() {
        val file = File(dataDir, "preposition_reference.json")
        assertTrue("Missing preposition_reference.json", file.isFile)
        val catalog = json.decodeFromString<com.iltatrainer.app.data.PrepositionCatalog>(file.readText(Charsets.UTF_8))
        catalog.topics.flatMap { it.prepositions }.forEach { entry ->
            assertNotNull("no drawable generated for ${entry.key}", PrepositionArt.drawableFor(entry.key))
        }
    }
}
