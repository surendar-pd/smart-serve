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

            // Try to resolve a name for each provider.
            // 1. profile.displayName (fast path – usually populated for new accounts)
            // 2. title of the onboarding service in THIS query (doc.id == providerUid)
            // 3. Separate fetch of services/{uid} for the onboarding service when it lives
            //    in a different category (e.g. provider onboarded as "Plumbing", customer
            //    browses "Electrical").

            // Build a quick map from docs already in this query
            val inQueryOnboardingTitle: Map<String, String> = activeDocs
                .mapNotNull { doc ->
                    val provId = doc.getDocumentReference("provider")?.id ?: return@mapNotNull null
                    val title = doc.getString("title")?.trim().orEmpty()
                    if (doc.id == provId && title.isNotBlank() && title != "Service")
                        provId to title
                    else null
                }.toMap()

            // Index active service docs by provider UID for quick lookup
            val serviceDocsByProvider: Map<String, List<com.google.firebase.firestore.DocumentSnapshot>> =
                activeDocs.groupBy { it.getDocumentReference("provider")?.id ?: "" }

            activeProviderIds.mapNotNull { uid ->
                val profileDoc = profiles.document(uid).get().await()
                val profileName = profileDoc.getString("displayName")?.trim()
                    ?.takeIf { it.isNotBlank() }

                val fallbackName = if (profileName.isNullOrBlank()) {
                    inQueryOnboardingTitle[uid]?.takeIf { it.isNotBlank() }
                        ?: try {
                            services.document(uid).get().await()
                                .getString("title")?.trim()
                                ?.takeIf { it.isNotBlank() && it != "Service" }
                        } catch (_: Exception) { null }
                        ?: ""
                } else ""

                // Use the first active service in this category for rate & availability
                val svcDoc = serviceDocsByProvider[uid]?.firstOrNull()
                val categoryRate = svcDoc?.getDouble("hourlyRate")
                    ?: svcDoc?.getLong("hourlyRate")?.toDouble() ?: 0.0
                val categoryDays = (svcDoc?.get("availabilityDays") as? List<*>)
                    ?.mapNotNull { it?.toString() } ?: emptyList()
                val categoryStart = svcDoc?.getString("availabilityStart") ?: ""
                val categoryEnd   = svcDoc?.getString("availabilityEnd")   ?: ""

                profileDoc.toCustomerProviderSummary(fallbackName = fallbackName)?.copy(
                    categoryServiceRate      = categoryRate,
                    categoryAvailabilityDays = categoryDays,
                    categoryAvailabilityStart = categoryStart,
                    categoryAvailabilityEnd   = categoryEnd,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "getProvidersByCategory failed", e)
            emptyList()
        }
    }

    /**
     * Returns services for a given provider, optionally filtered to a specific category.
     * Both filters are applied in-memory to avoid composite Firestore index requirements.
     */
    suspend fun getServicesForProvider(
        providerUid: String,
        providerName: String,
        categoryId: String = "",
    ): List<CustomerServiceListing> {
        return try {
            val providerRef = profiles.document(providerUid)
            // Single-field equality only → no composite index required
            val snap = services
                .whereEqualTo("provider", providerRef)
                .get().await()

            Log.d(TAG, "getServicesForProvider($providerUid, cat=$categoryId): ${snap.size()} docs total")

            snap.documents
                .filter { doc ->
                    val active = doc.getBoolean("isActive") != false
                    val matchesCat = categoryId.isEmpty() ||
                        doc.getDocumentReference("category")?.id == categoryId
                    active && matchesCat
                }
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

            // Build name fallback map from services already fetched (onboarding doc has id == uid)
            val inQueryOnboardingTitle: Map<String, String> = snap.documents
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

                val name = profileName
                    ?: inQueryOnboardingTitle[uid]?.takeIf { it.isNotBlank() }
                    // getAllActiveServices fetches ALL services, so if the onboarding service
                    // exists it will be in snap.documents already — no extra fetch needed here.
                    ?: "Provider ${uid.take(6)}"

                uid to name
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
     * For providers with a blank displayName, falls back to the title of their
     * onboarding service document (doc.id == providerUid).
     */
    suspend fun getTopProviders(limit: Int = 5): List<CustomerProviderSummary> {
        return try {
            // Fetch all provider profiles (no orderBy → no index required)
            val snap = profiles.limit(50).get().await()
            Log.d(TAG, "getTopProviders: ${snap.size()} profiles fetched")

            // For any profile with a blank displayName, look up their onboarding service
            // (document ID == provider UID) to use its title as the name.
            val blankNameUids = snap.documents
                .filter { doc -> doc.getString("displayName").isNullOrBlank() }
                .map { it.id }

            val onboardingTitleByUid: Map<String, String> = blankNameUids.associate { uid ->
                val title = try {
                    services.document(uid).get().await()
                        .getString("title")?.trim().orEmpty()
                        .let { if (it == "Service") "" else it }
                } catch (_: Exception) { "" }
                uid to title
            }

            snap.documents
                .mapNotNull { it.toCustomerProviderSummary(fallbackName = onboardingTitleByUid[it.id] ?: "") }
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
 * Falls back to a short UID-based placeholder so providers always appear.
 */
private fun com.google.firebase.firestore.DocumentSnapshot.toCustomerProviderSummary(
    fallbackName: String = "",
): CustomerProviderSummary? {
    val uid = id.ifBlank { return null }
    val displayName = getString("displayName")?.trim()?.takeIf { it.isNotBlank() }
        ?: fallbackName.takeIf { it.isNotBlank() }
        ?: getString("name")?.trim()?.takeIf { it.isNotBlank() }
        ?: "Provider ${uid.take(6)}"
    return CustomerProviderSummary(
        uid = uid,
        displayName = displayName,
        avgRating = getDouble("avgRating") ?: getLong("avgRating")?.toDouble() ?: 0.0,
        totalReviews = getLong("totalReviews")?.toInt() ?: 0,
        serviceDescription = getString("serviceDescription").orEmpty(),
        hourlyRate = getDouble("hourlyRate") ?: getLong("hourlyRate")?.toDouble() ?: 0.0,
    )
}
