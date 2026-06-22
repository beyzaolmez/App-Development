package com.nhlstenden.momentum.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.nhlstenden.momentum.data.QuestLocalCache
import com.nhlstenden.momentum.data.model.JournalEntry
import com.nhlstenden.momentum.data.model.LongTermQuestProgress
import com.nhlstenden.momentum.data.model.Quest
import com.nhlstenden.momentum.data.model.QuestCategory
import com.nhlstenden.momentum.data.model.QuestFeedback
import com.nhlstenden.momentum.data.model.QuestFeedbackRules
import com.nhlstenden.momentum.data.model.QuestFeedbackType
import com.nhlstenden.momentum.data.model.ProgressOverview
import com.nhlstenden.momentum.data.model.ProgressOverviewCalculator
import com.nhlstenden.momentum.data.model.QuestState
import com.nhlstenden.momentum.data.model.QuestStatus
import com.nhlstenden.momentum.data.model.persistenceRank
import com.nhlstenden.momentum.data.model.User
import com.nhlstenden.momentum.data.model.UserProgress
import com.nhlstenden.momentum.data.repository.FirestoreReflectionRepository
import com.nhlstenden.momentum.data.repository.FirestoreQuestFeedbackRepository
import com.nhlstenden.momentum.data.repository.FirestoreQuestRepository
import com.nhlstenden.momentum.data.repository.FirestoreUserRepository
import com.nhlstenden.momentum.data.repository.InMemoryReflectionRepository
import com.nhlstenden.momentum.data.repository.PredefinedQuestRepository
import com.nhlstenden.momentum.data.repository.QuestCatalog
import com.nhlstenden.momentum.data.repository.ReflectionRepository
import com.nhlstenden.momentum.data.repository.FirestoreSharedStreakRepository
import com.nhlstenden.momentum.data.repository.SharedStreakRepository
import com.nhlstenden.momentum.data.repository.UserRepository
import com.nhlstenden.momentum.util.FriendlyErrorMessages
import com.nhlstenden.momentum.util.toFriendlyQuestDataMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class QuestViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestoreQuestRepository: FirestoreQuestRepository = FirestoreQuestRepository(),
    private val feedbackRepository: FirestoreQuestFeedbackRepository = FirestoreQuestFeedbackRepository(),
    private val fallbackQuestRepository: QuestCatalog = PredefinedQuestRepository(),
    private val userRepository: UserRepository = FirestoreUserRepository(),
    private val reflectionRepository: ReflectionRepository = FirestoreReflectionRepository(),
    private val demoReflectionRepository: ReflectionRepository = InMemoryReflectionRepository(),
    private val sharedStreakRepository: SharedStreakRepository = FirestoreSharedStreakRepository()
) : ViewModel() {
    private val dailyQuestLimit = 3
    private val today: String
        get() = utcDateFormat().format(Calendar.getInstance(TimeZone.getTimeZone("UTC")).time)

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

    fun visibleQuests(selectedInterests: Set<String> = emptySet()): List<Quest> =
        DailyQuestSelector.visibleQuests(
            quests = quests,
            selectedStatus = selectedStatus,
            selectedInterests = selectedInterests,
            dailyQuestLimit = dailyQuestLimit,
            dailyQuestIds = dailyQuestIds,
            feedbackByQuestId = feedbackByQuestId,
            feedbackScoreByCategory = feedbackScoreByCategory
        )

    fun activeQuestCount(): Int = quests.count { it.status == QuestStatus.Active }

    fun completedQuestCount(): Int = quests.count { it.status == QuestStatus.Completed }

    fun totalQuestCount(): Int = quests.size

    fun completedDailyQuestCount(): Int =
        minOf(quests.count { !it.isLongTerm && it.status == QuestStatus.Completed }, totalDailyQuestCount())

    fun totalCompletedQuestCount(): Int = userProgress.completedQuestCount

    fun totalDailyQuestCount(): Int = minOf(dailyQuestLimit, quests.count { !it.isLongTerm })
    fun activeLongTermQuestCount(): Int =
        quests.count { it.isLongTerm && it.status == QuestStatus.Active }

    fun dailyQuestLimit(): Int = dailyQuestLimit

    fun currentStreak(): Int = userProgress.currentStreak

    fun completedCategoryCounts(): Map<QuestCategory, Int> =
        userProgress.categoryCounts.mapNotNull { (categoryName, count) ->
            val category = enumValues<QuestCategory>()
                .firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
                ?: return@mapNotNull null
            category to count
        }.toMap()

    fun progressOverview(): ProgressOverview =
        ProgressOverviewCalculator.build(
            quests = quests,
            progress = userProgress,
            dailyQuestLimit = dailyQuestLimit
        )

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

    fun canLogLongTermProgress(id: String): Boolean {
        val quest = questById(id) ?: return false
        return quest.isLongTerm &&
            quest.status == QuestStatus.Active &&
            !quest.wasProgressUpdatedToday()
    }

    fun updateQuestProgress(id: String, progressDelta: Int = 1): Boolean {
        val currentQuest = questById(id) ?: return false
        val now = System.currentTimeMillis()
        val canUpdateToday = !currentQuest.wasProgressUpdatedToday(now)
        val update = LongTermQuestProgress.updatedQuest(
            quest = currentQuest,
            progressDelta = progressDelta,
            now = now,
            canUpdateToday = canUpdateToday
        )
        if (update.quest == currentQuest) return false

        quests = quests.map { quest ->
            if (quest.id == id) update.quest else quest
        }

        val uid = auth.currentUser?.uid
        val questState = update.quest.toQuestState(update.quest.status, lastProgressUpdatedAt = update.updatedAt)
        if (uid == null) {
            updateDemoActivityProgress(update.quest, countCompletedQuest = update.completedNow)
            return update.completedNow
        }

        localCache?.saveQuestState(uid, questState)
        viewModelScope.launch {
            updateUserProgress(uid, update.quest, countCompletedQuest = update.completedNow)
            runCatching {
                firestoreQuestRepository.saveQuestState(uid, questState)
            }.onFailure { error ->
                errorMessage = error.toFriendlyQuestDataMessage()
            }
        }
        return update.completedNow
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
        if (QuestFeedbackRules.isDuplicateLike(feedbackByQuestId[id], QuestFeedbackType.Like)) {
            publishSharedQuestLike(id)
            return
        }
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
            }.onFailure {
                errorMessage = FriendlyErrorMessages.questFeedbackSave()
            }

            if (feedbackType == QuestFeedbackType.Like) {
                runCatching {
                    withTimeout(FIRESTORE_TIMEOUT_MS) {
                        sharedStreakRepository.recordQuestLike(uid, id)
                    }
                }.onFailure { Log.w(TAG, "saveFeedback: failed to mirror like to shared streaks", it) }
            }
        }
    }

    private fun publishSharedQuestLike(id: String) {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            runCatching {
                withTimeout(FIRESTORE_TIMEOUT_MS) {
                    sharedStreakRepository.recordQuestLike(uid, id)
                }
            }.onFailure { Log.w(TAG, "publishSharedQuestLike: failed to mirror like", it) }
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

                val predefined = fallbackQuestRepository.getQuests()
                val loadedQuests = if (remoteQuests.isEmpty()) {
                    runCatching {
                        withTimeout(FIRESTORE_TIMEOUT_MS) {
                            firestoreQuestRepository.seedQuests(predefined)
                        }
                    }
                    predefined
                } else {
                    val missingPredefinedQuests = predefined.filterNot { predefinedQuest ->
                        remoteQuests.any { it.id == predefinedQuest.id }
                    }
                    if (missingPredefinedQuests.isNotEmpty()) {
                        runCatching {
                            withTimeout(FIRESTORE_TIMEOUT_MS) {
                                firestoreQuestRepository.seedQuests(missingPredefinedQuests)
                            }
                        }
                    }
                    remoteQuests + missingPredefinedQuests
                }

                val loadedData = coroutineScope {
                    val statesDeferred = async {
                        runCatching {
                            withTimeout(FIRESTORE_TIMEOUT_MS) {
                                    firestoreQuestRepository.getQuestStates(uid)
                                        .filterForCurrentQuests(loadedQuests)
                            }
                        }.onSuccess { states ->
                            localCache?.saveQuestStates(uid, states)
                        }.getOrDefault(emptyList()).let { remoteStates ->
                            mergeQuestStates(
                                remoteStates,
                                localCache?.loadQuestStates(uid)
                                    ?.filterForCurrentQuests(loadedQuests)
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

                loadedQuests to loadedData
            }.onSuccess { (loadedQuests, loadedData) ->
                // State commit lives in onSuccess so a failure here can never fall
                // through to the demo fallback and clobber the user's real data.
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
                errorMessage = error.toFriendlyQuestDataMessage()
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
            if (quest.id == id) {
                quest.copy(
                    status = status,
                    currentProgress = if (status == QuestStatus.Completed && quest.isLongTerm) {
                        quest.targetProgress.coerceAtLeast(1)
                    } else {
                        quest.currentProgress
                    }
                )
            } else {
                quest
            }
        }

        val quest = questById(id) ?: return
        val uid = auth.currentUser?.uid
        if (uid == null) {
            if (status == QuestStatus.Completed) {
                updateDemoActivityProgress(quest)
            } else if (status == QuestStatus.Skipped && previousStatus != QuestStatus.Skipped) {
                userProgress = userProgress.copy(skippedQuestCount = userProgress.skippedQuestCount + 1)
            }
            return
        }
        val questState = quest.toQuestState(status)
        localCache?.saveQuestState(uid, questState)

        viewModelScope.launch {
            if (status == QuestStatus.Completed && previousStatus != QuestStatus.Completed) {
                updateUserProgress(uid, quest)
            } else if (status == QuestStatus.Skipped && previousStatus != QuestStatus.Skipped) {
                updateSkippedProgress(uid)
            }
            runCatching {
                firestoreQuestRepository.saveQuestState(uid, questState)
            }.onFailure { error ->
                errorMessage = error.toFriendlyQuestDataMessage()
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
                    // Roll back the optimistic markers so the user can retry the
                    // reflection instead of being permanently blocked by the
                    // duplicate guard with no entry actually persisted.
                    reflectedQuestIds = reflectedQuestIds - id
                    recentReflections = recentReflections.filterNot { it.questId == id }
                    errorMessage = "Quest completed, but we couldn't save your reflection. Please try again."
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

    private suspend fun updateUserProgress(
        uid: String,
        quest: Quest,
        countCompletedQuest: Boolean = true
    ) {
        val categoryKey = quest.category.name

        fun applyCompletion(current: UserProgress): UserProgress {
            val newStreak = when (current.lastQuestCompletionDate) {
                today -> current.currentStreak
                yesterday() -> current.currentStreak + 1
                else -> 1
            }
            val categoryCounts = current.categoryCounts.toMutableMap()
            categoryCounts[categoryKey] = (categoryCounts[categoryKey] ?: 0) + 1
            return current.copy(
                currentStreak = newStreak,
                completedQuestCount = current.completedQuestCount + if (countCompletedQuest) 1 else 0,
                categoryCounts = categoryCounts,
                lastQuestCompletionDate = today
            )
        }

        // Atomic read-modify-write so two quests completed in quick succession can't
        // clobber each other's increment. Falls back to a local-only update when offline.
        val updatedProgress = runCatching {
            withTimeout(FIRESTORE_TIMEOUT_MS) {
                ensureUserDocumentExists(uid)
                userRepository.applyProgressUpdate(uid, ::applyCompletion)
            }
        }.getOrElse {
            errorMessage = "Your progress is saved on this device, but we couldn't sync it online yet."
            applyCompletion(userProgress)
        }
        userProgress = updatedProgress
        localCache?.saveUserProgress(uid, updatedProgress)

        // Record today's completion on every shared streak this user is part of, so a
        // connected friend's shared streak advances once both of them finish today.
        runCatching {
            withTimeout(FIRESTORE_TIMEOUT_MS) {
                sharedStreakRepository.recordCompletion(uid, today)
            }
        }.onFailure { Log.w(TAG, "updateUserProgress: shared streak sync failed", it) }
    }

    private suspend fun updateSkippedProgress(uid: String) {
        fun applySkip(current: UserProgress): UserProgress =
            current.copy(skippedQuestCount = current.skippedQuestCount + 1)

        val updatedProgress = runCatching {
            withTimeout(FIRESTORE_TIMEOUT_MS) {
                ensureUserDocumentExists(uid)
                userRepository.applyProgressUpdate(uid, ::applySkip)
            }
        }.getOrElse {
            errorMessage = "Your progress is saved on this device, but we couldn't sync it online yet."
            applySkip(userProgress)
        }
        userProgress = updatedProgress
        localCache?.saveUserProgress(uid, updatedProgress)
    }

    private suspend fun ensureUserDocumentExists(uid: String) {
        if (userRepository.getUser(uid) != null) return
        val firebaseUser = auth.currentUser
        userRepository.saveUser(
            User(
                uid = uid,
                displayName = firebaseUser?.displayName.orEmpty(),
                email = firebaseUser?.email.orEmpty()
            )
        )
    }

    private fun List<Quest>.withStates(states: List<QuestState>): List<Quest> {
        val stateByQuestId = states.associateBy { it.questId }
        return map { quest ->
            val state = stateByQuestId[quest.id]
            if (state == null) {
                quest
            } else {
                quest.copy(
                    status = state.status,
                    currentProgress = state.currentProgress.coerceIn(0, quest.targetProgress.coerceAtLeast(1)),
                    targetProgress = state.targetProgress.coerceAtLeast(quest.targetProgress.coerceAtLeast(1)),
                    progressUnit = state.progressUnit.takeIf { it.isNotBlank() } ?: quest.progressUnit,
                    lastProgressUpdatedAt = state.lastProgressUpdatedAt
                )
            }
        }
    }

    private fun Quest.toQuestState(
        status: QuestStatus,
        lastProgressUpdatedAt: Long? = null
    ): QuestState {
        val now = System.currentTimeMillis()
        val completedProgress = if (status == QuestStatus.Completed && isLongTerm) {
            targetProgress.coerceAtLeast(1)
        } else {
            currentProgress
        }
        return QuestState(
            questStateId = if (isLongTerm) "long-term-$id" else "$today-$id",
            questId = id,
            date = today,
            status = status,
            isDailyAssigned = id in dailyQuestIds,
            startedAt = if (status == QuestStatus.Active) now else null,
            completedAt = if (status == QuestStatus.Completed) now else null,
            skippedAt = if (status == QuestStatus.Skipped) now else null,
            currentProgress = completedProgress.coerceIn(0, targetProgress.coerceAtLeast(1)),
            targetProgress = targetProgress.coerceAtLeast(1),
            progressUnit = progressUnit,
            lastProgressUpdatedAt = lastProgressUpdatedAt
        )
    }

    private fun refreshDailyAssignments(sourceQuests: List<Quest> = quests) {
        dailyQuestIds = DailyQuestSelector.dailyQuestIds(
            quests = sourceQuests,
            dailyQuestLimit = dailyQuestLimit,
            feedbackByQuestId = feedbackByQuestId,
            feedbackScoreByCategory = feedbackScoreByCategory
        )
    }

    private fun updateDemoActivityProgress(
        quest: Quest,
        countCompletedQuest: Boolean = true
    ) {
        val categoryCounts = userProgress.categoryCounts.toMutableMap()
        val categoryKey = quest.category.name
        categoryCounts[categoryKey] = (categoryCounts[categoryKey] ?: 0) + 1
        userProgress = userProgress.copy(
            currentStreak = maxOf(userProgress.currentStreak, 1),
            completedQuestCount = userProgress.completedQuestCount + if (countCompletedQuest) 1 else 0,
            categoryCounts = categoryCounts,
            lastQuestCompletionDate = today
        )
    }

    private fun Quest.wasProgressUpdatedToday(now: Long = System.currentTimeMillis()): Boolean {
        val updatedAt = lastProgressUpdatedAt ?: return false
        return utcDateFormat().format(Date(updatedAt)) == utcDateFormat().format(Date(now))
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

private const val TAG = "QuestViewModel"
private const val FIRESTORE_TIMEOUT_MS = 8_000L
private const val DEMO_REFLECTION_UID = "demo-reflections"
private const val DEFAULT_REFLECTION_PROMPT = "What did you notice, learn, or want to do differently next time?"

// UTC so that personal-streak dates line up with SharedStreakLogic (also UTC),
// preventing streaks from breaking or double-counting around midnight / across timezones.
private val dateFormat = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
}

// Single place that resolves the thread-local formatter, so the non-null handling
// lives here instead of being repeated as `!!` at every call site.
private fun utcDateFormat(): SimpleDateFormat =
    dateFormat.get() ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

private fun yesterday(): String {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    return utcDateFormat().format(calendar.time)
}

private fun mergeQuestStates(remoteStates: List<QuestState>, cachedStates: List<QuestState>): List<QuestState> =
    (remoteStates + cachedStates)
        .groupBy { it.questStateId }
        .map { (_, versions) -> versions.maxWith(compareBy<QuestState> { it.status.persistenceRank }.thenBy { it.currentProgress }) }

private fun List<QuestState>.filterForCurrentQuests(quests: List<Quest>): List<QuestState> {
    val longTermQuestIds = quests.filter { it.isLongTerm }.map { it.id }.toSet()
    return filter { state -> state.date == utcDateFormat().format(Date()) || state.questId in longTermQuestIds }
}

private fun preferredProgress(
    remoteProgress: UserProgress?,
    cachedProgress: UserProgress?
): UserProgress =
    listOfNotNull(remoteProgress, cachedProgress, UserProgress())
        .maxWith(
            // "yyyy-MM-dd" sorts lexicographically, so the most recent completion wins first;
            // counts only break ties when both sources were last active on the same day.
            compareBy<UserProgress> { it.lastQuestCompletionDate ?: "" }
                .thenBy { it.completedQuestCount }
                .thenBy { it.currentStreak }
        )

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

private fun Quest.reflectionPrompt(): String = journalPrompt ?: when (category) {
    QuestCategory.Academic -> "What helped your learning, and what could you improve next time?"
    QuestCategory.Focus -> "What made it easier or harder to stay focused?"
    QuestCategory.Wellbeing -> "How did this affect how you feel right now?"
    QuestCategory.Social -> "What did you notice about the interaction?"
    QuestCategory.Movement -> "How did your body or energy feel afterward?"
}
