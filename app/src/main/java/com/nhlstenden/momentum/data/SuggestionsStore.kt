package com.nhlstenden.momentum.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nhlstenden.momentum.util.MomentumDateFormat
import java.util.Date

// Saves user quest suggestions to Cloud Firestore under the "quest_suggestions" collection.
// Each document includes the suggestion content, a timestamp, and the submitting user's uid.
object SuggestionsStore {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val collection by lazy { Firebase.firestore.collection("quest_suggestions") }

    fun save(
        title: String,
        category: String,
        notes: String,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        // Security rules require uid == request.auth.uid, so an unauthenticated
        // ("anonymous") write would always be rejected. Fail fast with a clear
        // message instead of issuing a doomed request.
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError(IllegalStateException("Sign in to suggest a quest."))
            return
        }
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            onError(IllegalArgumentException("Give your quest suggestion a title."))
            return
        }

        val timestamp = MomentumDateFormat.formatIsoDateTime(Date())
        val data = hashMapOf(
            // Cap each field so a single document can't grow unbounded.
            "title" to trimmedTitle.take(MAX_TITLE_LENGTH),
            "category" to category.trim().take(MAX_CATEGORY_LENGTH),
            "notes" to notes.trim().take(MAX_NOTES_LENGTH),
            "timestamp" to timestamp,
            "uid" to uid
        )
        collection.add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }

    private const val MAX_TITLE_LENGTH = 120
    private const val MAX_CATEGORY_LENGTH = 60
    private const val MAX_NOTES_LENGTH = 1_000
}
