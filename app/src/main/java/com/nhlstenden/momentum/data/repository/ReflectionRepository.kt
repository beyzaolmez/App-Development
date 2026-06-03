package com.nhlstenden.momentum.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.nhlstenden.momentum.data.model.JournalEntry
import kotlinx.coroutines.tasks.await

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

class FirestoreReflectionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ReflectionRepository {
    override suspend fun getRecentReflections(uid: String): List<JournalEntry> {
        val snapshot = firestore
            .collection("users")
            .document(uid)
            .collection("journalEntries")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->
            val questId = document.getString("questId") ?: return@mapNotNull null
            JournalEntry(
                journalEntryId = document.getString("journalEntryId") ?: document.id,
                questId = questId,
                promptChoice = document.getString("promptChoice"),
                quickTake = document.getString("quickTake"),
                note = document.getString("note"),
                isPrivate = document.getBoolean("isPrivate") ?: true,
                createdAt = document.getLong("createdAt") ?: 0L
            )
        }
    }

    override suspend fun saveReflection(uid: String, entry: JournalEntry) {
        firestore
            .collection("users")
            .document(uid)
            .collection("journalEntries")
            .document(entry.journalEntryId)
            .set(entry.toFirestoreMap())
            .await()
    }
}

private fun JournalEntry.toFirestoreMap(): Map<String, Any?> = mapOf(
    "journalEntryId" to journalEntryId,
    "questId" to questId,
    "promptChoice" to promptChoice,
    "quickTake" to quickTake,
    "note" to note,
    "isPrivate" to isPrivate,
    "createdAt" to createdAt
)
