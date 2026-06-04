package com.nhlstenden.momentum.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.nhlstenden.momentum.data.QuestLocalCache
import com.nhlstenden.momentum.data.model.JournalEntry
import com.nhlstenden.momentum.data.model.Quest
import com.nhlstenden.momentum.data.model.QuestCategory
import com.nhlstenden.momentum.data.model.QuestFeedback
import com.nhlstenden.momentum.data.model.QuestFeedbackRules
import com.nhlstenden.momentum.data.model.QuestFeedbackType
import com.nhlstenden.momentum.data.model.QuestState
import com.nhlstenden.momentum.data.model.QuestStatus
import com.nhlstenden.momentum.data.model.User
import com.nhlstenden.momentum.data.model.UserProgress
import com.nhlstenden.momentum.data.repository.FirestoreReflectionRepository
import com.nhlstenden.momentum.data.repository.FirestoreQuestFeedbackRepository
import com.nhlstenden.momentum.data.repository.FirestoreQuestRepository
import com.nhlstenden.momentum.data.repository.FirestoreUserRepository
import com.nhlstenden.momentum.data.repository.InMemoryReflectionRepository
import com.nhlstenden.momentum.data.repository.PredefinedQuestRepository
import com.nhlstenden.momentum.data.repository.QuestRepository
import com.nhlstenden.momentum.data.repository.ReflectionRepository
import com.nhlstenden.momentum.data.repository.UserRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class QuestViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestoreQuestRepository: FirestoreQuestRepository = FirestoreQuestRepository(),
    private val feedbackRepository: FirestoreQuestFeedbackRepository = FirestoreQuestFeedbackRepository(),
    private val fallbackQuestRepository: QuestRepository = PredefinedQuestRepository(),
    private val userRepository: UserRepository = FirestoreUserRepository(),
    private val reflectionRepository: ReflectionRepository = FirestoreReflectionRepository(),
    private val demoReflectionRepository: ReflectionRepository = InMemoryReflectionRepository()
) : ViewModel() {
    private val dailyQuestLimit = 3
    private val today: String
        get() = dateFormat.format(Date())

    private var localCache: QuestLocalCache? = null
    private var dailyQuestIds by mutableStateOf<Set<String>>(emptySet())
    private var feedbackByQuestId by mutableStateOf<Map<String, QuestFeedbackType>>(emptyMap())
    private var feedbackScoreByCategory by mutableStateOf<Map<QuestCategory, Int>>(emptyMap())
    private var userProgress by mutableStateOf(UserProgress())
    private var reflectedQuestIds by mutableStateOf<Set<String>>(emptySet())

    var isLoading by mutableStateOf(true)
        private set

    var selectedStatus by mutableStateOf<QuestStatus?>(null)
        private set

    var dataMode by mutableStateOf(QuestDataMode.Demo)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var journalErrorByQuestId by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    var recentReflections by mutableStateOf<List<JournalEntry>>(emptyList())
        private set

    private var quests by mutableStateOf<List<Quest>>(emptyList())

    init {
        loadQuests()
    }

    fun attachLocalCache(context: Context) {
        if (localCache != null) return
        localCache = QuestLocalCache(context)
        refresh()
    }

    fun refresh() {
        loadQuests()
    }

    fun visibleQuests(selectedInterests: Set<String> = emptySet()): List<Quest> {
        val filteredQuests = selectedStatus?.let { status ->
            quests.filter { it.status == status }
        } ?: quests

        val interestFilteredQuests = if (selectedInterests.isEmpty()) {
            filteredQuests
        } else {
            filteredQuests.filter { it.category.label in selectedInterests }
        }.ifEmpty { filteredQuests }

        val visibleCandidates = if (selectedInterests.isNotEmpty() && selectedStatus == null) {
            interestFilteredQuests.take(dailyQuestLimit)
        } else {
            interestFilteredQuests.dailyLimited()
        }

        return visibleCandidates.sortedWith(
            compareByDescending<Quest> { it.recommendationScore() }
                .thenBy { it.status.sortOrder }
                .thenBy { it.category.label }
                .thenBy { it.title }
        )
    }

    fun activeQuestCount(): Int = quests.count { it.status == QuestStatus.Active }

    fun completedQuestCount(): Int = quests.count { it.status == QuestStatus.Completed }

    fun totalQuestCount(): Int = quests.size

    fun completedDailyQuestCount(): Int =
        minOf(completedQuestCount(), totalDailyQuestCount())

    fun totalCompletedQuestCount(): Int = userProgress.completedQuestCount

    fun totalDailyQuestCount(): Int = minOf(dailyQuestLimit, quests.size)

    fun dailyQuestLimit(): Int = dailyQuestLimit

    fun currentStreak(): Int = userProgress.currentStreak

    fun completedCategoryCounts(): Map<QuestCategory, Int> =
        userProgress.categoryCounts.mapNotNull { (categoryName, count) ->
            val category = enumValues<QuestCategory>()
                .firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
                ?: return@mapNotNull null
            category to count
        }.toMap()

    fun questById(id: String): Quest? = quests.firstOrNull { it.id == id }

    fun questTitleForReflection(questId: String): String =
        questById(questId)?.title ?: "Quest reflection"

    fun reflectionPromptForQuest(id: String): String = questById(id)?.reflectionPrompt() ?: DEFAULT_REFLECTION_PROMPT

    fun hasReflectionForQuest(id: String): Boolean = id in reflectedQuestIds

    fun feedbackForQuest(id: String): QuestFeedbackType? = feedbackByQuestId[id]

    fun isQuestLiked(id: String): Boolean = feedbackByQuestId[id] == QuestFeedbackType.Like

    fun isQuestDisliked(id: String): Boolean = feedbackByQuestId[id] == QuestFeedbackType.Dislike

    fun selectStatus(status: QuestStatus?) {
        selectedStatus = status
    }

    fun startQuest(id: String) {
        updateQuestStatus(id, QuestStatus.Active)
    }

    fun completeQuest(id: String) {
        if (questById(id)?.status == QuestStatus.Completed) return
        updateQuestStatus(id, QuestStatus.Completed)
    }

    fun completeQuestWithReflection(
        id: String,
        note: String,
        promptChoice: String? = null,
        quickTake: String? = null
    ): Boolean {
        val quest = questById(id) ?: return false
        val trimmedNote = note.trim()
        val reflectionPrompt = quest.reflectionPrompt()

        if (id in reflectedQuestIds) {
            journalErrorByQuestId = journalErrorByQuestId + (id to "Reflection already saved for this quest.")
            return false
        }

        if (trimmedNote.isBlank()) {
            journalErrorByQuestId = journalErrorByQuestId + (id to "Write a short reflection before saving.")
            return false
        }

        journalErrorByQuestId = journalErrorByQuestId - id
        reflectedQuestIds = reflectedQuestIds + id

        val entry = JournalEntry(
            journalEntryId = id,
            questId = id,
            promptChoice = promptChoice ?: reflectionPrompt,
            quickTake = quickTake,
            note = trimmedNote,
            createdAt = System.currentTimeMillis()
        )
        recentReflections = listOf(entry) + recentReflections.filterNot { it.questId == id }

        saveReflectionThenComplete(id = id, entry = entry)
        return true
    }

    fun skipQuest(id: String) {
        updateQuestStatus(id, QuestStatus.Skipped)
    }

    /**
     * Records a positive ("like") reaction for a quest. Duplicate likes are
     * rejected silently: if the quest is already liked, nothing is written again.
     */
    fun likeQuest(id: String) {
        if (QuestFeedbackRules.isDuplicateLike(feedbackByQuestId[id], QuestFeedbackType.Like)) return
        saveFeedback(id, QuestFeedbackType.Like)
    }

    /**
     * Records a negative ("dislike") reaction for a quest. Duplicate dislikes are
     * rejected silently: if the quest is already disliked, nothing is written again.
     */
    fun dislikeQuest(id: String) {
        if (QuestFeedbackRules.isDuplicateDislike(feedbackByQuestId[id], QuestFeedbackType.Dislike)) return
        saveFeedback(id, QuestFeedbackType.Dislike)
    }

    fun saveFeedback(id: String, feedbackType: QuestFeedbackType) {
        val quest = questById(id) ?: return
        val previousFeedbackType = feedbackByQuestId[id]
        feedbackByQuestId = feedbackByQuestId + (id to feedbackType)
        feedbackScoreByCategory = feedbackScoreByCategory.updatedWith(
            category = quest.category,
            previousFeedbackType = previousFeedbackType,
            newFeedbackType = feedbackType
        )
        refreshDailyAssignments()
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            runCatching {
                feedbackRepository.saveFeedback(
                    uid = uid,
                    feedback = QuestFeedback(
                        feedbackId = id,
                        questId = id,
                        category = quest.category,
                        feedbackType = feedbackType,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }.onFailure { error ->
                errorMessage = error.localizedMessage ?: "Could not save quest feedback."
            }
        }
    }

    private fun loadQuests() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val uid = auth.currentUser?.uid
            if (uid == null) {
                loadDemoQuests()
                isLoading = false
                return@launch
            }

            runCatching {
                val remoteQuests = withTimeout(FIRESTORE_TIMEOUT_MS) {
                    firestoreQuestRepository.getRemoteQuests()
                }

                val loadedQuests = if (remoteQuests.isEmpty()) {
                    val predefined = fallbackQuestRepository.getQuests()
                    runCatching {
                        withTimeout(FIRESTORE_TIMEOUT_MS) {
                            firestoreQuestRepository.seedQuests(predefined)
                        }
                    }
                    predefined
                } else {
                    remoteQuests
                }

                val loadedData = coroutineScope {
                    val statesDeferred = async {
                        runCatching {
                            withTimeout(FIRESTORE_TIMEOUT_MS) {
                                firestoreQuestRepository.getQuestStates(uid)
                                    .filter { it.date == today }
                            }
                        }.onSuccess { states ->
                            localCache?.saveQuestStates(uid, states)
                        }.getOrDefault(emptyList()).let { remoteStates ->
                            mergeQuestStates(
                                remoteStates,
                                localCache?.loadQuestStates(uid)
                                    ?.filter { it.date == today }
                                    .orEmpty()
                            )
                        }
                    }
                    val feedbackDeferred = async {
                        runCatching {
                            withTimeout(FIRESTORE_TIMEOUT_MS) {
                                feedbackRepository.getFeedback(uid)
                            }
                        }.getOrDefault(emptyList())
                    }
                    val reflectionsDeferred = async {
                        runCatching {
                            withTimeout(FIRESTORE_TIMEOUT_MS) {
                                reflectionRepository.getRecentReflections(uid)
                            }
                        }.getOrDefault(emptyList())
                    }
                    val userDeferred = async {
                        runCatching {
                            withTimeout(FIRESTORE_TIMEOUT_MS) {
                                userRepository.getUser(uid)
                            }
                        }.onSuccess { user ->
                            user?.progress?.let { localCache?.saveUserProgress(uid, it) }
                        }.getOrNull()
                    }
                    LoadedQuestData(
                        todayStates = statesDeferred.await(),
                        feedback = feedbackDeferred.await(),
                        reflections = reflectionsDeferred.await(),
                        user = userDeferred.await()
                    )
                }

                feedbackByQuestId = loadedData.feedback.associate { it.questId to it.feedbackType }
                feedbackScoreByCategory = loadedData.feedback.toCategoryScores()
                recentReflections = loadedData.reflections
                reflectedQuestIds = loadedData.reflections.map { it.questId }.toSet()
                userProgress = preferredProgress(
                    remoteProgress = loadedData.user?.progress,
                    cachedProgress = localCache?.loadUserProgress(uid)
                )
                quests = loadedQuests.withStates(loadedData.todayStates)
                refreshDailyAssignments()
                dataMode = QuestDataMode.Firestore
                runCatching {
                    withTimeout(FIRESTORE_TIMEOUT_MS) {
                        ensureDailyAssignments(uid)
                    }
                }
            }.onFailure { error ->
                loadDemoQuests()
                errorMessage = error.toQuestDataMessage()
            }

            isLoading = false
        }
    }

    private fun loadDemoQuests() {
        val demoQuests = fallbackQuestRepository.getQuests()
        quests = demoQuests
        refreshDailyAssignments(demoQuests)
        dataMode = QuestDataMode.Demo
        userProgress = UserProgress(currentStreak = if (completedQuestCount() > 0) 1 else 0)
    }

    private fun updateQuestStatus(id: String, status: QuestStatus) {
        val previousStatus = questById(id)?.status
        if (previousStatus == status) return

        quests = quests.map { quest ->
            if (quest.id == id) quest.copy(status = status) else quest
        }

        val quest = questById(id) ?: return
        val uid = auth.currentUser?.uid
        if (uid == null) {
            if (status == QuestStatus.Completed) {
                userProgress = userProgress.copy(currentStreak = maxOf(userProgress.currentStreak, 1))
            }
            return
        }
        val questState = quest.toQuestState(status)
        localCache?.saveQuestState(uid, questState)

        viewModelScope.launch {
            if (status == QuestStatus.Completed && previousStatus != QuestStatus.Completed) {
                updateUserProgress(uid, quest)
            }
            runCatching {
                firestoreQuestRepository.saveQuestState(uid, questState)
            }.onFailure { error ->
                errorMessage = error.toQuestDataMessage()
            }
        }
    }

    private fun saveReflectionThenComplete(id: String, entry: JournalEntry?) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            if (entry != null) {
                viewModelScope.launch {
                    demoReflectionRepository.saveReflection(DEMO_REFLECTION_UID, entry)
                }
            }
            completeQuest(id)
            return
        }

        viewModelScope.launch {
            if (entry != null) {
                runCatching {
                    withTimeout(FIRESTORE_TIMEOUT_MS) {
                        reflectionRepository.saveReflection(uid, entry)
                    }
                }.onFailure {
                    errorMessage = "Quest completed, but the reflection could not sync yet."
                }
            }
            completeQuest(id)
        }
    }

    private suspend fun ensureDailyAssignments(uid: String) {
        dailyQuestIds.forEach { questId ->
            val quest = questById(questId) ?: return@forEach
            val existingStatus = quest.status
            if (existingStatus == QuestStatus.Available) {
                val state = quest.toQuestState(existingStatus)
                localCache?.saveQuestState(uid, state)
                runCatching {
                    firestoreQuestRepository.saveQuestState(uid, state)
                }
            }
        }
    }

    private suspend fun updateUserProgress(uid: String, quest: Quest) {
        val user = runCatching {
            withTimeout(FIRESTORE_TIMEOUT_MS) {
                userRepository.getUser(uid)
            }
        }.getOrNull()
        val currentProgress = user?.progress ?: userProgress
        val newStreak = when (currentProgress.lastQuestCompletionDate) {
            today -> currentProgress.currentStreak
            yesterday() -> currentProgress.currentStreak + 1
            else -> 1
        }
        val categoryCounts = currentProgress.categoryCounts.toMutableMap()
        val categoryKey = quest.category.name
        categoryCounts[categoryKey] = (categoryCounts[categoryKey] ?: 0) + 1

        val updatedProgress = UserProgress(
            currentStreak = newStreak,
            completedQuestCount = currentProgress.completedQuestCount + 1,
            skippedQuestCount = currentProgress.skippedQuestCount,
            categoryCounts = categoryCounts,
            lastQuestCompletionDate = today
        )
        userProgress = updatedProgress
        localCache?.saveUserProgress(uid, updatedProgress)
        runCatching {
            withTimeout(FIRESTORE_TIMEOUT_MS) {
                if (user == null) {
                    val firebaseUser = auth.currentUser
                    userRepository.saveUser(
                        User(
                            uid = uid,
                            displayName = firebaseUser?.displayName.orEmpty(),
                            email = firebaseUser?.email.orEmpty(),
                            progress = updatedProgress
                        )
                    )
                } else {
                    userRepository.updateProgress(uid = uid, progress = updatedProgress)
                }
            }
        }.onFailure {
            errorMessage = "Progress updated locally, but could not sync to Firestore yet."
        }
    }

    private fun List<Quest>.withStates(states: List<QuestState>): List<Quest> {
        val stateByQuestId = states.associateBy { it.questId }
        return map { quest ->
            val state = stateByQuestId[quest.id]
            if (state == null) quest else quest.copy(status = state.status)
        }
    }

    private fun Quest.toQuestState(status: QuestStatus): QuestState {
        val now = System.currentTimeMillis()
        return QuestState(
            questStateId = "$today-$id",
            questId = id,
            date = today,
            status = status,
            isDailyAssigned = id in dailyQuestIds,
            startedAt = if (status == QuestStatus.Active) now else null,
            completedAt = if (status == QuestStatus.Completed) now else null,
            skippedAt = if (status == QuestStatus.Skipped) now else null
        )
    }

    private fun List<Quest>.dailyLimited(): List<Quest> {
        if (selectedStatus != null && selectedStatus != QuestStatus.Available) return this

        return filter { quest ->
            quest.status != QuestStatus.Available || quest.id in dailyQuestIds
        }
    }

    private fun List<Quest>.prioritizedByFeedback(): List<Quest> =
        sortedWith(
            compareByDescending<Quest> { it.recommendationScore() }
                .thenBy { it.status.sortOrder }
                .thenBy { it.category.label }
                .thenBy { it.title }
        )

    private fun Quest.recommendationScore(): Int =
        ((feedbackByQuestId[id]?.preferenceScore ?: 0) * DIRECT_FEEDBACK_WEIGHT) +
            (feedbackScoreByCategory[category] ?: 0)

    private fun refreshDailyAssignments(sourceQuests: List<Quest> = quests) {
        dailyQuestIds = sourceQuests
            .prioritizedByFeedback()
            .take(dailyQuestLimit)
            .map { it.id }
            .toSet()
    }
}

private data class LoadedQuestData(
    val todayStates: List<QuestState>,
    val feedback: List<QuestFeedback>,
    val reflections: List<JournalEntry>,
    val user: User?
)

enum class QuestDataMode {
    Firestore,
    Demo
}

private const val FIRESTORE_TIMEOUT_MS = 8_000L
private const val DIRECT_FEEDBACK_WEIGHT = 100
private const val DEMO_REFLECTION_UID = "demo-reflections"
private const val DEFAULT_REFLECTION_PROMPT = "What did you notice, learn, or want to do differently next time?"

private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

private fun yesterday(): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    return dateFormat.format(calendar.time)
}

private val QuestStatus.sortOrder: Int
    get() = when (this) {
        QuestStatus.Active -> 0
        QuestStatus.Available -> 1
        QuestStatus.Completed -> 2
        QuestStatus.Skipped -> 3
    }

private fun mergeQuestStates(remoteStates: List<QuestState>, cachedStates: List<QuestState>): List<QuestState> =
    (remoteStates + cachedStates)
        .groupBy { it.questStateId }
        .map { (_, versions) -> versions.maxBy { it.status.persistenceRank } }

private fun preferredProgress(
    remoteProgress: UserProgress?,
    cachedProgress: UserProgress?
): UserProgress =
    listOfNotNull(remoteProgress, cachedProgress, UserProgress())
        .maxWith(
            compareBy<UserProgress> { it.completedQuestCount }
                .thenBy { it.currentStreak }
        )

private val QuestStatus.persistenceRank: Int
    get() = when (this) {
        QuestStatus.Available -> 0
        QuestStatus.Active -> 1
        QuestStatus.Skipped -> 2
        QuestStatus.Completed -> 3
    }

private fun Throwable.toQuestDataMessage(): String {
    val diagnosticText = localizedMessage.orEmpty().uppercase()
    return when {
        "PERMISSION_DENIED" in diagnosticText || "CLOUD FIRESTORE API" in diagnosticText ->
            "Firestore is not enabled for this Firebase project yet. Showing demo quests until the database is enabled."
        else -> localizedMessage ?: "Could not sync quests right now. Showing demo quests."
    }
}

private fun List<QuestFeedback>.toCategoryScores(): Map<QuestCategory, Int> =
    groupBy { it.category }
        .mapValues { (_, feedback) -> feedback.sumOf { it.feedbackType.preferenceScore } }
        .filterValues { it != 0 }

private fun Map<QuestCategory, Int>.updatedWith(
    category: QuestCategory,
    previousFeedbackType: QuestFeedbackType?,
    newFeedbackType: QuestFeedbackType
): Map<QuestCategory, Int> {
    val updatedScore = (this[category] ?: 0) -
        (previousFeedbackType?.preferenceScore ?: 0) +
        newFeedbackType.preferenceScore

    return if (updatedScore == 0) {
        this - category
    } else {
        this + (category to updatedScore)
    }
}

private val QuestFeedbackType.preferenceScore: Int
    get() = when (this) {
        QuestFeedbackType.Like -> 2
        QuestFeedbackType.MoreLikeThis -> 1
        QuestFeedbackType.NotForMe -> -1
        QuestFeedbackType.Dislike -> -2
    }

private fun Quest.reflectionPrompt(): String = journalPrompt ?: when (category) {
    QuestCategory.Academic -> "What helped your learning, and what could you improve next time?"
    QuestCategory.Focus -> "What made it easier or harder to stay focused?"
    QuestCategory.Wellbeing -> "How did this affect how you feel right now?"
    QuestCategory.Social -> "What did you notice about the interaction?"
    QuestCategory.Movement -> "How did your body or energy feel afterward?"
}
