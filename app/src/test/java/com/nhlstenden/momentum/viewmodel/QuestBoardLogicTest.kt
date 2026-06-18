package com.nhlstenden.momentum.viewmodel

import com.nhlstenden.momentum.data.model.Quest
import com.nhlstenden.momentum.data.model.QuestCategory
import com.nhlstenden.momentum.data.model.QuestDifficulty
import com.nhlstenden.momentum.data.model.SharedStreak
import com.nhlstenden.momentum.data.model.SharedStreakLogic
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
                streak("friend", "Noor", SharedStreakStatus.Active, likedQuestIds = listOf("liked-quest", "missing-quest")),
                streak("pending", "Sam", SharedStreakStatus.Pending, likedQuestIds = listOf("pending-friend-quest"))
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
            streaks = listOf(SharedStreakLogic.recordLike(activeStreak, "friend", "first")),
            questById = quests::get
        )
        val refreshedResult = QuestBoardLogic.friendLikedQuests(
            currentUid = "me",
            streaks = listOf(
                SharedStreakLogic.recordLike(
                    SharedStreakLogic.recordLike(activeStreak, "friend", "first"),
                    "friend",
                    "second"
                )
            ),
            questById = quests::get
        )

        assertEquals(listOf("first"), firstResult.map { it.quest.id })
        assertEquals(listOf("first", "second"), refreshedResult.map { it.quest.id })
    }

    @Test
    fun `friend liked quests includes activity from multiple active friends`() {
        val quests = listOf(
            quest("shared", "Shared quest"),
            quest("solo", "Solo quest")
        ).associateBy { it.id }

        val results = QuestBoardLogic.friendLikedQuests(
            currentUid = "me",
            streaks = listOf(
                streak("noor", "Noor", SharedStreakStatus.Active, likedQuestIds = listOf("shared")),
                streak("alex", "Alex", SharedStreakStatus.Active, likedQuestIds = listOf("shared", "solo"))
            ),
            questById = quests::get
        )

        assertEquals(
            listOf(
                "Noor" to "shared",
                "Alex" to "shared",
                "Alex" to "solo"
            ),
            results.map { it.friendName to it.quest.id }
        )
    }

    private fun streak(
        friendUid: String,
        friendName: String,
        status: SharedStreakStatus,
        likedQuestIds: List<String> = emptyList()
    ) =
        SharedStreak(
            id = "streak-$friendUid",
            memberIds = listOf("me", friendUid),
            memberNames = mapOf("me" to "Me", friendUid to friendName),
            status = status,
            likedQuestIdsByMember = mapOf(friendUid to likedQuestIds)
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
