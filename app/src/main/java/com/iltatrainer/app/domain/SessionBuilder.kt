package com.iltatrainer.app.domain

import com.iltatrainer.app.data.MistakeEntry
import com.iltatrainer.app.data.ProgressState
import com.iltatrainer.app.data.Question
import kotlin.random.Random

enum class PracticeMode {
    /** Mixed questions across all rules, weighted towards weak / unseen items. */
    QUICK,

    /** Only questions of one rule. */
    TOPIC,

    /** Open mistakes: original question + 3 similar questions of the same theme. */
    MISTAKES,

    /** Explicit list of question ids (for "retry these mistakes"). */
    RETRY,
}

data class SessionConfig(
    val mode: PracticeMode,
    val ruleId: String? = null,
    /** Optional subset of rules for a mixed quick practice (empty = all rules). */
    val ruleIds: List<String> = emptyList(),
    val size: Int = 10,
    val questionIds: List<String> = emptyList(),
)

data class SessionMistake(
    val question: Question,
    val userInput: String,
)

data class SessionResult(
    val total: Int,
    val correct: Int,
    val mistakes: List<SessionMistake> = emptyList(),
) {
    val percent: Int get() = if (total <= 0) 0 else (correct * 100.0 / total).toInt()
    val allCorrect: Boolean get() = total > 0 && correct == total
    val mistakeIds: List<String> get() = mistakes.map { it.question.id }
}

object SessionBuilder {

    fun build(
        config: SessionConfig,
        questions: List<Question>,
        state: ProgressState,
        random: Random = Random.Default,
    ): List<Question> {
        val size = config.size.coerceAtLeast(1)
        val byId = questions.associateBy { it.id }

        val selected = when (config.mode) {
            PracticeMode.RETRY -> config.questionIds.mapNotNull { byId[it] }.take(size)
            PracticeMode.TOPIC -> questions
                .filter { it.ruleId == config.ruleId }
                .sortedWith(
                    compareBy<Question> { if (it.id in state.questionStats) 1 else 0 }
                        .thenBy { accuracy(state, it.id) }
                )
                .take(size)
                .shuffled(random)
            PracticeMode.MISTAKES -> buildMistakeReview(state, questions, size, random)
            PracticeMode.QUICK -> weightedPick(
                if (config.ruleIds.isEmpty()) questions else questions.filter { it.ruleId in config.ruleIds },
                state,
                size,
                random,
            )
        }
        return selected
    }

    /** Open mistakes, oldest first. */
    fun openMistakes(state: ProgressState): List<Pair<String, MistakeEntry>> =
        state.mistakes.entries.sortedBy { it.value.openedAt }.map { it.key to it.value }

    /**
     * Builds a mistake-review session: for every open mistake the original question
     * plus 3 similar questions of the same theme. At most `size / 4` mistakes per session.
     */
    fun buildMistakeReview(
        state: ProgressState,
        questions: List<Question>,
        size: Int,
        random: Random,
    ): List<Question> {
        val open = openMistakes(state)
        if (open.isEmpty()) return emptyList()
        val maxMistakes = (size / 4).coerceAtLeast(1)
        val byId = questions.associateBy { it.id }
        val byRule = questions.groupBy { it.ruleId }
        val pickedIds = LinkedHashSet<String>()
        open.take(maxMistakes).forEach { (questionId, entry) ->
            val original = byId[questionId] ?: return@forEach
            pickedIds += original.id
            val similar = byRule[entry.ruleId].orEmpty()
                .filter { it.id != questionId && it.id !in pickedIds && it.id !in state.mistakes }
                .shuffled(random)
                .sortedByDescending { candidate -> similarityScore(original, candidate) }
                .take(3)
            similar.forEach { pickedIds += it.id }
        }
        return pickedIds.mapNotNull { byId[it] }.shuffled(random)
    }

    /** Number of mistakes currently open for review. */
    fun openMistakeCount(state: ProgressState): Int = state.mistakes.size

    /**
     * Similarity used for mistake review: questions that share the same context words
     * (or the same article/preposition answer) are considered closer to the mistake.
     */
    private fun similarityScore(original: Question, candidate: Question): Double {
        val sentenceOverlap = jaccard(sentenceTokens(original), sentenceTokens(candidate))
        val explanationOverlap = jaccard(explanationTokens(original), explanationTokens(candidate))
        var score = sentenceOverlap * 0.75 + explanationOverlap * 0.25
        if (original.answer != null && original.answer == candidate.answer) score += 0.2
        if (original.level == candidate.level) score += 0.03
        return score
    }

    private fun jaccard(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val intersection = a.count { it in b }
        val union = a.size + b.size - intersection
        return if (union == 0) 0.0 else intersection.toDouble() / union
    }

    private fun sentenceTokens(question: Question): Set<String> = buildSet {
        addAll(tokens(question.sentence.orEmpty()))
        question.prompt?.let { prompt ->
            addAll(tokens(prompt.en))
            addAll(tokens(prompt.ru))
        }
        question.pairs.forEach { pair ->
            addAll(tokens(pair.left.en))
            addAll(tokens(pair.left.ru))
            addAll(tokens(pair.right))
        }
    }

    private fun explanationTokens(question: Question): Set<String> = tokens(question.explanation.en)

    private fun tokens(text: String): Set<String> =
        Regex("[\\p{L}']+").findAll(text.lowercase())
            .map { it.value.replace("'", "") }
            .filter { it.length > 2 && it !in STOP_WORDS }
            .toSet()

    private fun accuracy(state: ProgressState, questionId: String): Float {
        val stat = state.questionStats[questionId] ?: return 0f
        return if (stat.attempts <= 0) 0f else stat.correct.toFloat() / stat.attempts
    }

    /** Weighted sampling without replacement: unseen and weak questions are more likely. */
    private fun weightedPick(
        candidates: List<Question>,
        state: ProgressState,
        size: Int,
        random: Random,
    ): List<Question> {
        if (candidates.size <= size) return candidates.shuffled(random)
        val pool = candidates.toMutableList()
        val picked = ArrayList<Question>(size)
        repeat(size) {
            val weights = pool.map { question ->
                val stat = state.questionStats[question.id]
                when {
                    stat == null || stat.attempts == 0 -> UNSEEN_WEIGHT
                    else -> {
                        val acc = stat.correct.toFloat() / stat.attempts
                        (MIN_WEIGHT + (1f - acc) * WEAK_BONUS).coerceAtLeast(MIN_WEIGHT)
                    }
                }
            }
            var ticket = random.nextDouble() * weights.sum()
            var index = 0
            while (index < pool.size - 1 && ticket > weights[index]) {
                ticket -= weights[index]
                index++
            }
            picked += pool.removeAt(index)
        }
        return picked.shuffled(random)
    }

    private val STOP_WORDS = setOf(
        "the", "and", "for", "you", "your", "with", "that", "this", "these", "those",
        "was", "were", "are", "have", "has", "had", "not", "but", "from", "they",
        "their", "them", "there", "then", "than", "when", "what", "which", "who",
        "how", "why", "all", "any", "some", "one", "two", "three", "into", "onto",
        "over", "under", "about", "after", "before", "because", "while", "also",
        "can", "could", "will", "would", "should", "may", "might", "must", "been",
        "being", "does", "did", "doing", "just", "only", "very", "more", "most",
        "our", "out", "off", "its", "his", "her", "him", "she", "you", "who",
        "его", "она", "они", "оно", "это", "этот", "эта", "для", "или", "при",
        "над", "под", "без", "про", "как", "так", "что", "чтобы", "же", "ли",
        "мы", "вы", "он", "она", "они", "всё", "все", "где", "когда", "если",
        "ещё", "уже", "очень", "только", "нужно", "можно", "будет", "есть",
    )

    private const val MISTAKE_THRESHOLD = 0.75f
    private const val UNSEEN_WEIGHT = 3.0
    private const val MIN_WEIGHT = 0.4
    private const val WEAK_BONUS = 2.6
}
