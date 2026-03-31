package com.smartserve.customerapp.ui.app

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val firestore: FirebaseFirestore,
) {
    private fun messagesCollection(bookingId: String) =
        firestore.collection("bookings").document(bookingId).collection("messages")

    private fun bookingDocument(bookingId: String) =
        firestore.collection("bookings").document(bookingId)

    fun getMessages(bookingId: String): Flow<List<Message>> = callbackFlow {
        val subscription = messagesCollection(bookingId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val messages = snapshot.documents.mapNotNull { it.toMessage() }
                trySend(messages)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun sendMessage(bookingId: String, senderId: String, text: String) {
        val messageData = mapOf(
            "senderId"  to senderId,
            "text"      to text,
            "timestamp" to Timestamp.now(),
        )
        messagesCollection(bookingId).add(messageData).await()
    }

    fun getBookingStatus(bookingId: String): Flow<String?> = callbackFlow {
        val subscription = bookingDocument(bookingId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot.getString("status"))
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getProviderName(bookingId: String): String {
        val bookingSnap = bookingDocument(bookingId).get().await()
        val providerRef = bookingSnap.get("provider") as? DocumentReference
            ?: return ""
        return providerRef.get().await()
            .getString("displayName")
            ?.trim()
            .orEmpty()
    }
}

