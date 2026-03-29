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
class BookingRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private val bookings get() = firestore.collection("bookings")

    // Real-time stream: new, pending, active requests for this provider
    fun getIncomingRequests(providerId: String): Flow<List<ServiceRequest>> = callbackFlow {
        val sub = bookings
            .whereEqualTo("providerId", providerId)
            .whereIn("status", listOf("new", "pending", "active"))
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                trySend(snap.documents.mapNotNull { it.toServiceRequest() })
            }
        awaitClose { sub.remove() }
    }

    // Real-time stream: completed and declined, newest first
    fun getPastBookings(providerId: String): Flow<List<ServiceRequest>> = callbackFlow {
        val sub = bookings
            .whereEqualTo("providerId", providerId)
            .whereIn("status", listOf("completed", "declined"))
            .orderBy("completedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                trySend(snap.documents.mapNotNull { it.toServiceRequest() })
            }
        awaitClose { sub.remove() }
    }

    suspend fun acceptRequest(bookingId: String) {
        bookings.document(bookingId).update("status", "active").await()
    }

    suspend fun declineRequest(bookingId: String) {
        bookings.document(bookingId).update("status", "declined").await()
    }

    suspend fun markDone(bookingId: String) {
        bookings.document(bookingId).update(
            mapOf(
                "status"      to "completed",
                "completedAt" to Timestamp.now(),
            )
        ).await()
    }

    suspend fun logCall(bookingId: String) {
        bookings.document(bookingId)
            .update("callLoggedAt", Timestamp.now()).await()
    }

    suspend fun rateCustomer(bookingId: String, rating: Float) {
        bookings.document(bookingId)
            .update("customerRating", rating).await()
    }
}