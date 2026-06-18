package com.nhlstenden.momentum.viewmodel

import com.nhlstenden.momentum.data.model.Quest
import com.nhlstenden.momentum.data.model.QuestCategory
import com.nhlstenden.momentum.data.model.QuestDifficulty
import com.nhlstenden.momentum.data.model.QuestFeedback
import com.nhlstenden.momentum.data.model.QuestFeedbackType
import com.nhlstenden.momentum.data.model.SharedStreak
import com.nhlstenden.momentum.data.model.SharedStreakStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class QuestBoardLogicTest {

    @Test
    fun `friend liked quests only includes likes from active connected friends`() {
        val quests = listOf(
            quest("liked-quest", "Liked quest"),
            quest("disliked-quest", "Disliked quest"),
            quest("pending-friend-quest", "Pending friend quest")
        ).associateBy { it.id }

        val results = QuestBoardLogic.friendLikedQuests(
            currentUid = "me",
            streaks = listOf(
                streak("friend", "Noor", SharedStreakStatus.Active),
                streak("pending", "Sam", SharedStreakStatus.Pending)
            ),
            feedbackByFriendUid = mapOf(
                "friend" to listOf(
                    feedback("liked-quest", QuestFeedbackType.Like),
                    feedback("disliked-quest", QuestFeedbackType.Dislike),
                    feedback("missing-quest", QuestFeedbackType.Like)
                ),
                "pending" to listOf(
                    feedback("pending-friend-quest", QuestFeedbackType.Like)
                )
            ),
            questById = quests::get
        )

        assertEquals(listOf(FriendLikedQuest("Noor", quests.getValue("liked-quest"))), results)
    }

    @Test
    fun `friend liked quests updates when new likes are provided`() {
        val quests = listOf(
            quest("first", "First quest"),
            quest("second", "Second quest")
        ).associateBy { it.id }
        val activeStreak = streak("friend", "Noor", SharedStreakStatus.Active)

        val firstResult = QuestBoardLogic.friendLikedQuests(
            currentUid = "me",
            streaks = listOf(activeStreak),
            feedbackByFriendUid = mapOf("friend" to listOf(feedback("first", QuestFeedbackType.Like))),
            questById = quests::get
        )
        val refreshedResult = QuestBoardLogic.friendLikedQuests(
            currentUid = "me",
            streaks = listOf(activeStreak),
            feedbackByFriendUid = mapOf(
                "friend" to listOf(
                    feedback("first", QuestFeedbackType.Like),
                    feedback("second", QuestFeedbackType.Like)
                )
            ),
            questById = quests::get
        )

        assertEquals(listOf("first"), firstResult.map { it.quest.id })
        assertEquals(listOf("first", "second"), refreshedResult.map { it.quest.id })
    }

    private fun streak(friendUid: String, friendName: String, status: SharedStreakStatus) =
        SharedStreak(
            id = "streak-$friendUid",
            memberIds = listOf("me", friendUid),
            memberNames = mapOf("me" to "Me", friendUid to friendName),
            status = status
        )

    private fun feedback(questId: String, type: QuestFeedbackType) =
        QuestFeedback(
            feedbackId = "feedback-$questId-${type.name}",
            questId = questId,
            category = QuestCategory.Wellbeing,
            feedbackType = type,
            createdAt = 1L
        )

    private fun quest(id: String, title: String) =
        Quest(
            id = id,
            title = title,
            description = "Description for $title",
            category = QuestCategory.Wellbeing,
            xp = 10,
            difficulty = QuestDifficulty.Easy,
            estimatedMinutes = 5,
            steps = emptyList()
        )
}
