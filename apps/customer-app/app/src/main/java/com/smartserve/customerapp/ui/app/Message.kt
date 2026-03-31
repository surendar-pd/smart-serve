package com.smartserve.customerapp.ui.app

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null,
)

fun DocumentSnapshot.toMessage(): Message? {
    return try {
        Message(
            id        = id,
            senderId  = getString("senderId") ?: return null,
            text      = getString("text") ?: return null,
            timestamp = getTimestamp("timestamp"),
        )
    } catch (_: Exception) {
        null
    }
}

