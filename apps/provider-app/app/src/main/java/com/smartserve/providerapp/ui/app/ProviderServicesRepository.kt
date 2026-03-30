package com.smartserve.providerapp.ui.app

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageException
import dagger.hilt.android.qualifiers.ApplicationContext
import com.smartserve.sharedauth.AuthCollections
import com.smartserve.sharedauth.DefaultServiceAvailability
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderServicesRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    @ApplicationContext private val appContext: Context,
) {
    private companion object {
        const val TAG = "ProviderServicesRepo"
    }

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
            val existingCount = services.whereEqualTo("provider", providerRef).get().await().size()
            if (existingCount >= 3) {
                return Result.failure(IllegalStateException("You can add up to 3 services only"))
            }
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
                "photoUrls" to draft.photoUrls,
                "serviceCenter" to draft.serviceCenter,
                "serviceRadiusKm" to draft.serviceRadiusKm,
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
                "photoUrls" to draft.photoUrls,
                "updatedAt" to Timestamp.now(),
                "phone" to FieldValue.delete(),
            )
            draft.serviceCenter?.let { updates["serviceCenter"] = it }
            draft.serviceRadiusKm?.let { updates["serviceRadiusKm"] = it }
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

    suspend fun uploadServicePhotos(
        providerUid: String,
        serviceId: String,
        uris: List<Uri>,
    ): Result<List<String>> {
        return try {
            val urls = mutableListOf<String>()
            uris.forEachIndexed { index, uri ->
                val extension = resolveFileExtension(uri)
                val path = "services/$providerUid/$serviceId/${System.currentTimeMillis()}_${index + 1}.$extension"
                val ref = storage.reference.child(path)
                val meta = StorageMetadata.Builder()
                    .setContentType(appContext.contentResolver.getType(uri) ?: "image/jpeg")
                    .build()
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    ref.putStream(input, meta).await()
                } ?: return Result.failure(
                    IllegalStateException("Could not open selected image URI for upload: $uri")
                )

                val exists = runCatching { ref.metadata.await() }.getOrNull()
                if (exists == null) {
                    val bucket = storage.reference.bucket
                    Log.e(TAG, "Upload metadata check failed. bucket=$bucket path=$path uri=$uri")
                    return Result.failure(
                        IllegalStateException(
                            "Upload did not become readable at '$path' in bucket '$bucket'. Check Firebase Storage bucket/rules."
                        )
                    )
                }

                // Some environments can return a transient "object does not exist" immediately
                // after upload; retry downloadUrl retrieval a few times before failing.
                var lastError: Exception? = null
                var download: String? = null
                repeat(5) { attempt ->
                    val fetched = runCatching { ref.downloadUrl.await().toString() }
                    if (fetched.isSuccess) {
                        download = fetched.getOrNull()
                        return@repeat
                    }
                    lastError = fetched.exceptionOrNull() as? Exception
                    if (attempt < 4) delay(500)
                }

                if (download == null) {
                    val code = (lastError as? StorageException)?.errorCode
                    val message = lastError?.localizedMessage ?: "Unknown storage error"
                    val bucket = storage.reference.bucket
                    Log.e(TAG, "Download URL fetch failed. bucket=$bucket path=$path code=$code msg=$message")
                    return Result.failure(
                        IllegalStateException(
                            "Image uploaded but URL fetch failed for '$path' in '$bucket' (code=$code): $message. Storage read rules may be blocking URL retrieval."
                        )
                    )
                }
                urls += download!!
            }
            Result.success(urls)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun diagnoseStorageAccess(providerUid: String): Result<Unit> {
        return try {
            val path = "services/$providerUid/_diag/${System.currentTimeMillis()}.txt"
            val ref = storage.reference.child(path)
            ref.putBytes("ping".toByteArray()).await()
            runCatching { ref.delete().await() }
            Result.success(Unit)
        } catch (e: Exception) {
            val se = e as? StorageException
            val code = se?.errorCode
            val bucket = storage.reference.bucket
            val message = e.localizedMessage ?: "Unknown storage error"
            Log.e(TAG, "Storage diagnostic failed. bucket=$bucket code=$code msg=$message", e)
            Result.failure(
                IllegalStateException(
                    "Storage write test failed for bucket '$bucket' (code=$code): $message"
                )
            )
        }
    }

    private fun resolveFileExtension(uri: Uri): String {
        val mime = appContext.contentResolver.getType(uri)
        val fromMime = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
        if (!fromMime.isNullOrBlank()) return fromMime
        val path = uri.toString().substringAfterLast('.', "").lowercase()
        return if (path.isNotBlank()) path else "jpg"
    }

    suspend fun deleteServicePhoto(url: String) {
        runCatching { storage.getReferenceFromUrl(url).delete().await() }
    }
}
