package com.nhlstenden.momentum.data.repository

import com.nhlstenden.momentum.data.model.Quest
import com.nhlstenden.momentum.data.model.QuestCategory
import com.nhlstenden.momentum.data.model.QuestDifficulty
import com.nhlstenden.momentum.data.model.QuestState
import com.nhlstenden.momentum.data.model.QuestStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface QuestRepository {
    fun getQuests(): List<Quest>
    fun getQuestById(id: String): Quest?
    suspend fun getQuestStates(uid: String): List<QuestState>
    suspend fun saveQuestState(uid: String, questState: QuestState)
}

class PredefinedQuestRepository : QuestRepository {
    private val questStatesByUser = mutableMapOf<String, List<QuestState>>()

    private val quests = listOf(
        Quest(
            id = "chapter-focus",
            title = "Read Chapter 4",
            description = "Finish the History 101 reading before tomorrow's seminar.",
            category = QuestCategory.Academic,
            xp = 150,
            difficulty = QuestDifficulty.Medium,
            estimatedMinutes = 25,
            steps = listOf(
                "Open the chapter and remove one distraction.",
                "Read in two 10-minute blocks.",
                "Write down one question for the seminar."
            )
        ),
        Quest(
            id = "mindful-reset",
            title = "15-minute mindful reset",
            description = "Take a short grounding break between classes.",
            category = QuestCategory.Wellbeing,
            xp = 80,
            difficulty = QuestDifficulty.Easy,
            estimatedMinutes = 15,
            steps = listOf(
                "Find a quiet spot.",
                "Breathe slowly for two minutes.",
                "Name one thing that would make today lighter."
            )
        ),
        Quest(
            id = "friend-check-in",
            title = "Message one friend",
            description = "Send one low-pressure check-in to someone you trust.",
            category = QuestCategory.Social,
            xp = 60,
            difficulty = QuestDifficulty.Easy,
            estimatedMinutes = 5,
            steps = listOf(
                "Pick one friend.",
                "Send a short honest message.",
                "No need to keep the conversation going if you are tired."
            )
        ),
        Quest(
            id = "walk-loop",
            title = "Take a campus walk",
            description = "Do one small movement quest to reset your energy.",
            category = QuestCategory.Movement,
            xp = 100,
            difficulty = QuestDifficulty.Easy,
            estimatedMinutes = 10,
            steps = listOf(
                "Choose a short route.",
                "Walk without checking study notifications.",
                "Notice one thing outside your usual routine."
            )
        )
    )

    override fun getQuests(): List<Quest> = quests

    override fun getQuestById(id: String): Quest? = quests.firstOrNull { it.id == id }

    override suspend fun getQuestStates(uid: String): List<QuestState> =
        questStatesByUser[uid].orEmpty()

    override suspend fun saveQuestState(uid: String, questState: QuestState) {
        val existingStates = questStatesByUser[uid].orEmpty()
        val existingState = existingStates.firstOrNull { it.questStateId == questState.questStateId }
        if (existingState != null && existingState.status.isMoreFinalThan(questState.status)) return

        questStatesByUser[uid] = existingStates
            .filterNot { it.questStateId == questState.questStateId }
            .plus(questState)
    }
}

class FirestoreQuestRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : QuestRepository {
    override fun getQuests(): List<Quest> = emptyList()

    override fun getQuestById(id: String): Quest? = null

    suspend fun getRemoteQuests(): List<Quest> {
        val snapshot = firestore
            .collection("quests")
            .whereEqualTo("isActive", true)
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->
            document.toQuest()
        }
    }

    override suspend fun getQuestStates(uid: String): List<QuestState> {
        val snapshot = firestore
            .collection("users")
            .document(uid)
            .collection("questStates")
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->
            document.toQuestState()
        }
    }

    override suspend fun saveQuestState(uid: String, questState: QuestState) {
        val document = firestore
            .collection("users")
            .document(uid)
            .collection("questStates")
            .document(questState.questStateId)

        firestore.runTransaction { transaction ->
            val existingStatus = transaction.get(document).getString("status").toQuestStatus()
            if (!existingStatus.isMoreFinalThan(questState.status)) {
                transaction.set(document, questState.toFirestoreMap())
            }
        }.await()
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toQuest(): Quest? {
    val title = getString("title") ?: return null
    val description = getString("description") ?: return null
    val category = getString("category").toQuestCategory()
    val difficulty = getString("difficulty").toQuestDifficulty()
    val estimatedMinutes = getLong("estimatedMinutes")?.toInt() ?: 5
    val xp = getLong("xp")?.toInt() ?: 0
    val steps = get("steps").toStringList()

    return Quest(
        id = id,
        title = title,
        description = description,
        category = category,
        xp = xp,
        difficulty = difficulty,
        estimatedMinutes = estimatedMinutes,
        steps = steps
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toQuestState(): QuestState? {
    val questId = getString("questId") ?: return null
    val date = getString("date") ?: return null

    return QuestState(
        questStateId = getString("questStateId") ?: id,
        questId = questId,
        date = date,
        status = getString("status").toQuestStatus(),
        isDailyAssigned = getBoolean("isDailyAssigned") ?: false,
        startedAt = getLong("startedAt"),
        completedAt = getLong("completedAt"),
        skippedAt = getLong("skippedAt")
    )
}

private fun QuestState.toFirestoreMap(): Map<String, Any?> = mapOf(
    "questStateId" to questStateId,
    "questId" to questId,
    "date" to date,
    "status" to status.name,
    "isDailyAssigned" to isDailyAssigned,
    "startedAt" to startedAt,
    "completedAt" to completedAt,
    "skippedAt" to skippedAt
)

private fun Any?.toStringList(): List<String> =
    (this as? List<*>)
        ?.mapNotNull { it as? String }
        .orEmpty()

private fun String?.toQuestCategory(): QuestCategory =
    enumValues<QuestCategory>().firstOrNull { it.name.equals(this, ignoreCase = true) }
        ?: QuestCategory.Wellbeing

private fun String?.toQuestDifficulty(): QuestDifficulty =
    enumValues<QuestDifficulty>().firstOrNull { it.name.equals(this, ignoreCase = true) }
        ?: QuestDifficulty.Easy

private fun String?.toQuestStatus(): QuestStatus =
    enumValues<QuestStatus>().firstOrNull { it.name.equals(this, ignoreCase = true) }
        ?: QuestStatus.Available

private fun QuestStatus.isMoreFinalThan(other: QuestStatus): Boolean =
    persistenceRank > other.persistenceRank

private val QuestStatus.persistenceRank: Int
    get() = when (this) {
        QuestStatus.Available -> 0
        QuestStatus.Active -> 1
        QuestStatus.Skipped -> 2
        QuestStatus.Completed -> 3
    }
