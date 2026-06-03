package com.nhlstenden.momentum.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.nhlstenden.momentum.data.model.Quest
import com.nhlstenden.momentum.data.model.QuestCategory
import com.nhlstenden.momentum.data.model.QuestFeedback
import com.nhlstenden.momentum.data.model.QuestFeedbackRules
import com.nhlstenden.momentum.data.model.QuestFeedbackType
import com.nhlstenden.momentum.data.model.QuestState
import com.nhlstenden.momentum.data.model.QuestStatus
import com.nhlstenden.momentum.data.model.User
import com.nhlstenden.momentum.data.model.UserProgress
import com.nhlstenden.momentum.data.repository.FirestoreQuestFeedbackRepository
import com.nhlstenden.momentum.data.repository.FirestoreQuestRepository
import com.nhlstenden.momentum.data.repository.FirestoreUserRepository
import com.nhlstenden.momentum.data.repository.PredefinedQuestRepository
import com.nhlstenden.momentum.data.repository.QuestRepository
import com.nhlstenden.momentum.data.repository.UserRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class QuestViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestoreQuestRepository: FirestoreQuestRepository = FirestoreQuestRepository(),
    private val feedbackRepository: FirestoreQuestFeedbackRepository = FirestoreQuestFeedbackRepository(),
    private val fallbackQuestRepository: QuestRepository = PredefinedQuestRepository(),
    private val userRepository: UserRepository = FirestoreUserRepository()
) : ViewModel() {
    private val dailyQuestLimit = 3
    private val today: String
        get() = dateFormat.format(Date())

    private var dailyQuestIds by mutableStateOf<Set<String>>(emptySet())
    private var feedbackByQuestId by mutableStateOf<Map<String, QuestFeedbackType>>(emptyMap())
    private var userProgress by mutableStateOf(UserProgress())

    var isLoading by mutableStateOf(true)
        private set

    var selectedStatus by mutableStateOf<QuestStatus?>(null)
        private set

    var dataMode by mutableStateOf(QuestDataMode.Demo)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var quests by mutableStateOf<List<Quest>>(emptyList())

    init {
        loadQuests()
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
            compareBy<Quest> { it.status.sortOrder }
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
        feedbackByQuestId = feedbackByQuestId + (id to feedbackType)
        val uid = auth.currentUser?.uid ?: return
        val quest = questById(id) ?: return

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
                val loadedQuests = remoteQuests.ifEmpty {
                    errorMessage = "No Firestore quests found yet. Showing demo quests."
                    fallbackQuestRepository.getQuests()
                }
                val todayStates = withTimeout(FIRESTORE_TIMEOUT_MS) {
                    firestoreQuestRepository.getQuestStates(uid)
                        .filter { it.date == today }
                }
                feedbackByQuestId = withTimeout(FIRESTORE_TIMEOUT_MS) {
                    feedbackRepository.getFeedback(uid)
                        .associate { it.questId to it.feedbackType }
                }
                userProgress = withTimeout(FIRESTORE_TIMEOUT_MS) {
                    userRepository.getUser(uid)?.progress ?: UserProgress()
                }

                quests = loadedQuests.withStates(todayStates)
                dailyQuestIds = todayStates
                    .filter { it.isDailyAssigned }
                    .map { it.questId }
                    .toSet()
                    .ifEmpty { loadedQuests.take(dailyQuestLimit).map { it.id }.toSet() }
                dataMode = if (remoteQuests.isEmpty()) QuestDataMode.Demo else QuestDataMode.Firestore
                withTimeout(FIRESTORE_TIMEOUT_MS) {
                    ensureDailyAssignments(uid)
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
        dailyQuestIds = demoQuests.take(dailyQuestLimit).map { it.id }.toSet()
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

    private suspend fun ensureDailyAssignments(uid: String) {
        dailyQuestIds.forEach { questId ->
            val quest = questById(questId) ?: return@forEach
            val existingStatus = quest.status
            if (existingStatus == QuestStatus.Available) {
                firestoreQuestRepository.saveQuestState(uid, quest.toQuestState(existingStatus))
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
}

enum class QuestDataMode {
    Firestore,
    Demo
}

private const val FIRESTORE_TIMEOUT_MS = 8_000L

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

private fun Throwable.toQuestDataMessage(): String {
    val diagnosticText = localizedMessage.orEmpty().uppercase()
    return when {
        "PERMISSION_DENIED" in diagnosticText || "CLOUD FIRESTORE API" in diagnosticText ->
            "Firestore is not enabled for this Firebase project yet. Showing demo quests until the database is enabled."
        else -> localizedMessage ?: "Could not sync quests right now. Showing demo quests."
    }
}
