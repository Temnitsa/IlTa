package com.iltatrainer.app.data

import android.content.res.AssetManager
import kotlinx.serialization.json.Json

/**
 * Loads the section catalogue (`assets/data/sections.json`) and every content package
 * from `assets/data/content`.
 */
class ContentRepository(private val assets: AssetManager) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        coerceInputValues = true
    }

    fun load(): ContentLoadResult {
        val errors = mutableListOf<String>()
        val sections = loadSections(errors)
        val sectionIds = sections.map { it.id }.toSet()
        val sectionOrder = sections.associate { it.id to it.order }

        val rules = LinkedHashMap<String, Rule>()
        val questions = LinkedHashMap<String, Question>()
        val files = contentFiles(errors)

        files.forEach { fileName ->
            val path = "$CONTENT_DIR/$fileName"
            try {
                val raw = assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
                val pkg = json.decodeFromString<ContentPackage>(raw)
                pkg.rules.forEach { rule ->
                    when {
                        rule.id.isBlank() -> errors += "$fileName: rule with empty id skipped"
                        rule.sectionId.isBlank() -> errors += "$fileName: rule '${rule.id}' has no sectionId"
                        rule.sectionId !in sectionIds ->
                            errors += "$fileName: rule '${rule.id}' refers to unknown section '${rule.sectionId}'"
                        rules.putIfAbsent(rule.id, rule) != null ->
                            errors += "$fileName: duplicate rule id '${rule.id}' skipped"
                        else -> Unit
                    }
                }
                pkg.questions.forEach { question ->
                    if (question.id.isBlank()) {
                        errors += "$fileName: question with empty id skipped"
                    } else if (questions.putIfAbsent(question.id, question) != null) {
                        errors += "$fileName: duplicate question id '${question.id}' skipped"
                    }
                }
            } catch (t: Throwable) {
                errors += "$fileName: ${t.message ?: t.javaClass.simpleName}"
            }
        }

        val ruleIds = rules.keys
        val usableQuestions = questions.values.filter { question ->
            when {
                question.ruleId !in ruleIds -> {
                    errors += "Question '${question.id}' refers to unknown rule '${question.ruleId}'"
                    false
                }
                question.type !in SUPPORTED_TYPES -> {
                    errors += "Question '${question.id}' has unsupported type '${question.type}'"
                    false
                }
                !isWellFormed(question) -> {
                    errors += "Question '${question.id}' is malformed and was skipped"
                    false
                }
                else -> true
            }
        }

        val sortedRules = rules.values.sortedWith(
            compareBy<Rule>(
                { sectionOrder[it.sectionId] ?: Int.MAX_VALUE },
                { it.order },
            ),
        )

        return ContentLoadResult(
            sections = sections,
            rules = sortedRules,
            questions = usableQuestions,
            errors = errors,
        )
    }

    private fun loadSections(errors: MutableList<String>): List<Section> {
        val sections = try {
            val raw = assets.open(SECTIONS_PATH).bufferedReader(Charsets.UTF_8).use { it.readText() }
            val file = json.decodeFromString<SectionsFile>(raw)
            file.sections
        } catch (t: Throwable) {
            errors += "Cannot load $SECTIONS_PATH: ${t.message ?: t.javaClass.simpleName}"
            emptyList()
        }
        if (sections.isEmpty()) {
            errors += "No sections found in $SECTIONS_PATH"
            return emptyList()
        }
        val unique = LinkedHashMap<String, Section>()
        sections.sortedBy { it.order }.forEach { section ->
            if (section.id.isBlank()) {
                errors += "section with empty id skipped"
            } else if (unique.putIfAbsent(section.id, section) != null) {
                errors += "duplicate section id '${section.id}' skipped"
            }
        }
        return unique.values.toList()
    }

    private fun contentFiles(errors: MutableList<String>): List<String> = try {
        assets.list(CONTENT_DIR)
            ?.filter { it.endsWith(".json", ignoreCase = true) }
            ?.sorted()
            .orEmpty()
    } catch (t: Throwable) {
        errors += "Cannot list asset folder $CONTENT_DIR: ${t.message}"
        emptyList()
    }

    private fun isWellFormed(question: Question): Boolean = when (question.type) {
        "choice" -> !question.sentence.isNullOrBlank() &&
            question.sentence.contains("___") &&
            question.options.size in 3..4 &&
            question.answer != null &&
            question.answer in question.options &&
            question.explanation.ru.isNotBlank() &&
            question.explanation.en.isNotBlank()
        "match" -> question.pairs.size >= 2 && question.pairs.all { it.right.isNotBlank() } &&
            question.explanation.ru.isNotBlank() &&
            question.explanation.en.isNotBlank()
        else -> false
    }

    companion object {
        const val DATA_DIR = "data"
        const val SECTIONS_PATH = "$DATA_DIR/sections.json"
        const val CONTENT_DIR = "$DATA_DIR/content"
        val SUPPORTED_TYPES = setOf("choice", "match")
    }
}
