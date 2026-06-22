package com.nhlstenden.momentum.data.model

data class User(
    val uid: String,
    val displayName: String,
    val email: String,
    val interests: List<String> = emptyList(),
    val notificationEnabled: Boolean = false,
    val progress: UserProgress = UserProgress(),
    val onboardingCompleted: Boolean = false,
    val themePreference: String? = null
)

data class UserProgress(
    val currentStreak: Int = 0,
    val completedQuestCount: Int = 0,
    val skippedQuestCount: Int = 0,
    val categoryCounts: Map<String, Int> = emptyMap(),
    val lastQuestCompletionDate: String? = null
)
