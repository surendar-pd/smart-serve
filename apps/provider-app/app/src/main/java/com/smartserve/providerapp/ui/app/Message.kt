package com.smartserve.providerapp.ui.app

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null,
)

/**
 * Extension to convert a Firestore document into a Message.
 * Returns null if required fields are missing.
 */
fun DocumentSnapshot.toMessage(): Message? {
    return try {
        Message(
            id        = this.id,
            senderId  = getString("senderId") ?: return null,
            text      = getString("text") ?: return null,
            timestamp = getTimestamp("timestamp"),
        )
    } catch (e: Exception) {
        null
    }
}