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

    /** Only `choice` questions are checked here; match questions are graded inside the matching UI. */
    fun isCorrect(question: Question, userInput: String): Boolean {
        if (question.type != "choice") return false
        return normalize(userInput) == canonicalAnswer(question)
    }

    fun displayAnswer(value: String): String = normalize(value)
}
