package com.iltatrainer.app.data

import com.iltatrainer.app.core.AppLanguage
import kotlinx.serialization.Serializable

/** A string that exists in Russian and English. */
@Serializable
data class Bilingual(
    val ru: String = "",
    val en: String = "",
) {
    fun text(language: AppLanguage): String = when (language) {
        AppLanguage.RU -> ru.ifBlank { en }
        AppLanguage.EN -> en.ifBlank { ru }
    }

    fun isBlank(): Boolean = ru.isBlank() && en.isBlank()
}

/** Russian translation hint for a sentence with a blank. */
@Serializable
data class Translation(
    val ru: String = "",
    val en: String = "",
)

@Serializable
data class RuleSection(
    val heading: Bilingual = Bilingual(),
    val body: Bilingual = Bilingual(),
    val examples: List<String> = emptyList(),
    /** Optional Russian translations aligned with [examples] (same order). */
    val exampleTranslations: List<String> = emptyList(),
)

@Serializable
data class Rule(
    val id: String,
    val sectionId: String = "articles",
    val order: Int = 0,
    val level: String = "A1",
    val title: Bilingual = Bilingual(),
    val summary: Bilingual = Bilingual(),
    val sections: List<RuleSection> = emptyList(),
)

/** A top-level grammar section shown on the entry screen (Articles, Prepositions, ...). */
@Serializable
data class Section(
    val id: String,
    val order: Int = 0,
    val emoji: String = "",
    val title: Bilingual = Bilingual(),
    val short: Bilingual = Bilingual(),
    val description: Bilingual = Bilingual(),
)

@Serializable
data class SectionsFile(
    val sections: List<Section> = emptyList(),
)

@Serializable
data class MatchPair(
    val left: Bilingual = Bilingual(),
    val right: String = "",
)

/**
 * One exercise. [type] is one of "choice", "fill", "match"; the remaining fields
 * are used depending on the type (the JSON schema is documented in docs/content-schema.md).
 */
@Serializable
data class Question(
    val id: String,
    val type: String,
    val ruleId: String,
    val level: String = "A1",
    val sentence: String? = null,
    val translation: Translation? = null,
    val options: List<String> = emptyList(),
    val answer: String? = null,
    val prompt: Bilingual? = null,
    val pairs: List<MatchPair> = emptyList(),
    val explanation: Bilingual = Bilingual(),
    /** Optional pattern tags used to pick truly similar questions for mistake review. */
    val tags: List<String> = emptyList(),
    /** Word bank for the sentence-building question. */
    val buildKind: String = "affirmative",
    val words: List<String> = emptyList(),
    /** Accepted token sequences for the sentence-building question. */
    val answers: List<List<String>> = emptyList(),
) {
    val isChoice: Boolean get() = type == "choice"
    val isMatch: Boolean get() = type == "match"
    val isBuild: Boolean get() = type == "build"

    /** Sentence with the blank marker replaced by [filler] (used by the UI). */
    fun sentenceWith(filler: String): String = (sentence ?: "").replace("___", filler)
}

/** A content package = one JSON file from assets/data/content. */
@Serializable
data class ContentPackage(
    val rules: List<Rule> = emptyList(),
    val questions: List<Question> = emptyList(),
)

data class ContentLoadResult(
    val sections: List<Section>,
    val rules: List<Rule>,
    val questions: List<Question>,
    val errors: List<String>,
) {
    val sectionsById: Map<String, Section> = sections.associateBy { it.id }
    val rulesById: Map<String, Rule> = rules.associateBy { it.id }
    val questionsById: Map<String, Question> = questions.associateBy { it.id }
    val rulesBySection: Map<String, List<Rule>> = rules.groupBy { it.sectionId }
    val questionsByRule: Map<String, Int> = questions.groupingBy { it.ruleId }.eachCount()

    fun questionsForSection(sectionId: String): List<Question> = questions.filter { question ->
        rulesById[question.ruleId]?.sectionId == sectionId
    }
}
