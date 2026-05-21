package com.nhlstenden.momentum.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.nhlstenden.momentum.data.model.Quest
import com.nhlstenden.momentum.data.model.QuestStatus
import com.nhlstenden.momentum.data.repository.PredefinedQuestRepository

class QuestViewModel : ViewModel() {
    private val dailyQuestLimit = 3
    private val repository = PredefinedQuestRepository()
    private val initialQuests = repository.getQuests()
    private val dailyQuestIds = initialQuests
        .take(dailyQuestLimit)
        .map { it.id }
        .toSet()

    var isLoading by mutableStateOf(true)
        private set

    var selectedStatus by mutableStateOf<QuestStatus?>(null)
        private set

    private var quests by mutableStateOf(initialQuests)

    init {
        isLoading = false
    }

    fun visibleQuests(): List<Quest> {
        val filteredQuests = selectedStatus?.let { status ->
            quests.filter { it.status == status }
        } ?: quests

        return filteredQuests.dailyLimited().sortedWith(
            compareBy<Quest> { it.status.sortOrder }
                .thenBy { it.category.label }
                .thenBy { it.title }
        )
    }

    fun activeQuestCount(): Int = quests.count { it.status == QuestStatus.Active }

    fun completedQuestCount(): Int = quests.count { it.status == QuestStatus.Completed }

    fun totalQuestCount(): Int = quests.size

    fun dailyQuestLimit(): Int = dailyQuestLimit

    fun currentStreak(): Int = if (completedQuestCount() > 0) 1 else 0

    fun questById(id: String): Quest? = quests.firstOrNull { it.id == id }

    fun selectStatus(status: QuestStatus?) {
        selectedStatus = status
    }

    fun startQuest(id: String) {
        updateQuestStatus(id, QuestStatus.Active)
    }

    fun completeQuest(id: String) {
        updateQuestStatus(id, QuestStatus.Completed)
    }

    fun skipQuest(id: String) {
        updateQuestStatus(id, QuestStatus.Skipped)
    }

    private fun updateQuestStatus(id: String, status: QuestStatus) {
        quests = quests.map { quest ->
            if (quest.id == id) quest.copy(status = status) else quest
        }
    }

    private fun List<Quest>.dailyLimited(): List<Quest> {
        if (selectedStatus != null && selectedStatus != QuestStatus.Available) return this

        return filter { quest ->
            quest.status != QuestStatus.Available || quest.id in dailyQuestIds
        }
    }
}

private val QuestStatus.sortOrder: Int
    get() = when (this) {
        QuestStatus.Active -> 0
        QuestStatus.Available -> 1
        QuestStatus.Completed -> 2
        QuestStatus.Skipped -> 3
    }
