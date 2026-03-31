package com.smartserve.providerapp.ui.app

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import com.smartserve.sharedauth.AuthCollections
import com.smartserve.sharedauth.DefaultServiceAvailability
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.smartserve.providerapp.BuildConfig

@Singleton
class ProviderServicesRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val appContext: Context,
) {
    private companion object {
        const val TAG = "ProviderServicesRepo"
    }

    private val services get() = firestore.collection(AuthCollections.SERVICES)
    private val profiles get() = firestore.collection(AuthCollections.PROVIDER_PROFILES)
    private val imageKit by lazy { ImageKitUploader(appContext) }

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
        if (uris.isEmpty()) return Result.success(emptyList())
        val publicKey = BuildConfig.IMAGEKIT_PUBLIC_KEY.trim()
        val privateKey = BuildConfig.IMAGEKIT_PRIVATE_KEY.trim()
        if (publicKey.isBlank() || privateKey.isBlank()) {
            Log.w(
                TAG,
                "ImageKit keys missing in BuildConfig; add IMAGEKIT_PUBLIC_KEY / IMAGEKIT_PRIVATE_KEY " +
                    "to repo-root local.properties (rebuild). Skipping ${uris.size} photo upload(s).",
            )
            return Result.success(emptyList())
        }
        return runCatching {
            withContext(Dispatchers.IO) {
                val folder = "smartserve/services/$providerUid/$serviceId"

                uris.mapIndexed { index, uri ->
                    val fileName = "${System.currentTimeMillis()}_${index + 1}.${resolveFileExtension(uri)}"
                    imageKit.upload(
                        publicKey = publicKey,
                        privateKey = privateKey,
                        fileUri = uri,
                        folder = folder,
                        fileName = fileName,
                    )
                }
            }
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
        // With ImageKit (client-side uploads), we typically cannot delete files securely from the app
        // because deletion requires private credentials. We just remove the URL from Firestore.
        Log.d(TAG, "Skipping remote delete for photoUrl=$url")
    }
}
