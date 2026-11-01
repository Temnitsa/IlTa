package com.iltatrainer.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Metadata about the content bank, stored in exported configs for diagnostics and migrations. */
@Serializable
data class ContentInfo(
    val sections: Int = 0,
    val rules: Int = 0,
    val questions: Int = 0,
    val contentSchema: Int = 3,
)

/**
 * The portable progress/config file of IlTa.
 *
 * Forward/backward compatibility rules:
 * - any future [schema] is accepted on import (best effort, unknown fields are ignored);
 * - files without the wrapper (`state` block) are treated as raw [ProgressState];
 * - if strict decoding fails, known fields are salvaged one by one;
 * - unknown question/rule ids are kept in the maps, so the stats survive content edits
 *   and can be used again if the same ids return in a later app version.
 */
@Serializable
data class ProgressExport(
    val app: String = APP_ID,
    val schema: Int = SCHEMA,
    val appVersion: String = "",
    val exportedAt: String = "",
    val content: ContentInfo? = null,
    val state: ProgressState = ProgressState(),
) {
    companion object {
        const val APP_ID = "IlTa"
        const val SCHEMA = 3
        const val MIN_SCHEMA = 1
    }
}

data class ImportedProgress(
    val state: ProgressState,
    val sourceSchema: Int,
    val migratedFromLegacy: Boolean,
    val warnings: List<String> = emptyList(),
)

/** Pure-Kotlin encoder/decoder for the progress config (unit-testable, no Android deps). */
object ProgressConfigCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val parser = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    fun encode(
        state: ProgressState,
        appVersion: String,
        content: ContentInfo?,
        exportedAt: String,
    ): String = json.encodeToString(
        ProgressExport.serializer(),
        ProgressExport(
            appVersion = appVersion,
            exportedAt = exportedAt,
            content = content,
            state = state,
        ),
    )

    /**
     * Decodes an exported config of any known or future IlTa version.
     * Throws [IllegalArgumentException] only when the file is not a JSON object at all.
     */
    fun decode(raw: String): ImportedProgress {
        val text = raw.trim()
        require(text.isNotEmpty()) { "The backup file is empty" }
        val root = runCatching { parser.parseToJsonElement(text) }.getOrNull()
            ?: throw IllegalArgumentException("The file is not valid JSON")
        require(root is JsonObject) { "The backup file must contain a JSON object" }

        val warnings = mutableListOf<String>()
        val sourceSchema = root.intValue("schema") ?: 1
        val stateElement: JsonElement = when (val nested = root["state"]) {
            is JsonObject -> nested
            else -> {
                warnings += "No `state` block found; the file is read as a raw progress state."
                root
            }
        }

        val direct = runCatching {
            json.decodeFromString(ProgressState.serializer(), stateElement.toString())
        }.getOrNull()
        if (direct != null) {
            return ImportedProgress(
                state = direct,
                sourceSchema = sourceSchema,
                migratedFromLegacy = sourceSchema < ProgressExport.SCHEMA,
                warnings = warnings,
            )
        }

        warnings += "Some fields use an unexpected format; importing everything that can be recovered."
        val salvaged = salvage(stateElement as? JsonObject ?: JsonObject(emptyMap()), warnings)
        return ImportedProgress(
            state = salvaged,
            sourceSchema = sourceSchema,
            migratedFromLegacy = true,
            warnings = warnings,
        )
    }

    private fun salvage(obj: JsonObject, warnings: MutableList<String>): ProgressState {
        fun <T> read(keys: List<String>, decode: (JsonElement) -> T?): T? {
            keys.forEach { key ->
                val element = obj[key] ?: return@forEach
                runCatching { decode(element) }.getOrNull()?.let { return it }
            }
            return null
        }

        fun int(keys: List<String>): Int? = read(keys) { it.jsonPrimitive.intOrNull }
        fun long(keys: List<String>): Long? = read(keys) { it.jsonPrimitive.longOrNull }
        fun string(keys: List<String>): String? = read(keys) { it.jsonPrimitive.contentOrNull }

        val questionStats = mutableMapOf<String, QuestionStat>()
        read(listOf("questionStats", "question_stats", "questions")) { element ->
            (element as? JsonObject)?.forEach { (id, value) ->
                runCatching { json.decodeFromJsonElement(QuestionStat.serializer(), value) }
                    .onSuccess { questionStats[id] = it }
            }
        }

        val ruleStats = mutableMapOf<String, RuleStat>()
        read(listOf("ruleStats", "rule_stats", "rules")) { element ->
            (element as? JsonObject)?.forEach { (id, value) ->
                runCatching { json.decodeFromJsonElement(RuleStat.serializer(), value) }
                    .onSuccess { ruleStats[id] = it }
            }
        }

        val days = mutableMapOf<String, DayStat>()
        read(listOf("days", "daily", "dayStats", "day_stats")) { element ->
            (element as? JsonObject)?.forEach { (day, value) ->
                runCatching { json.decodeFromJsonElement(DayStat.serializer(), value) }
                    .onSuccess { days[day] = it }
            }
        }

        val mistakes = mutableMapOf<String, MistakeEntry>()
        read(listOf("mistakes", "mistakeEntries", "mistake_entries")) { element ->
            (element as? JsonObject)?.forEach { (id, value) ->
                runCatching { json.decodeFromJsonElement(MistakeEntry.serializer(), value) }
                    .onSuccess { mistakes[id] = it }
            }
        }

        val favorites = linkedSetOf<String>()
        read(listOf("favorites", "favourites", "favoriteIds", "favorite_ids")) { element ->
            (element as? JsonArray)?.forEach { item ->
                item.jsonPrimitive.contentOrNull?.let { favorites += it }
            }
        }

        val settings = read(listOf("settings", "preferences")) { element ->
            runCatching { json.decodeFromJsonElement(Settings.serializer(), element) }.getOrNull()
        } ?: Settings()

        val fallbackAnswered = days.values.sumOf { it.answered }
        val fallbackCorrect = days.values.sumOf { it.correct }
        val totalAnswered = int(listOf("totalAnswered", "total_answered")) ?: fallbackAnswered
        val totalCorrect = int(listOf("totalCorrect", "total_correct")) ?: fallbackCorrect

        if (questionStats.isEmpty() && totalAnswered > 0) {
            warnings += "Question-level history was not found; totals and daily history were preserved."
        }

        return ProgressState(
            version = int(listOf("version", "stateVersion", "state_version")) ?: ProgressState.CURRENT_VERSION,
            totalAnswered = totalAnswered,
            totalCorrect = totalCorrect,
            questionStats = questionStats,
            ruleStats = ruleStats,
            days = days,
            favorites = favorites,
            mistakes = mistakes,
            closedMistakes = int(listOf("closedMistakes", "closed_mistakes")) ?: 0,
            bestStreak = int(listOf("bestStreak", "best_streak")) ?: 0,
            settings = settings,
        )
    }

    private fun JsonObject.intValue(key: String): Int? =
        (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.intOrNull

    @Suppress("unused")
    private fun JsonObject.longValue(key: String): Long? = this[key]?.jsonPrimitive?.longOrNull

    @Suppress("unused")
    private fun JsonObject.stringValue(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
}
