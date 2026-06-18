package com.nhlstenden.momentum.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.nhlstenden.momentum.data.model.Quest
import com.nhlstenden.momentum.data.model.QuestFeedbackType
import com.nhlstenden.momentum.data.model.SharedStreakStatus
import com.nhlstenden.momentum.data.repository.FirestoreQuestFeedbackRepository
import com.nhlstenden.momentum.data.repository.FirestoreSharedStreakRepository
import com.nhlstenden.momentum.data.repository.PredefinedQuestRepository
import com.nhlstenden.momentum.data.repository.SharedStreakRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class FriendLikedQuest(
    val friendName: String,
    val quest: Quest
)

class QuestBoardViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val sharedStreakRepository: SharedStreakRepository = FirestoreSharedStreakRepository(),
    private val feedbackRepository: FirestoreQuestFeedbackRepository = FirestoreQuestFeedbackRepository(),
    private val questRepository: PredefinedQuestRepository = PredefinedQuestRepository()
) : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set

    var isSignedIn by mutableStateOf(auth.currentUser != null)
        private set

    var loadError by mutableStateOf<String?>(null)
        private set

    var friendLikedQuests by mutableStateOf<List<FriendLikedQuest>>(emptyList())
        private set

    init {
        load()
    }

    fun refresh() = load()

    private fun load() {
        val uid = auth.currentUser?.uid
        isSignedIn = uid != null
        if (uid == null) return

        viewModelScope.launch {
            isLoading = true
            loadError = null

            runCatching {
                // Step 1: get active friend connections (shared streaks that are Active)
                val activeStreaks = withTimeout(8_000L) {
                    sharedStreakRepository.getStreaksForUser(uid)
                        .filter { it.status == SharedStreakStatus.Active }
                }

                // Step 2: for each friend, fetch their liked quests in parallel
                coroutineScope {
                    activeStreaks.map { streak ->
                        async {
                            val friendUid = streak.otherMemberId(uid)
                                ?: return@async emptyList<FriendLikedQuest>()
                            val friendName = streak.otherMemberName(uid)
                            runCatching {
                                withTimeout(8_000L) {
                                    feedbackRepository.getFeedback(friendUid)
                                }
                            }.getOrDefault(emptyList())
                                .filter { it.feedbackType == QuestFeedbackType.Like }
                                .mapNotNull { feedback ->
                                    // Step 3: resolve the quest object by id
                                    questRepository.getQuestById(feedback.questId)?.let { quest ->
                                        FriendLikedQuest(friendName, quest)
                                    }
                                }
                        }
                    }.awaitAll().flatten()
                }
            }.onSuccess { results ->
                friendLikedQuests = results
            }.onFailure {
                loadError = "Couldn't load the quest board. Try refreshing."
            }

            isLoading = false
        }
    }
}
