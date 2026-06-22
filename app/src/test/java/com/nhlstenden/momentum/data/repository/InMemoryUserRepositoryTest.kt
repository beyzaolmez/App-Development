package com.nhlstenden.momentum.data.repository

import com.nhlstenden.momentum.data.model.User
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryUserRepositoryTest {

    @Test
    fun updateThemePreferenceStoresThemeOnUser() = runBlocking {
        val repository = InMemoryUserRepository()
        repository.saveUser(
            User(
                uid = "user-1",
                displayName = "Test User",
                email = "test@example.com"
            )
        )

        repository.updateThemePreference("user-1", "paper")

        assertEquals("paper", repository.getUser("user-1")?.themePreference)
    }

    @Test
    fun deleteUserRemovesStoredUser() = runBlocking {
        val repository = InMemoryUserRepository()
        repository.saveUser(
            User(
                uid = "user-1",
                displayName = "Test User",
                email = "test@example.com"
            )
        )

        repository.deleteUser("user-1")

        assertNull(repository.getUser("user-1"))
    }
}
