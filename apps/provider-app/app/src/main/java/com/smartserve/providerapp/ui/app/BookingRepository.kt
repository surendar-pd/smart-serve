package com.smartserve.providerapp.ui.app

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.smartserve.sharedauth.AuthCollections
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val services get() = firestore.collection(AuthCollections.SERVICES)
    private val categories get() = firestore.collection(AuthCollections.CATEGORIES)
    private val providerProfiles get() = firestore.collection(AuthCollections.PROVIDER_PROFILES)

    // Real-time stream: new, pending, active requests for this provider
    fun getIncomingRequests(providerId: String): Flow<List<ServiceRequest>> = callbackFlow {
        val providerRef = providerProfiles.document(providerId)
        val allowed = setOf("new", "pending", "active", "completed", "confirmed", "CONFIRMED")

        val sub = bookings
            .whereEqualTo("provider", providerRef)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) {
                    android.util.Log.e("BookingRepo", "getIncomingRequests error: ${err?.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                launch {
                    val parsed = snap.documents
                        .mapNotNull { it.toServiceRequest() }
                        .filter { it.status.value in allowed }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                    trySend(withCategoryLabels(withServiceTitles(withCustomerNames(parsed))))
                }
            }

        awaitClose { sub.remove() }
    }.distinctUntilChanged()

    // Real-time stream: completed and declined, newest first
    fun getPastBookings(providerId: String): Flow<List<ServiceRequest>> = callbackFlow {
        val providerRef = providerProfiles.document(providerId)
        val allowed = setOf("completed", "declined")

        val sub = bookings
            .whereEqualTo("provider", providerRef)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) {
                    android.util.Log.e("BookingRepo", "getPastBookings error: ${err?.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                launch {
                    val parsed = snap.documents
                        .mapNotNull { it.toServiceRequest() }
                        .filter { it.status.value in allowed }
                        .sortedByDescending { it.completedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                    trySend(withCategoryLabels(withServiceTitles(withCustomerNames(parsed))))
                }
            }

        awaitClose { sub.remove() }
    }.distinctUntilChanged()

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
                if (!profileName.isNullOrBlank()) {
                    names[customerId] = profileName
                } else {
                    val userDoc = users.document(customerId).get().await()
                    val userName = userDoc.getString("displayName")?.trim()?.takeIf { it.isNotBlank() }
                    if (!userName.isNullOrBlank()) names[customerId] = userName
                }

                doc.getString("homeAddress")?.trim()?.takeIf { it.isNotBlank() }?.let { addr ->
                    profilesHomeAddresses[customerId] = addr
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

    private suspend fun withServiceTitles(requests: List<ServiceRequest>): List<ServiceRequest> {
        if (requests.isEmpty()) return requests
        val ids = requests.map { it.serviceId }.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return requests

        val titles = mutableMapOf<String, String>()
        ids.forEach { serviceId ->
            runCatching {
                val title = services.document(serviceId).get().await()
                    .getString("title")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                if (!title.isNullOrBlank()) titles[serviceId] = title
            }
        }

        return requests.map { req ->
            val title = titles[req.serviceId].orEmpty()
            if (title.isNotBlank() && req.serviceType.isBlank()) req.copy(serviceType = title) else req
        }
    }

    private suspend fun withCategoryLabels(requests: List<ServiceRequest>): List<ServiceRequest> {
        if (requests.isEmpty()) return requests
        val ids = requests
            .map { it.categoryId.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (ids.isEmpty()) return requests

        val labels = mutableMapOf<String, String>()
        ids.forEach { categoryId ->
            runCatching {
                val label = categories.document(categoryId).get().await()
                    .getString("label")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                if (!label.isNullOrBlank()) labels[categoryId] = label
            }
        }

        return requests.map { req ->
            val label = labels[req.categoryId].orEmpty()
            if (label.isNotBlank() && req.categoryLabel.isBlank()) req.copy(categoryLabel = label) else req
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