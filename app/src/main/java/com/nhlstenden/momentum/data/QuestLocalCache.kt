package com.nhlstenden.momentum.data

import android.content.Context
import com.nhlstenden.momentum.data.model.QuestState
import com.nhlstenden.momentum.data.model.QuestStatus
import com.nhlstenden.momentum.data.model.UserProgress
import org.json.JSONArray
import org.json.JSONObject

class QuestLocalCache(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadQuestStates(uid: String): List<QuestState> {
        val raw = prefs.getString(statesKey(uid), null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.toQuestState()?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveQuestStates(uid: String, states: List<QuestState>) {
        val merged = (loadQuestStates(uid) + states)
            .groupBy { it.questStateId }
            .map { (_, versions) -> versions.maxWith(compareBy<QuestState> { it.status.persistenceRank }.thenBy { it.currentProgress }) }
        prefs.edit().putString(statesKey(uid), JSONArray(merged.map { it.toJson() }).toString()).apply()
    }

    fun saveQuestState(uid: String, state: QuestState) {
        saveQuestStates(uid, listOf(state))
    }

    fun loadUserProgress(uid: String): UserProgress? {
        val raw = prefs.getString(progressKey(uid), null) ?: return null
        return runCatching { JSONObject(raw).toUserProgress() }.getOrNull()
    }

    fun saveUserProgress(uid: String, progress: UserProgress) {
        prefs.edit().putString(progressKey(uid), progress.toJson().toString()).apply()
    }

    private fun statesKey(uid: String): String = "quest_states_$uid"

    private fun progressKey(uid: String): String = "user_progress_$uid"

    private fun QuestState.toJson(): JSONObject = JSONObject()
        .put("questStateId", questStateId)
        .put("questId", questId)
        .put("date", date)
        .put("status", status.name)
        .put("isDailyAssigned", isDailyAssigned)
        .putNullable("startedAt", startedAt)
        .putNullable("completedAt", completedAt)
        .putNullable("skippedAt", skippedAt)
        .put("currentProgress", currentProgress)
        .put("targetProgress", targetProgress)
        .put("progressUnit", progressUnit)
        .putNullable("lastProgressUpdatedAt", lastProgressUpdatedAt)

    private fun JSONObject.toQuestState(): QuestState? {
        val questStateId = optString("questStateId").takeIf { it.isNotBlank() } ?: return null
        val questId = optString("questId").takeIf { it.isNotBlank() } ?: return null
        val date = optString("date").takeIf { it.isNotBlank() } ?: return null
        return QuestState(
            questStateId = questStateId,
            questId = questId,
            date = date,
            status = optString("status").toQuestStatus(),
            isDailyAssigned = optBoolean("isDailyAssigned", false),
            startedAt = optNullableLong("startedAt"),
            completedAt = optNullableLong("completedAt"),
            skippedAt = optNullableLong("skippedAt"),
            currentProgress = optInt("currentProgress", 0),
            targetProgress = optInt("targetProgress", 1).coerceAtLeast(1),
            progressUnit = optString("progressUnit", "completion").takeIf { it.isNotBlank() } ?: "completion",
            lastProgressUpdatedAt = optNullableLong("lastProgressUpdatedAt")
        )
    }

    private fun UserProgress.toJson(): JSONObject = JSONObject()
        .put("currentStreak", currentStreak)
        .put("completedQuestCount", completedQuestCount)
        .put("skippedQuestCount", skippedQuestCount)
        .put("categoryCounts", JSONObject(categoryCounts))
        .putNullable("lastQuestCompletionDate", lastQuestCompletionDate)

    private fun JSONObject.toUserProgress(): UserProgress {
        val categoryCountsJson = optJSONObject("categoryCounts") ?: JSONObject()
        val categoryCounts = buildMap {
            val keys = categoryCountsJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                put(key, categoryCountsJson.optInt(key, 0))
            }
        }
        return UserProgress(
            currentStreak = optInt("currentStreak", 0),
            completedQuestCount = optInt("completedQuestCount", 0),
            skippedQuestCount = optInt("skippedQuestCount", 0),
            categoryCounts = categoryCounts,
            lastQuestCompletionDate = optString("lastQuestCompletionDate").takeIf { it.isNotBlank() }
        )
    }

    private fun JSONObject.putNullable(key: String, value: Any?): JSONObject =
        if (value == null) put(key, JSONObject.NULL) else put(key, value)

    private fun JSONObject.optNullableLong(key: String): Long? =
        if (isNull(key)) null else optLong(key)

    private fun String?.toQuestStatus(): QuestStatus =
        enumValues<QuestStatus>().firstOrNull { it.name.equals(this, ignoreCase = true) }
            ?: QuestStatus.Available

    private val QuestStatus.persistenceRank: Int
        get() = when (this) {
            QuestStatus.Available -> 0
            QuestStatus.Active -> 1
            QuestStatus.Skipped -> 2
            QuestStatus.Completed -> 3
        }

    private companion object {
        const val PREFS_NAME = "momentum_quest_cache"
    }
}
