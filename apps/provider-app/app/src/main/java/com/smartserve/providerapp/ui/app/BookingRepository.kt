package com.smartserve.providerapp.ui.app

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.smartserve.sharedauth.AuthCollections
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private val bookings get() = firestore.collection(AuthCollections.BOOKINGS)
    private val customerProfiles get() = firestore.collection(AuthCollections.CUSTOMER_PROFILES)
    private val users get() = firestore.collection(AuthCollections.USERS)

    // Real-time stream: new, pending, active requests for this provider
    fun getIncomingRequests(providerId: String): Flow<List<ServiceRequest>> = callbackFlow {
    val sub = bookings
        .whereEqualTo("providerUid", providerId)
        .whereIn("status", listOf("new", "pending", "active", "confirmed", "CONFIRMED"))
        .addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            android.util.Log.d("BookingRepo", "Docs found: ${snap.documents.size}")
            launch {
                val parsed = snap.documents.mapNotNull { it.toServiceRequest() }
                trySend(withCustomerNames(parsed))
            }
        }
        awaitClose { sub.remove() }
    }

    // Real-time stream: completed and declined, newest first
    fun getPastBookings(providerId: String): Flow<List<ServiceRequest>> = callbackFlow {
    val sub = bookings
        .whereEqualTo("providerUid", providerId)
        .whereIn("status", listOf("completed", "declined"))
        // ── Remove .orderBy("completedAt") — sorts in memory instead ─────────
        .addSnapshotListener { snap, err ->
            if (err != null) {
                android.util.Log.e("BookingRepo", "getPastBookings error: ${err.message}")
                return@addSnapshotListener
            }
            if (snap == null) return@addSnapshotListener
            android.util.Log.d("BookingRepo", "Past bookings count: ${snap.documents.size}")
            launch {
                val sorted = withCustomerNames(
                    snap.documents.mapNotNull { it.toServiceRequest() }
                ).sortedByDescending { it.completedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                trySend(sorted)
            }
        }
        awaitClose { sub.remove() }
    }

    private suspend fun withCustomerNames(requests: List<ServiceRequest>): List<ServiceRequest> {
        if (requests.isEmpty()) return requests
        val ids = requests.map { it.customerId }.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return requests

        val names = mutableMapOf<String, String>()
        val profilesHomeAddresses = mutableMapOf<String, String>()

        ids.forEach { customerId ->
            runCatching {
                val doc = customerProfiles.document(customerId).get().await()

                val profileName = doc.getString("displayName")?.trim()?.takeIf { it.isNotBlank() }
                    ?: doc.getString("fullName")?.trim()?.takeIf { it.isNotBlank() }
                    ?: doc.getString("name")?.trim()?.takeIf { it.isNotBlank() }
                if (!profileName.isNullOrBlank()) {
                    names[customerId] = profileName
                } else {
                    val userName = users.document(customerId).get().await()
                        .getString("displayName")?.trim()?.takeIf { it.isNotBlank() }
                    if (!userName.isNullOrBlank()) names[customerId] = userName
                }

                val profileAddress = doc.getString("homeAddress")?.trim()?.takeIf { it.isNotBlank() }
                    ?: doc.getString("address")?.trim()?.takeIf { it.isNotBlank() }
                    ?: doc.getString("home_address")?.trim()?.takeIf { it.isNotBlank() }
                if (!profileAddress.isNullOrBlank()) {
                    profilesHomeAddresses[customerId] = profileAddress
                }
            }.getOrNull()
        }

        return requests.map { request ->
            val resolved = names[request.customerId]
            val profileHomeAddress = profilesHomeAddresses[request.customerId]
            val homeAddressResolved = request.homeAddress.takeIf { it.isNotBlank() }
                ?: profileHomeAddress.orEmpty()
            val neighborhoodResolved = request.neighborhood.takeIf { it.isNotBlank() }
                ?: profileHomeAddress.orEmpty()

            var enriched = request.copy(
                homeAddress = homeAddressResolved,
                neighborhood = neighborhoodResolved,
            )

            if (!resolved.isNullOrBlank()) {
                val initials = resolved
                    .split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .take(2)
                    .joinToString("")
                    .ifBlank { enriched.customerInitials }

                enriched = enriched.copy(
                    customerFirstName = resolved,
                    customerInitials = initials,
                )
            }

            enriched
        }
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