package com.smartserve.providerapp.ui.app

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.smartserve.sharedauth.AuthCollections
import com.smartserve.sharedauth.DefaultServiceAvailability
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderServicesRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    private val services get() = firestore.collection(AuthCollections.SERVICES)
    private val profiles get() = firestore.collection(AuthCollections.PROVIDER_PROFILES)

    fun observeServicesForProvider(providerUid: String): Flow<List<ProviderServiceRow>> = callbackFlow {
        val providerRef = profiles.document(providerUid)
        val sub = services
            .whereEqualTo("provider", providerRef)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val rows = snap.documents.mapNotNull { it.toProviderServiceRow() }
                    .sortedBy { it.title.lowercase() }
                trySend(rows)
            }
        awaitClose { sub.remove() }
    }

    suspend fun getService(serviceId: String): ProviderServiceRow? =
        services.document(serviceId).get().await().takeIf { it.exists() }?.toProviderServiceRow()

    private suspend fun documentOwnedByProvider(providerUid: String, serviceId: String): Boolean {
        val doc = services.document(serviceId).get().await()
        if (!doc.exists()) return false
        val p = doc.getDocumentReference("provider") ?: return false
        return p.id == providerUid
    }

    suspend fun createService(providerUid: String, draft: ServiceDraft): Result<String> {
        return try {
            val providerRef = profiles.document(providerUid)
            val categoryRef = firestore.collection(AuthCollections.CATEGORIES).document(draft.categoryId)
            val days = draft.availabilityDays.ifEmpty { DefaultServiceAvailability.DAYS }
            val start = draft.availabilityStart.ifBlank { DefaultServiceAvailability.START }
            val end = draft.availabilityEnd.ifBlank { DefaultServiceAvailability.END }
            val data = mapOf(
                "provider" to providerRef,
                "category" to categoryRef,
                "title" to draft.title.trim(),
                "description" to draft.description.trim(),
                "hourlyRate" to draft.hourlyRate,
                "isActive" to draft.isActive,
                "availabilityDays" to days,
                "availabilityStart" to start,
                "availabilityEnd" to end,
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now(),
            )
            val ref = services.add(data).await()
            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateService(providerUid: String, serviceId: String, draft: ServiceDraft): Result<Unit> {
        return try {
            if (!documentOwnedByProvider(providerUid, serviceId)) {
                return Result.failure(SecurityException("Not allowed to edit this service"))
            }
            val categoryRef = firestore.collection(AuthCollections.CATEGORIES).document(draft.categoryId)
            val days = draft.availabilityDays.ifEmpty { DefaultServiceAvailability.DAYS }
            val start = draft.availabilityStart.ifBlank { DefaultServiceAvailability.START }
            val end = draft.availabilityEnd.ifBlank { DefaultServiceAvailability.END }
            val updates = hashMapOf<String, Any>(
                "category" to categoryRef,
                "title" to draft.title.trim(),
                "description" to draft.description.trim(),
                "hourlyRate" to draft.hourlyRate,
                "isActive" to draft.isActive,
                "availabilityDays" to days,
                "availabilityStart" to start,
                "availabilityEnd" to end,
                "updatedAt" to Timestamp.now(),
                "phone" to FieldValue.delete(),
                "serviceRadiusKm" to FieldValue.delete(),
                "serviceCenter" to FieldValue.delete(),
            )
            services.document(serviceId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteService(providerUid: String, serviceId: String): Result<Unit> {
        return try {
            if (!documentOwnedByProvider(providerUid, serviceId)) {
                return Result.failure(SecurityException("Not allowed to delete this service"))
            }
            services.document(serviceId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
