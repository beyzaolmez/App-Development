package com.nhlstenden.momentum.data.repository

import com.nhlstenden.momentum.data.model.Quest
import com.nhlstenden.momentum.data.model.QuestCategory
import com.nhlstenden.momentum.data.model.QuestGoalType

object QuestContentValidator {
    private val requiredWorkoutQuestIds = setOf(
        "workout-session",
        "bodyweight-circuit",
        "cardio-intervals",
        "core-reset",
        "workout-streak"
    )

    fun validateAll(quests: List<Quest>): List<String> {
        val issues = mutableListOf<String>()

        if (quests.isEmpty()) {
            issues += "Quest list must not be empty."
            return issues
        }

        val duplicateIds = quests
            .groupBy { it.id }
            .filterValues { it.size > 1 }
            .keys
        duplicateIds.forEach { id -> issues += "Quest id '$id' is duplicated." }

        quests.forEach { quest ->
            issues += validate(quest)
        }

        val existingCategories = quests.map { it.category }.toSet()
        QuestCategory.entries.forEach { category ->
            if (category !in existingCategories) {
                issues += "Category '${category.name}' has no quest content."
            }
        }

        val questIds = quests.map { it.id }.toSet()
        requiredWorkoutQuestIds.forEach { questId ->
            if (questId !in questIds) {
                issues += "Required workout quest '$questId' is missing."
            }
        }

        quests
            .filter { it.id in requiredWorkoutQuestIds }
            .filterNot { it.category == QuestCategory.Movement }
            .forEach { quest ->
                issues += "Workout quest '${quest.id}' must be in the Movement category."
            }

        if (quests.none { it.goalType == QuestGoalType.LongTerm }) {
            issues += "At least one long-term quest is required."
        }

        return issues
    }

    fun validate(quest: Quest): List<String> {
        val issues = mutableListOf<String>()
        val prefix = "Quest '${quest.id.ifBlank { "<blank>" }}'"

        if (quest.id.isBlank()) issues += "$prefix must have a non-blank id."
        if (quest.title.isBlank()) issues += "$prefix must have a non-blank title."
        if (quest.description.isBlank()) issues += "$prefix must have a non-blank description."
        if (quest.xp <= 0) issues += "$prefix must award positive XP."
        if (quest.estimatedMinutes < 0) issues += "$prefix must not have negative estimated minutes."
        if (quest.steps.isEmpty()) issues += "$prefix must include at least one step."
        if (quest.steps.any { it.isBlank() }) issues += "$prefix must not include blank steps."

        when (quest.goalType) {
            QuestGoalType.Daily -> {
                if (quest.targetProgress < 1) issues += "$prefix must have targetProgress of at least 1."
                if (quest.progressUnit.isBlank()) issues += "$prefix must have a progressUnit."
            }
            QuestGoalType.LongTerm -> {
                if (quest.targetProgress <= 1) issues += "$prefix must have long-term targetProgress greater than 1."
                if (quest.progressUnit.isBlank() || quest.progressUnit == "completion") {
                    issues += "$prefix must have a specific long-term progressUnit."
                }
            }
        }

        return issues
    }
}

internal fun List<Quest>.requireValidQuestContent() {
    val issues = QuestContentValidator.validateAll(this)
    require(issues.isEmpty()) {
        "Quest content validation failed:\n" + issues.joinToString(separator = "\n") { "- $it" }
    }
}
