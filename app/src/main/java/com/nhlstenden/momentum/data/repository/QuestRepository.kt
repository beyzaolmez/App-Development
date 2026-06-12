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
        // ── Academic ──────────────────────────────────────────────────────────
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
            id = "lecture-summary",
            title = "Summarise one lecture",
            description = "Convert your raw notes into a short structured summary.",
            category = QuestCategory.Academic,
            xp = 100,
            difficulty = QuestDifficulty.Easy,
            estimatedMinutes = 20,
            steps = listOf(
                "Open your notes from the most recent lecture.",
                "Identify the three main points.",
                "Write a 5-sentence summary in your own words."
            )
        ),
        Quest(
            id = "deadline-plan",
            title = "Map out this week's deadlines",
            description = "Get every upcoming task out of your head and into a plan.",
            category = QuestCategory.Academic,
            xp = 120,
            difficulty = QuestDifficulty.Medium,
            estimatedMinutes = 15,
            steps = listOf(
                "Open your calendar or planner.",
                "List every deadline for the next 7 days.",
                "Assign one work slot to the most urgent item."
            )
        ),
        Quest(
            id = "essay-outline",
            title = "Draft an essay outline",
            description = "Build the skeleton before you write a single sentence.",
            category = QuestCategory.Academic,
            xp = 180,
            difficulty = QuestDifficulty.Hard,
            estimatedMinutes = 40,
            steps = listOf(
                "State your thesis in one sentence.",
                "List three supporting arguments with evidence.",
                "Write a one-line topic sentence for each paragraph."
            )
        ),
        // ── Focus ─────────────────────────────────────────────────────────────
        Quest(
            id = "deep-work-block",
            title = "25-minute deep work block",
            description = "One task, zero interruptions, full attention.",
            category = QuestCategory.Focus,
            xp = 120,
            difficulty = QuestDifficulty.Easy,
            estimatedMinutes = 25,
            steps = listOf(
                "Choose exactly one task to work on.",
                "Put your phone face-down and close unrelated tabs.",
                "Work until the 25 minutes are up, then take a 5-minute break."
            )
        ),
        Quest(
            id = "phone-free-hour",
            title = "Phone-free study hour",
            description = "Put your phone in another room and see what your brain can do.",
            category = QuestCategory.Focus,
            xp = 100,
            difficulty = QuestDifficulty.Easy,
            estimatedMinutes = 60,
            steps = listOf(
                "Place your phone in a different room.",
                "Set a timer for 60 minutes.",
                "Notice how your focus changes without the pull of notifications."
            )
        ),
        Quest(
            id = "desk-reset",
            title = "Clear your workspace before starting",
            description = "A tidy desk signals to your brain that it's time to work.",
            category = QuestCategory.Focus,
            xp = 60,
            difficulty = QuestDifficulty.Easy,
            estimatedMinutes = 10,
            steps = listOf(
                "Remove everything from your desk that isn't for today's task.",
                "Wipe the surface and open only the tabs you need.",
                "Start the work within two minutes of finishing the clear-out."
            )
        ),
        Quest(
            id = "weekly-review",
            title = "Weekly priority review",
            description = "Close the week by capturing loose ends and setting three priorities for next week.",
            category = QuestCategory.Focus,
            xp = 200,
            difficulty = QuestDifficulty.Hard,
            estimatedMinutes = 45,
            steps = listOf(
                "List everything still open from this week.",
                "Mark the three most important items for next week.",
                "Schedule a specific time slot for each of the three."
            )
        ),
        // ── Wellbeing ─────────────────────────────────────────────────────────
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
            id = "gratitude-note",
            title = "Write a gratitude note",
            description = "Three specific things you're glad happened today or this week.",
            category = QuestCategory.Wellbeing,
            xp = 60,
            difficulty = QuestDifficulty.Easy,
            estimatedMinutes = 10,
            steps = listOf(
                "Open a notebook or notes app.",
                "Write three things you're grateful for — be specific.",
                "Read them back once before closing."
            ),
            journalPrompt = "Write three specific things you are grateful for today."
        ),
        Quest(
            id = "body-scan",
            title = "5-minute body scan",
            description = "Check in with where you're holding tension and consciously release it.",
            category = QuestCategory.Wellbeing,
            xp = 50,
            difficulty = QuestDifficulty.Easy,
            estimatedMinutes = 5,
            steps = listOf(
                "Sit or lie down comfortably.",
                "Slowly scan from your feet to your head, noticing tension.",
                "Breathe into each tense area and let it soften."
            )
        ),
        Quest(
            id = "wind-down",
            title = "Evening wind-down routine",
            description = "Signal to your nervous system that the day is done.",
            category = QuestCategory.Wellbeing,
            xp = 110,
            difficulty = QuestDifficulty.Medium,
            estimatedMinutes = 30,
            steps = listOf(
                "Dim your lights or switch to warm lighting 30 minutes before bed.",
                "Put your phone on Do Not Disturb.",
                "Do one calming activity: read, stretch, or journal."
            ),
            journalPrompt = "What helped you slow down tonight?"
        ),
        // ── Social ────────────────────────────────────────────────────────────
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
            id = "voice-note",
            title = "Send a voice note",
            description = "Skip the typing and send a 60-second voice message instead.",
            category = QuestCategory.Social,
            xp = 60,
            difficulty = QuestDifficulty.Easy,
            estimatedMinutes = 5,
            steps = listOf(
                "Choose someone you've been meaning to catch up with.",
                "Record a short voice message — no script needed.",
                "Send it without overthinking."
            )
        ),
        Quest(
            id = "classmate-outreach",
            title = "Reach out to a classmate",
            description = "Share notes, ask a question, or suggest a study session.",
            category = QuestCategory.Social,
            xp = 90,
            difficulty = QuestDifficulty.Medium,
            estimatedMinutes = 10,
            steps = listOf(
                "Pick a classmate you haven't spoken to this week.",
                "Share something useful: a summary, a question, or a resource.",
                "See if they want to study together."
            )
        ),
        Quest(
            id = "honest-convo",
            title = "Have one honest conversation",
            description = "Say something real instead of something safe.",
            category = QuestCategory.Social,
            xp = 130,
            difficulty = QuestDifficulty.Medium,
            estimatedMinutes = 20,
            steps = listOf(
                "Think of something you've been holding back from saying.",
                "Choose the right moment and the right person.",
                "Say it clearly and listen to the response."
            )
        ),
        // ── Movement ──────────────────────────────────────────────────────────
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
            ),
            journalPrompt = "What did you notice during the walk?"
        ),
        Quest(
            id = "stretch-break",
            title = "10-minute full-body stretch",
            description = "Undo the damage of sitting by moving every major muscle group.",
            category = QuestCategory.Movement,
            xp = 70,
            difficulty = QuestDifficulty.Easy,
            estimatedMinutes = 10,
            steps = listOf(
                "Stand up from your desk.",
                "Stretch your neck, shoulders, back, and hips for 2 minutes each.",
                "Finish with three slow deep breaths."
            )
        ),
        Quest(
            id = "stairs-day",
            title = "Stairs only — all day",
            description = "Every lift you skip is a small win for your heart.",
            category = QuestCategory.Movement,
            xp = 80,
            difficulty = QuestDifficulty.Easy,
            estimatedMinutes = 0,
            steps = listOf(
                "Commit to using stairs instead of lifts or escalators today.",
                "Notice how you feel by the end of the day.",
                "Count it as a win even if you only manage it for part of the day."
            )
        ),
        Quest(
            id = "workout-session",
            title = "30-minute workout",
            description = "A full session — whatever form of movement works for you.",
            category = QuestCategory.Movement,
            xp = 200,
            difficulty = QuestDifficulty.Hard,
            estimatedMinutes = 30,
            steps = listOf(
                "Choose your activity: gym, run, home workout, cycle, or swim.",
                "Warm up for 5 minutes before you push hard.",
                "Cool down and stretch afterwards."
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

    suspend fun seedQuests(quests: List<Quest>) {
        val batch = firestore.batch()
        quests.forEach { quest ->
            val doc = firestore.collection("quests").document(quest.id)
            batch.set(doc, quest.toQuestFirestoreMap())
        }
        batch.commit().await()
    }

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

        document.set(questState.toFirestoreMap()).await()
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
    val journalPrompt = getString("journalPrompt")?.takeIf { it.isNotBlank() } ?: defaultJournalPrompt(id)

    return Quest(
        id = id,
        title = title,
        description = description,
        category = category,
        xp = xp,
        difficulty = difficulty,
        estimatedMinutes = estimatedMinutes,
        steps = steps,
        journalPrompt = journalPrompt
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

private fun Quest.toQuestFirestoreMap(): Map<String, Any?> = mapOf(
    "title" to title,
    "description" to description,
    "category" to category.name,
    "difficulty" to difficulty.name,
    "estimatedMinutes" to estimatedMinutes,
    "xp" to xp,
    "steps" to steps,
    "journalPrompt" to journalPrompt,
    "isActive" to true
)

private fun defaultJournalPrompt(questId: String): String? = when (questId) {
    "gratitude-note" -> "Write three specific things you are grateful for today."
    "wind-down" -> "What helped you slow down tonight?"
    "walk-loop" -> "What did you notice during the walk?"
    else -> null
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
