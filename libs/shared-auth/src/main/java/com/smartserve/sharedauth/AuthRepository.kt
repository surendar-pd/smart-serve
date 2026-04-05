package com.smartserve.sharedauth

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    val currentUser: FirebaseUser? get() = auth.currentUser

    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        fullName: String,
        role: String,
        phone: String? = null
    ): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return AuthResult.Error("User creation failed")

            firebaseUser.sendEmailVerification().await()

            val resolvedRole =
                if (role == UserRole.PROVIDER.value) UserRole.PROVIDER.value else UserRole.CUSTOMER.value
            syncDisplayNameToAuth(firebaseUser, fullName)
            commitFirestoreUser(firebaseUser.uid, resolvedRole, displayName = fullName)
            // Seed provider_profiles on sign-up; full listings are added from the provider app.
            if (resolvedRole == UserRole.PROVIDER.value) {
                val profileSeed = mutableMapOf<String, Any>(
                    "displayName" to fullName.trim()
                )
                if (!phone.isNullOrBlank()) profileSeed["phone"] = phone
                firestore.collection(AuthCollections.PROVIDER_PROFILES).document(firebaseUser.uid)
                    .set(profileSeed, SetOptions.merge())
                    .await()
            }

            AuthResult.Success(auth.currentUser ?: firebaseUser)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Sign up failed")
        }
    }

    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return AuthResult.Error("Login failed")
            AuthResult.Success(firebaseUser)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Login failed")
        }
    }

    suspend fun signInWithGoogle(idToken: String, role: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: return AuthResult.Error("Google sign-in failed")

            if (result.additionalUserInfo?.isNewUser == true) {
                val resolvedRole =
                    if (role == UserRole.PROVIDER.value) UserRole.PROVIDER.value else UserRole.CUSTOMER.value
                commitFirestoreUser(
                    firebaseUser.uid,
                    resolvedRole,
                    displayName = firebaseUser.displayName?.trim().orEmpty(),
                )
            }
            AuthResult.Success(firebaseUser)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Google sign-in failed")
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isProfileSetupComplete(uid: String, role: String): Boolean {
    return try {
        if (role == "customer") {
            val doc = firestore
                .collection(AuthCollections.CUSTOMER_PROFILES)
                .document(uid)
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()
            doc.exists() && doc.getString("homeAddress")?.isNotBlank() == true
        } else {
            val doc = firestore
                .collection(AuthCollections.PROVIDER_PROFILES)
                .document(uid)
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()
            doc.exists() && doc.getString("displayName")?.isNotBlank() == true
        }
    } catch (e: Exception) {
        false
    }
}

    suspend fun getUserRole(uid: String): String {
    return try {
        val doc = firestore.collection(AuthCollections.USERS)
            .document(uid)
            .get(com.google.firebase.firestore.Source.SERVER)
            .await()
        doc.getString("role") ?: "customer"
    } catch (e: Exception) {
        "customer"
    }
}

    suspend fun listServiceCategories(): Result<List<ServiceCategoryOption>> {
        return try {
            val snap = firestore.collection(AuthCollections.CATEGORIES).get().await()
            val list = snap.documents.mapNotNull { doc ->
                val label = doc.getString("label")?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                ServiceCategoryOption(id = doc.id, label = label)
            }.sortedBy { it.label.lowercase() }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Adds a document to [AuthCollections.CATEGORIES] with `label` and `createdAt`.
     * Returns the new document id for use as [ProviderServiceProfile.serviceCategory].
     */
    suspend fun addServiceCategory(label: String): Result<ServiceCategoryOption> {
        val trimmed = label.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Category name is required"))
        }
        return try {
            val data = mapOf(
                "label" to trimmed,
                "createdAt" to Timestamp.now(),
            )
            val ref = firestore.collection(AuthCollections.CATEGORIES).add(data).await()
            Result.success(ServiceCategoryOption(id = ref.id, label = trimmed))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveCustomerProfile(
        uid: String,
        phone: String?,
        homeAddress: String,
        locationAwareness: Boolean,
        pushNotifications: Boolean,
        photoUrl: String?
    ): Result<Unit> {
        return try {
            val displayName = auth.currentUser
                ?.takeIf { it.uid == uid }
                ?.displayName
                ?.trim()
                .orEmpty()
            val prefs = CustomerProfile(
                uid = uid,
                phone = phone?.takeIf { it.isNotBlank() },
                homeAddress = homeAddress,
                locationAwareness = locationAwareness,
                pushNotifications = pushNotifications,
                smartSuggestions = true
            )
            val data = mutableMapOf<String, Any>(
                "homeAddress" to prefs.homeAddress,
                "locationAwareness" to prefs.locationAwareness,
                "pushNotifications" to prefs.pushNotifications,
                "smartSuggestions" to prefs.smartSuggestions,
            )
            if (!prefs.phone.isNullOrBlank()) data["phone"] = prefs.phone
            if (displayName.isNotBlank()) data["displayName"] = displayName
            firestore.collection(AuthCollections.CUSTOMER_PROFILES).document(uid)
                .set(data, SetOptions.merge())
                .await()
            syncAuthProfileFromStrings(uid, displayName = null, photoUrl = photoUrl)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveProviderProfile(
        uid: String,
        displayName: String,
        phone: String,
        photoUrl: String?,
        serviceCategory: String,
        serviceDescription: String,
        hourlyRate: Double,
        serviceCenter: com.google.firebase.firestore.GeoPoint?,
        serviceRadiusKm: Double,
    ): Result<Unit> {
        return try {
            val existing = firestore.collection(AuthCollections.PROVIDER_PROFILES).document(uid).get().await()
            val resolvedDisplayName = displayName.takeIf { it.isNotBlank() }
                ?: auth.currentUser?.takeIf { it.uid == uid }?.displayName
                ?: existing.getString("displayName").orEmpty()
            val resolvedPhone = phone.takeIf { it.isNotBlank() }
                ?: existing.getString("phone").orEmpty()

            syncAuthProfileFromStrings(
                uid,
                displayName = resolvedDisplayName.takeIf { it.isNotBlank() },
                photoUrl = photoUrl
            )

            val providerDocRef = firestore.collection(AuthCollections.PROVIDER_PROFILES).document(uid)

            val providerCore = mutableMapOf<String, Any>(
                "displayName" to resolvedDisplayName,
                "phone" to resolvedPhone,
                "updatedAt" to Timestamp.now(),
            )
            if (!existing.exists()) {
                providerCore["createdAt"] = Timestamp.now()
            }

            resolvedDisplayName.trim().takeIf { it.isNotBlank() }?.let { name ->
                firestore.collection(AuthCollections.USERS).document(uid)
                    .set(mapOf("displayName" to name, "updatedAt" to Timestamp.now()), SetOptions.merge())
                    .await()
            }

            if (serviceCategory.isBlank()) {
                providerDocRef.set(providerCore, SetOptions.merge()).await()
            } else {
                val categoryDocRef = firestore.collection(AuthCollections.CATEGORIES).document(serviceCategory)
                val serviceDocRef = firestore.collection(AuthCollections.SERVICES).document(uid)
                val serviceSnap = serviceDocRef.get().await()
                val title = resolvedDisplayName.trim().ifBlank { "Service" }
                val serviceListing = mutableMapOf<String, Any>(
                    "provider" to providerDocRef,
                    "category" to categoryDocRef,
                    "title" to title,
                    "description" to serviceDescription,
                    "hourlyRate" to hourlyRate,
                    "isActive" to true,
                    "availabilityDays" to DefaultServiceAvailability.DAYS,
                    "availabilityStart" to DefaultServiceAvailability.START,
                    "availabilityEnd" to DefaultServiceAvailability.END,
                    "updatedAt" to Timestamp.now(),
                )
                serviceCenter?.let { serviceListing["serviceCenter"] = it }
                serviceListing["serviceRadiusKm"] = serviceRadiusKm
                if (!serviceSnap.exists()) {
                    serviceListing["createdAt"] = Timestamp.now()
                }

                val batch = firestore.batch()
                batch.set(providerDocRef, providerCore, SetOptions.merge())
                batch.set(
                    providerDocRef,
                    mapOf("category" to categoryDocRef),
                    SetOptions.merge(),
                )
                batch.set(serviceDocRef, serviceListing, SetOptions.merge())
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() = auth.signOut()

    private suspend fun syncDisplayNameToAuth(user: FirebaseUser, displayName: String) {
        if (displayName.isBlank()) return
        val request = UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
        user.updateProfile(request).await()
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Updates Firebase Auth profile (display name / photo URL) when [auth.currentUser] matches [uid].
     * Phone numbers require Phone Auth; keep those in [CustomerProfile] / [ProviderServiceProfile].
     */
    private suspend fun syncAuthProfileFromStrings(
        uid: String,
        displayName: String?,
        photoUrl: String?
    ) {
        val user = auth.currentUser ?: return
        if (user.uid != uid) return
        val builder = UserProfileChangeRequest.Builder()
        var any = false
        displayName?.takeIf { it.isNotBlank() }?.let {
            builder.setDisplayName(it)
            any = true
        }
        photoUrl?.takeIf { it.isNotBlank() }?.let {
            builder.setPhotoUri(Uri.parse(it))
            any = true
        }
        if (!any) return
        user.updateProfile(builder.build()).await()
       }

    /** Role + timestamps only; identity lives on [FirebaseUser]. */
    private suspend fun commitFirestoreUser(uid: String, role: String, displayName: String? = null) {
        val docRef = firestore.collection(AuthCollections.USERS).document(uid)
        val snap = docRef.get().await()
        val data = mutableMapOf<String, Any>(
            "role" to role,
            "updatedAt" to Timestamp.now()
        )
        displayName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { data["displayName"] = it }
        if (!snap.exists()) {
            data["createdAt"] = Timestamp.now()
        }
        docRef.set(data, SetOptions.merge()).await()
    }

}
