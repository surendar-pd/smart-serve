package com.smartserve.customerapp.ui.app

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.smartserve.sharedauth.AuthCollections
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonalizationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    private val customerProfiles get() = firestore.collection(AuthCollections.CUSTOMER_PROFILES)
    private val bookings get() = firestore.collection(AuthCollections.BOOKINGS)
    private val providerProfiles get() = firestore.collection(AuthCollections.PROVIDER_PROFILES)

    /**
     * Increments the tap count for a category in the customer's profile subcollection.
     * Silently ignored when not signed in.
     */
    suspend fun recordCategoryTap(categoryId: String) {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            customerProfiles
                .document(uid)
                .collection("category_taps")
                .document(categoryId)
                .set(
                    mapOf("count" to FieldValue.increment(1)),
                    SetOptions.merge(),
                ).await()
        }
    }

    /**
     * Returns a map of categoryId → tap count for the current customer.
     * Returns empty map when not signed in or on error.
     */
    suspend fun getCategoryTapCounts(): Map<String, Int> {
        val uid = auth.currentUser?.uid ?: return emptyMap()
        return runCatching {
            customerProfiles
                .document(uid)
                .collection("category_taps")
                .get().await()
                .documents
                .associate { doc ->
                    doc.id to (doc.getLong("count")?.toInt() ?: 0)
                }
        }.getOrDefault(emptyMap())
    }

    /**
     * Returns the set of provider UIDs that the customer has previously completed a booking with.
     * Returns empty set when not signed in or on error.
     */
    suspend fun getPreviouslyBookedProviderIds(): Set<String> {
        val uid = auth.currentUser?.uid ?: return emptySet()
        return runCatching {
            val customerRef = customerProfiles.document(uid)
            bookings
                .whereEqualTo("customer", customerRef)
                .whereEqualTo("status", "completed")
                .get().await()
                .documents
                .mapNotNull { doc ->
                    val provRef = doc.get("provider")
                    when (provRef) {
                        is com.google.firebase.firestore.DocumentReference -> provRef.id
                        else -> null
                    }
                }
                .toSet()
        }.getOrDefault(emptySet())
    }
}
