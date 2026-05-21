package com.nhlstenden.momentum.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nhlstenden.momentum.data.model.Quest
import com.nhlstenden.momentum.data.model.QuestStatus
import com.nhlstenden.momentum.data.repository.QuestRepository

class QuestStateHolder(
    repository: QuestRepository
) {
    var isLoading by mutableStateOf(true)
        private set

    var selectedStatus by mutableStateOf<QuestStatus?>(null)
        private set

    private var quests by mutableStateOf(repository.getQuests())

    init {
        isLoading = false
    }

    fun visibleQuests(): List<Quest> {
        val filteredQuests = selectedStatus?.let { status ->
            quests.filter { it.status == status }
        } ?: quests

        return filteredQuests.sortedWith(
            compareBy<Quest> { it.status.sortOrder }
                .thenBy { it.category.label }
                .thenBy { it.title }
        )
    }

    fun activeQuestCount(): Int = quests.count { it.status == QuestStatus.Active }

    fun completedQuestCount(): Int = quests.count { it.status == QuestStatus.Completed }

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
}

private val QuestStatus.sortOrder: Int
    get() = when (this) {
        QuestStatus.Active -> 0
        QuestStatus.Available -> 1
        QuestStatus.Completed -> 2
        QuestStatus.Skipped -> 3
    }
