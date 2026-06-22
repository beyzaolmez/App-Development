package com.nhlstenden.momentum.data.repository

import com.nhlstenden.momentum.data.model.Quest

object QuestSeedExporter {
    fun toJson(quests: List<Quest>): String {
        quests.requireValidQuestContent()

        val documents = quests.map { quest ->
            mapOf(
                "id" to quest.id,
                "data" to quest.toQuestSeedMap()
            )
        }

        return mapOf(
            "collection" to "quests",
            "documents" to documents
        ).toJsonValue()
    }
}

private fun Any?.toJsonValue(): String = when (this) {
    null -> "null"
    is String -> "\"${escapeJson()}\""
    is Number, is Boolean -> toString()
    is Map<*, *> -> entries.joinToString(
        prefix = "{",
        postfix = "}",
        separator = ","
    ) { (key, value) ->
        "\"${key.toString().escapeJson()}\":${value.toJsonValue()}"
    }
    is Iterable<*> -> joinToString(
        prefix = "[",
        postfix = "]",
        separator = ","
    ) { value -> value.toJsonValue() }
    else -> "\"${toString().escapeJson()}\""
}

private fun String.escapeJson(): String =
    buildString {
        this@escapeJson.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (char.code < 0x20) {
                        append("\\u")
                        append(char.code.toString(16).padStart(4, '0'))
                    } else {
                        append(char)
                    }
                }
            }
        }
    }
