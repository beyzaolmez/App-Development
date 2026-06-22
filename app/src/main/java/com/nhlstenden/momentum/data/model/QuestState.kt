package com.nhlstenden.momentum.data.model

data class QuestState(
    val questStateId: String,
    val questId: String,
    val date: String,
    val status: QuestStatus,
    val isDailyAssigned: Boolean,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val skippedAt: Long? = null,
    val currentProgress: Int = 0,
    val targetProgress: Int = 1,
    val progressUnit: String = "completion",
    val lastProgressUpdatedAt: Long? = null
)

/**
 * How "final" a status is when merging two versions of the same quest state
 * (e.g. remote vs. cached). A higher rank wins so a Completed state is never
 * overwritten by a stale Available one. Single source of truth for all merge logic.
 */
val QuestStatus.persistenceRank: Int
    get() = when (this) {
        QuestStatus.Available -> 0
        QuestStatus.Active -> 1
        QuestStatus.Skipped -> 2
        QuestStatus.Completed -> 3
    }
