package com.nhlstenden.momentum.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * A streak shared between exactly two connected users. Both people must complete
 * at least one quest on the same day for the shared streak to advance — that is
 * what makes it a *shared* commitment rather than two independent streaks.
 *
 * Lifecycle (see [SharedStreakStatus]):
 *  • Pending  — one user invited another; waiting for the invite to be accepted.
 *  • Active   — both users are connected; the streak can grow or break.
 *  • Declined — the invited user rejected the invitation.
 *
 * SimpleDateFormat + Calendar are used (instead of java.time) to stay compatible
 * with minSdk 24, matching [StreakStore] and the quest progress logic.
 * The formatter is wrapped in ThreadLocal because SimpleDateFormat is NOT thread-safe.
 */
data class SharedStreak(
    val id: String = "",
    // Always holds exactly the two participant uids. Stored as an array so it can be
    // queried with Firestore's `whereArrayContains` for "streaks I'm part of".
    val memberIds: List<String> = emptyList(),
    // uid -> display name, so the UI can label the other person without a second fetch.
    val memberNames: Map<String, String> = emptyMap(),
    val status: SharedStreakStatus = SharedStreakStatus.Pending,
    val invitedByUid: String = "",
    val currentStreak: Int = 0,
    // uid -> last "yyyy-MM-dd" on which that member completed a quest.
    val lastCompletionDates: Map<String, String> = emptyMap(),
    // uid -> quest ids liked by that member while this shared streak is active.
    val likedQuestIdsByMember: Map<String, List<String>> = emptyMap(),
    // The day the streak last advanced (both members done). Null until it first advances.
    val lastIncrementDate: String? = null,
    val createdAt: Long = 0L
) {
    fun otherMemberId(uid: String): String? = memberIds.firstOrNull { it != uid }

    fun otherMemberName(uid: String): String =
        otherMemberId(uid)?.let { memberNames[it] }?.takeIf { it.isNotBlank() } ?: "Friend"

    /** True once both members have completed a quest today (today already counted). */
    fun bothCompletedOn(date: String): Boolean =
        memberIds.isNotEmpty() && memberIds.all { lastCompletionDates[it] == date }

    fun likedQuestIdsFor(uid: String): List<String> =
        likedQuestIdsByMember[uid].orEmpty()
}

enum class SharedStreakStatus { Pending, Active, Declined }

/**
 * Pure, side-effect-free rules for a shared streak. Kept separate from Firestore so
 * the behaviour can be reasoned about and unit-tested without any Firebase setup.
 */
object SharedStreakLogic {

    private val dateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false  // Strict parsing - reject invalid dates like "2024-13-45"
        }
    }

    private val datePattern = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    /** Validates that [date] is in "yyyy-MM-dd" format and represents a real calendar date. */
    fun isValidDate(date: String): Boolean {
        if (!datePattern.matches(date)) return false
        return runCatching { dateFormat.get()!!.parse(date) }.isSuccess
    }

    /** Returns today's date in UTC ("yyyy-MM-dd") for consistent cross-timezone streak calculation. */
    fun today(): String = dateFormat.get()!!.format(Calendar.getInstance(TimeZone.getTimeZone("UTC")).time)

    /** The calendar day immediately before [date] (expects "yyyy-MM-dd"). Returns [date] if invalid. */
    fun previousDay(date: String): String {
        if (!isValidDate(date)) return date
        val parsed = dateFormat.get()!!.parse(date) ?: return date
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            time = parsed
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return dateFormat.get()!!.format(calendar.time)
    }

    /**
     * Records that [uid] completed a quest on [today] and advances the shared streak
     * when both members are done for the day.
     *
     * • Advances by 1 only the first time both finish on a given day.
     * • Continues the run if the previous advance was yesterday; otherwise restarts at 1.
     * Only Active streaks change; pending/declined ones are returned untouched.
     */
    fun recordCompletion(streak: SharedStreak, uid: String, today: String): SharedStreak {
        if (streak.status != SharedStreakStatus.Active) return streak
        if (uid !in streak.memberIds) return streak

        val updatedDates = streak.lastCompletionDates + (uid to today)
        val candidate = streak.copy(lastCompletionDates = updatedDates)

        val alreadyCountedToday = streak.lastIncrementDate == today
        if (!candidate.bothCompletedOn(today) || alreadyCountedToday) {
            return candidate
        }

        val continuingRun = streak.lastIncrementDate == previousDay(today)
        val newCount = if (continuingRun) streak.currentStreak + 1 else 1
        return candidate.copy(currentStreak = newCount, lastIncrementDate = today)
    }

    fun recordLike(streak: SharedStreak, uid: String, questId: String): SharedStreak {
        if (streak.status != SharedStreakStatus.Active) return streak
        if (uid !in streak.memberIds) return streak
        if (questId.isBlank()) return streak

        val currentLikedQuestIds = streak.likedQuestIdsFor(uid)
        if (questId in currentLikedQuestIds) return streak

        return streak.copy(
            likedQuestIdsByMember = streak.likedQuestIdsByMember + (uid to (currentLikedQuestIds + questId))
        )
    }

    /**
     * Re-evaluates an Active streak against [today]. The streak stays intact only while
     * its last advance was today or yesterday; once a full day is missed it breaks and
     * the count resets to 0. Idempotent — safe to run every time a streak is loaded.
     */
    fun evaluateForToday(streak: SharedStreak, today: String): SharedStreak {
        if (streak.status != SharedStreakStatus.Active) return streak
        val lastIncrement = streak.lastIncrementDate ?: return streak

        val intact = lastIncrement == today || lastIncrement == previousDay(today)
        return if (intact || streak.currentStreak == 0) streak
        else streak.copy(currentStreak = 0)
    }
}
