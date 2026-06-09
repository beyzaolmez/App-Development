package com.example.emptyapp.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Saves user feedback to Cloud Firestore under the "feedback" collection.
// Each document contains the message text and a local timestamp.
// Firestore queues writes offline automatically — feedback is never lost
// due to temporary connectivity issues.
object FeedbackStore {

    // Lazy so Firestore is only accessed after Firebase has initialised
    // (which happens automatically when google-services.json is present).
    private val collection by lazy {
        Firebase.firestore.collection("feedback")
    }

    fun save(
        message: String,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val data = hashMapOf(
            "message" to message.trim(),
            "timestamp" to timestamp
        )
        collection.add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }
}
