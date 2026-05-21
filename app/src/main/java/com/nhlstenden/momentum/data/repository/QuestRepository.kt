package com.nhlstenden.momentum.data.repository

import com.nhlstenden.momentum.data.model.Quest
import com.nhlstenden.momentum.data.model.QuestCategory
import com.nhlstenden.momentum.data.model.QuestDifficulty

interface QuestRepository {
    fun getQuests(): List<Quest>
    fun getQuestById(id: String): Quest?
}

class PredefinedQuestRepository : QuestRepository {
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
}
