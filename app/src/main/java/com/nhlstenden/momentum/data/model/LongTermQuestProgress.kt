package com.nhlstenden.momentum.data.model

object LongTermQuestProgress {
    fun updatedQuest(
        quest: Quest,
        progressDelta: Int,
        now: Long,
        canUpdateToday: Boolean = true
    ): QuestProgressUpdate {
        if (!quest.isLongTerm || quest.status == QuestStatus.Completed || !canUpdateToday) {
            return QuestProgressUpdate(quest = quest, completedNow = false, updatedAt = null)
        }

        val target = quest.targetProgress.coerceAtLeast(1)
        val nextProgress = (quest.currentProgress + progressDelta)
            .coerceIn(0, target)
        val completedNow = quest.currentProgress < target && nextProgress >= target
        val nextStatus = when {
            completedNow -> QuestStatus.Completed
            nextProgress > 0 -> QuestStatus.Active
            else -> quest.status
        }

        return QuestProgressUpdate(
            quest = quest.copy(
                currentProgress = nextProgress,
                status = nextStatus,
                lastProgressUpdatedAt = now
            ),
            completedNow = completedNow,
            updatedAt = now
        )
    }
}

data class QuestProgressUpdate(
    val quest: Quest,
    val completedNow: Boolean,
    val updatedAt: Long?
)
