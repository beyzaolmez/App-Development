package com.nhlstenden.momentum.data.model

data class QuestState(
    val questStateId: String,
    val questId: String,
    val date: String,
    val status: QuestStatus,
    val isDailyAssigned: Boolean,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val skippedAt: Long? = null
)
