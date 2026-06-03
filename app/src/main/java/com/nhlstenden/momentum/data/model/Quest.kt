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
    val status: QuestStatus = QuestStatus.Available
)

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
