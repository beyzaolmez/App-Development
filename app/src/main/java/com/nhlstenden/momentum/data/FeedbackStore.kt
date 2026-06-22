package com.nhlstenden.momentum.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nhlstenden.momentum.util.MomentumDateFormat
import java.util.Date

// Saves user feedback to Cloud Firestore under the "feedback" collection.
// Each document contains the message text and a local timestamp.
// Firestore queues writes offline automatically — feedback is never lost
// due to temporary connectivity issues.
object FeedbackStore {

    // Lazy so Firestore is only accessed after Firebase has initialised
    // (which happens automatically when google-services.json is present).
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val collection by lazy { Firebase.firestore.collection("feedback") }

    fun save(
        message: String,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        // Security rules require uid == request.auth.uid, so an unauthenticated
        // ("anonymous") write would always be rejected. Fail fast with a clear
        // message instead of issuing a doomed request.
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError(IllegalStateException("Sign in to send feedback."))
            return
        }
        val timestamp = MomentumDateFormat.formatIsoDateTime(Date())
        val data = hashMapOf(
            "message" to message.trim(),
            "timestamp" to timestamp,
            "uid" to uid
        )
        collection.add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }
}
