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
        val timestamp = MomentumDateFormat.formatIsoDateTime(Date())
        val data = hashMapOf(
            "title" to title.trim(),
            "category" to category.trim(),
            "notes" to notes.trim(),
            "timestamp" to timestamp,
            "uid" to (auth.currentUser?.uid ?: "anonymous")
        )
        collection.add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }
}
