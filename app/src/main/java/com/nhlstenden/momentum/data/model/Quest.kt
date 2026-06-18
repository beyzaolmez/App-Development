package com.nhlstenden.momentum.data.model

data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val category: QuestCategory,
    val xp: Int,
    val difficulty: QuestDifficulty,
    val estimatedMinutes: Int,
    val steps: List<String>,
    val journalPrompt: String? = null,
    val status: QuestStatus = QuestStatus.Available,
    val goalType: QuestGoalType = QuestGoalType.Daily,
    val targetProgress: Int = 1,
    val progressUnit: String = "completion",
    val currentProgress: Int = 0,
    val lastProgressUpdatedAt: Long? = null
) {
    val isLongTerm: Boolean
        get() = goalType == QuestGoalType.LongTerm

    val progressFraction: Float
        get() = if (targetProgress <= 0) 0f else currentProgress.toFloat() / targetProgress.toFloat()
}

enum class QuestGoalType(val label: String) {
    Daily("Daily"),
    LongTerm("Long-term")
}

enum class QuestCategory(val label: String) {
    Academic("Academic"),
    Focus("Focus"),
    Wellbeing("Wellbeing"),
    Social("Social"),
    Movement("Movement")
}

enum class QuestDifficulty(val label: String) {
    Easy("Easy"),
    Medium("Medium"),
    Hard("Hard")
}

enum class QuestStatus(val label: String) {
    Available("Available"),
    Active("Active"),
    Completed("Completed"),
    Skipped("Skipped")
}
