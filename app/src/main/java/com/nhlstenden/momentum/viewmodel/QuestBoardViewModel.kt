package com.nhlstenden.momentum.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.nhlstenden.momentum.data.model.Quest
import com.nhlstenden.momentum.data.model.SharedStreak
import com.nhlstenden.momentum.data.model.SharedStreakStatus
import com.nhlstenden.momentum.data.repository.FirestoreSharedStreakRepository
import com.nhlstenden.momentum.data.repository.PredefinedQuestRepository
import com.nhlstenden.momentum.data.repository.SharedStreakRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class FriendLikedQuest(
    val friendName: String,
    val quest: Quest
)

object QuestBoardLogic {
    fun friendLikedQuests(
        currentUid: String,
        streaks: List<SharedStreak>,
        questById: (String) -> Quest?
    ): List<FriendLikedQuest> =
        streaks
            .filter { it.status == SharedStreakStatus.Active }
            .flatMap { streak ->
                val friendUid = streak.otherMemberId(currentUid) ?: return@flatMap emptyList()
                val friendName = streak.otherMemberName(currentUid)

                streak.likedQuestIdsFor(friendUid)
                    .mapNotNull { questId ->
                        questById(questId)?.let { quest ->
                            FriendLikedQuest(friendName, quest)
                        }
                    }
            }
}

class QuestBoardViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val sharedStreakRepository: SharedStreakRepository = FirestoreSharedStreakRepository(),
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
                val activeStreaks = withTimeout(8_000L) {
                    sharedStreakRepository.getStreaksForUser(uid)
                        .filter { it.status == SharedStreakStatus.Active }
                }

                QuestBoardLogic.friendLikedQuests(
                    currentUid = uid,
                    streaks = activeStreaks,
                    questById = questRepository::getQuestById
                )
            }.onSuccess { results ->
                friendLikedQuests = results
            }.onFailure {
                loadError = "Couldn't load the quest board. Try refreshing."
            }

            isLoading = false
        }
    }
}
