package com.nhlstenden.momentum.data.repository

import com.nhlstenden.momentum.data.model.JournalEntry

interface ReflectionRepository {
    suspend fun getRecentReflections(uid: String): List<JournalEntry>
    suspend fun saveReflection(uid: String, entry: JournalEntry)
}

class InMemoryReflectionRepository : ReflectionRepository {
    private val entriesByUser = mutableMapOf<String, List<JournalEntry>>()

    override suspend fun getRecentReflections(uid: String): List<JournalEntry> =
        entriesByUser[uid].orEmpty().sortedByDescending { it.createdAt }

    override suspend fun saveReflection(uid: String, entry: JournalEntry) {
        entriesByUser[uid] = entriesByUser[uid].orEmpty() + entry
    }
}
