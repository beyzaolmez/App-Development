package com.nhlstenden.momentum.data.repository

import com.nhlstenden.momentum.data.model.User
import com.nhlstenden.momentum.data.model.UserProgress

interface UserRepository {
    suspend fun getUser(uid: String): User?
    suspend fun saveUser(user: User)
    suspend fun updateInterests(uid: String, interests: List<String>)
    suspend fun updateNotificationPreference(uid: String, enabled: Boolean)
    suspend fun updateProgress(uid: String, progress: UserProgress)
}

class InMemoryUserRepository : UserRepository {
    private val users = mutableMapOf<String, User>()

    override suspend fun getUser(uid: String): User? = users[uid]

    override suspend fun saveUser(user: User) {
        users[user.uid] = user
    }

    override suspend fun updateInterests(uid: String, interests: List<String>) {
        users[uid] = users[uid]?.copy(interests = interests) ?: return
    }

    override suspend fun updateNotificationPreference(uid: String, enabled: Boolean) {
        users[uid] = users[uid]?.copy(notificationEnabled = enabled) ?: return
    }

    override suspend fun updateProgress(uid: String, progress: UserProgress) {
        users[uid] = users[uid]?.copy(progress = progress) ?: return
    }
}
