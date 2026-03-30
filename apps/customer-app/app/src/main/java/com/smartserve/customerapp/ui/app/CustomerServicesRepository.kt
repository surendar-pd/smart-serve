package com.smartserve.customerapp.ui.app

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.smartserve.sharedauth.AuthCollections
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CustomerServicesRepo"

@Singleton
class CustomerServicesRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    private val services get() = firestore.collection(AuthCollections.SERVICES)
    private val profiles get() = firestore.collection(AuthCollections.PROVIDER_PROFILES)
    private val categories get() = firestore.collection(AuthCollections.CATEGORIES)

    /**
     * Returns providers who have at least one active service in [categoryId].
     * Filters isActive in-memory to avoid requiring a composite Firestore index.
     */
    suspend fun getProvidersByCategory(categoryId: String): List<CustomerProviderSummary> {
        return try {
            val categoryRef = categories.document(categoryId)
            // Single-field equality only → no composite index required
            val snap = services
                .whereEqualTo("category", categoryRef)
                .get().await()

            val activeDocs = snap.documents.filter { it.getBoolean("isActive") != false }

            val activeProviderIds = activeDocs
                .mapNotNull { it.getDocumentReference("provider")?.id }
                .distinct()

            Log.d(TAG, "getProvidersByCategory($categoryId): ${snap.size()} docs, ${activeProviderIds.size} active providers")

            // The onboarding service has doc.id == providerUid and title = provider's display name.
            // Use it as a fallback when provider_profiles.displayName is blank.
            val onboardingTitleByUid: Map<String, String> = activeDocs
                .mapNotNull { doc ->
                    val provId = doc.getDocumentReference("provider")?.id ?: return@mapNotNull null
                    val title = doc.getString("title")?.trim().orEmpty()
                    if (doc.id == provId && title.isNotBlank() && title != "Service")
                        provId to title
                    else null
                }.toMap()

            activeProviderIds.mapNotNull { uid ->
                profiles.document(uid).get().await()
                    .toCustomerProviderSummary(fallbackName = onboardingTitleByUid[uid] ?: "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "getProvidersByCategory failed", e)
            emptyList()
        }
    }

    /**
     * Returns all services for a given provider.
     * Filters isActive in-memory to avoid requiring a composite Firestore index.
     */
    suspend fun getServicesForProvider(
        providerUid: String,
        providerName: String,
    ): List<CustomerServiceListing> {
        return try {
            val providerRef = profiles.document(providerUid)
            // Single-field equality only → no composite index required
            val snap = services
                .whereEqualTo("provider", providerRef)
                .get().await()

            Log.d(TAG, "getServicesForProvider($providerUid): ${snap.size()} docs total")

            snap.documents
                .filter { it.getBoolean("isActive") != false }   // treat missing as true
                .mapNotNull { doc ->
                    val title = doc.getString("title")?.trim().orEmpty()
                        .ifBlank { return@mapNotNull null }
                    CustomerServiceListing(
                        serviceId = doc.id,
                        title = title,
                        description = doc.getString("description").orEmpty(),
                        hourlyRate = doc.getDouble("hourlyRate")
                            ?: doc.getLong("hourlyRate")?.toDouble() ?: 0.0,
                        providerUid = providerUid,
                        providerName = providerName,
                        availabilityDays = (doc.get("availabilityDays") as? List<*>)
                            ?.mapNotNull { it?.toString() } ?: emptyList(),
                        availabilityStart = doc.getString("availabilityStart") ?: "09:00",
                        availabilityEnd = doc.getString("availabilityEnd") ?: "18:00",
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "getServicesForProvider failed", e)
            emptyList()
        }
    }

    /**
     * Returns all services for the search screen.
     * Fetches all services and filters isActive in-memory so docs without the field
     * (e.g. written before isActive was added) are also included.
     */
    suspend fun getAllActiveServices(): List<CustomerServiceListing> {
        return try {
            // No filter → no index required; missing isActive treated as active
            val snap = services.get().await()

            Log.d(TAG, "getAllActiveServices: ${snap.size()} docs")

            // Batch fetch unique provider names
            val providerIds = snap.documents
                .mapNotNull { it.getDocumentReference("provider")?.id }
                .distinct()

            // Onboarding service has doc.id == providerUid; its title = provider's display name
            val onboardingTitleByUid: Map<String, String> = snap.documents
                .mapNotNull { doc ->
                    val provId = doc.getDocumentReference("provider")?.id ?: return@mapNotNull null
                    val title = doc.getString("title")?.trim().orEmpty()
                    if (doc.id == provId && title.isNotBlank() && title != "Service")
                        provId to title
                    else null
                }.toMap()

            val providerNames = providerIds.associate { uid ->
                val profileName = profiles.document(uid).get().await().getString("displayName")
                    ?.trim()?.takeIf { it.isNotBlank() }
                uid to (profileName ?: onboardingTitleByUid[uid] ?: "")
            }

            snap.documents
                .filter { it.getBoolean("isActive") != false }   // treat missing as active
                .mapNotNull { doc ->
                val providerUid = doc.getDocumentReference("provider")?.id
                    ?: return@mapNotNull null
                val title = doc.getString("title")?.trim().orEmpty()
                    .ifBlank { return@mapNotNull null }
                CustomerServiceListing(
                    serviceId = doc.id,
                    title = title,
                    description = doc.getString("description").orEmpty(),
                    hourlyRate = doc.getDouble("hourlyRate")
                        ?: doc.getLong("hourlyRate")?.toDouble() ?: 0.0,
                    providerUid = providerUid,
                    providerName = providerNames[providerUid] ?: "",
                    availabilityDays = (doc.get("availabilityDays") as? List<*>)
                        ?.mapNotNull { it?.toString() } ?: emptyList(),
                    availabilityStart = doc.getString("availabilityStart") ?: "09:00",
                    availabilityEnd = doc.getString("availabilityEnd") ?: "18:00",
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "getAllActiveServices failed", e)
            emptyList()
        }
    }

    /**
     * Returns top providers sorted by avgRating.
     * Sorts in-memory to avoid requiring a Firestore index and to include
     * providers whose avgRating field may not yet be set.
     */
    suspend fun getTopProviders(limit: Int = 5): List<CustomerProviderSummary> {
        return try {
            // Fetch all provider profiles (no orderBy → no index required, includes new providers)
            val snap = profiles.limit(50).get().await()
            Log.d(TAG, "getTopProviders: ${snap.size()} profiles fetched")
            snap.documents
                .mapNotNull { it.toCustomerProviderSummary() }
                .sortedByDescending { it.avgRating }
                .take(limit)
        } catch (e: Exception) {
            Log.e(TAG, "getTopProviders failed", e)
            emptyList()
        }
    }
}

/**
 * [fallbackName] is used when [displayName] field is blank — callers can pass the
 * onboarding service title (service doc where doc.id == providerUid) as the name.
 */
private fun com.google.firebase.firestore.DocumentSnapshot.toCustomerProviderSummary(
    fallbackName: String = "",
): CustomerProviderSummary? {
    val uid = id.ifBlank { return null }
    val displayName = getString("displayName")?.trim()?.takeIf { it.isNotBlank() }
        ?: fallbackName.takeIf { it.isNotBlank() }
        ?: getString("email")?.substringBefore("@")?.takeIf { it.isNotBlank() }
        ?: return null   // Don't surface a provider whose name we cannot resolve at all
    return CustomerProviderSummary(
        uid = uid,
        displayName = displayName,
        avgRating = getDouble("avgRating") ?: getLong("avgRating")?.toDouble() ?: 0.0,
        totalReviews = getLong("totalReviews")?.toInt() ?: 0,
        serviceDescription = getString("serviceDescription").orEmpty(),
    )
}
