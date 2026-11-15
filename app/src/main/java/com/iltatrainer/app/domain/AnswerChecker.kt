package com.iltatrainer.app.domain

import com.iltatrainer.app.data.Question

/** Pure answer-checking logic for multiple-choice questions: no Android dependencies. */
object AnswerChecker {

    const val ZERO_ITEM = "\u2014" // em dash: zero article / no preposition

    fun normalize(raw: String): String {
        return raw
            .replace('\u00A0', ' ')
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
    }

    fun canonicalAnswer(question: Question): String = normalize(question.answer.orEmpty())

    /**
     * Checks a typed or tapped answer for a `choice` question. The same normalization is used by
     * the "Письмо" mode, so `at`/`At`/` at ` are all accepted. Other question types return false.
     */
    fun isCorrect(question: Question, userInput: String): Boolean {
        if (question.type != "choice") return false
        return normalize(userInput) == canonicalAnswer(question)
    }

    fun isBuildCorrect(question: Question, selectedTokens: List<String>): Boolean {
        if (question.type != "build") return false
        return question.answers.any { answer -> answer == selectedTokens }
    }

    /** Human-readable correct answer for any non-choice exercise. */
    fun correctText(question: Question): String = when {
        question.isBuild -> {
            val raw = question.answers.firstOrNull()?.joinToString(" ").orEmpty()
            if (raw.isBlank()) {
                ""
            } else {
                val sentence = raw
                    .replaceFirstChar { char -> char.uppercaseChar() }
                    .replace(Regex("\\bi\\b"), "I")
                sentence + if (question.buildKind == "question") "?" else "."
            }
        }
        else -> ""
    }

    fun displayAnswer(value: String): String = normalize(value)
}
