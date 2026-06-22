package com.nhlstenden.momentum.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FriendlyErrorMessagesTest {

    @Test
    fun accountDeletionExplainsWrongPassword() {
        val message = FriendlyErrorMessages.accountDeletion(
            errorCode = "ERROR_WRONG_PASSWORD",
            rawMessage = null
        )

        assertEquals(
            "That password doesn't match this account. Please check it and try again.",
            message
        )
    }

    @Test
    fun accountDeletionExplainsNetworkFailure() {
        val message = FriendlyErrorMessages.accountDeletion(
            errorCode = null,
            rawMessage = "NETWORK_ERROR"
        )

        assertEquals(
            "Can't reach the network. Check your connection and try again.",
            message
        )
    }

    @Test
    fun accountDeletionExplainsMissingSession() {
        val message = FriendlyErrorMessages.accountDeletion(
            errorCode = null,
            rawMessage = "No user signed in"
        )

        assertEquals(
            "Please sign in before deleting your account.",
            message
        )
    }
}
