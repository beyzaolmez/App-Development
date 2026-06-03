package com.nhlstenden.momentum.data.model

data class JournalEntry(
    val journalEntryId: String,
    val questId: String,
    val promptChoice: String? = null,
    val quickTake: String? = null,
    val note: String? = null,
    val isPrivate: Boolean = true,
    val createdAt: Long
)
