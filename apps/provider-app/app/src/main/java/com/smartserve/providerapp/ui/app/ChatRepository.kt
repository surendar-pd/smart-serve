package com.smartserve.providerapp.ui.app

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    // ── Collection references ─────────────────────────────────────────────

    private fun messagesCollection(bookingId: String) =
        firestore.collection("bookings").document(bookingId).collection("messages")

    private fun bookingDocument(bookingId: String) =
        firestore.collection("bookings").document(bookingId)

    // ── Real-time message stream ──────────────────────────────────────────

    /**
     * Returns a Flow that emits the list of messages in chronological order
     * every time the Firestore collection changes. Uses addSnapshotListener
     * under the hood — the listener is cleaned up when the Flow collector
     * is cancelled (e.g. when the screen navigates away).
     */
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

    // ── Send a message ────────────────────────────────────────────────────

    /**
     * Writes a new message document to the booking's messages subcollection.
     * Firestore auto-generates the document ID.
     */
    suspend fun sendMessage(bookingId: String, senderId: String, text: String) {
        val messageData = mapOf(
            "senderId"  to senderId,
            "text"      to text,
            "timestamp" to Timestamp.now(),
        )
        messagesCollection(bookingId).add(messageData).await()
    }

    // ── Booking status check ──────────────────────────────────────────────

    /**
     * Real-time stream of the booking's status field.
     * Used to enable/disable the chat input dynamically.
     */
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
}