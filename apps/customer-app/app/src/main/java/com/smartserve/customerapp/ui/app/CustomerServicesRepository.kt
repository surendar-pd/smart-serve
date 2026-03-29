package com.smartserve.customerapp.ui.app

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.smartserve.sharedauth.AuthCollections
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerServicesRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    private val services get() = firestore.collection(AuthCollections.SERVICES)
    private val profiles get() = firestore.collection(AuthCollections.PROVIDER_PROFILES)
    private val categories get() = firestore.collection(AuthCollections.CATEGORIES)

    suspend fun getProvidersByCategory(categoryId: String): List<CustomerProviderSummary> {
        return try {
            val categoryRef = categories.document(categoryId)
            val snap = services
                .whereEqualTo("category", categoryRef)
                .whereEqualTo("isActive", true)
                .get().await()

            val providerIds = snap.documents
                .mapNotNull { it.getDocumentReference("provider")?.id }
                .distinct()

            providerIds.mapNotNull { uid ->
                profiles.document(uid).get().await().toCustomerProviderSummary()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getServicesForProvider(
        providerUid: String,
        providerName: String,
    ): List<CustomerServiceListing> {
        return try {
            val providerRef = profiles.document(providerUid)
            val snap = services
                .whereEqualTo("provider", providerRef)
                .whereEqualTo("isActive", true)
                .get().await()

            snap.documents.mapNotNull { doc ->
                val title = doc.getString("title")?.trim().orEmpty().ifBlank { return@mapNotNull null }
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
            emptyList()
        }
    }

    suspend fun getAllActiveServices(): List<CustomerServiceListing> {
        return try {
            val snap = services
                .whereEqualTo("isActive", true)
                .get().await()

            // Batch fetch unique provider names
            val providerIds = snap.documents
                .mapNotNull { it.getDocumentReference("provider")?.id }
                .distinct()
            val providerNames = providerIds.associate { uid ->
                uid to (profiles.document(uid).get().await().getString("displayName") ?: "")
            }

            snap.documents.mapNotNull { doc ->
                val providerUid = doc.getDocumentReference("provider")?.id ?: return@mapNotNull null
                val title = doc.getString("title")?.trim().orEmpty().ifBlank { return@mapNotNull null }
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
            emptyList()
        }
    }

    suspend fun getTopProviders(limit: Int = 5): List<CustomerProviderSummary> {
        return try {
            profiles
                .orderBy("avgRating", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get().await()
                .documents
                .mapNotNull { it.toCustomerProviderSummary() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toCustomerProviderSummary(): CustomerProviderSummary? {
    val uid = id.ifBlank { return null }
    val displayName = getString("displayName")?.trim().orEmpty().ifBlank { return null }
    return CustomerProviderSummary(
        uid = uid,
        displayName = displayName,
        avgRating = getDouble("avgRating") ?: getLong("avgRating")?.toDouble() ?: 0.0,
        totalReviews = getLong("totalReviews")?.toInt() ?: 0,
        serviceDescription = getString("serviceDescription").orEmpty(),
    )
}
