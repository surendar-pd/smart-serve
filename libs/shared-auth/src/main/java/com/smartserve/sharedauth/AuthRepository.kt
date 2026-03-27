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
            commitFirestoreUser(firebaseUser.uid, resolvedRole)
            if (resolvedRole == UserRole.PROVIDER.value && !phone.isNullOrBlank()) {
                firestore.collection(AuthCollections.PROVIDER_PROFILES).document(firebaseUser.uid)
                    .set(mapOf("phone" to phone), SetOptions.merge())
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
                commitFirestoreUser(firebaseUser.uid, resolvedRole)
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
            doc.exists() && doc.getString("serviceCategory")?.isNotBlank() == true
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

    suspend fun saveCustomerProfile(
        uid: String,
        phone: String?,
        homeAddress: String,
        locationAwareness: Boolean,
        pushNotifications: Boolean,
        photoUrl: String?
    ): Result<Unit> {
        return try {
            val prefs = CustomerProfile(
                uid = uid,
                phone = phone?.takeIf { it.isNotBlank() },
                homeAddress = homeAddress,
                locationAwareness = locationAwareness,
                pushNotifications = pushNotifications,
                smartSuggestions = true
            )
            firestore.collection(AuthCollections.CUSTOMER_PROFILES).document(uid).set(prefs).await()
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
        availabilityDays: List<String>,
        availabilityStart: String,
        availabilityEnd: String
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

            val profile = ProviderServiceProfile(
                uid = uid,
                displayName = resolvedDisplayName,
                phone = resolvedPhone,
                serviceCategory = serviceCategory,
                serviceDescription = serviceDescription,
                hourlyRate = hourlyRate,
                serviceCenter = serviceCenter,
                serviceRadiusKm = serviceRadiusKm,
                availabilityDays = availabilityDays,
                availabilityStart = availabilityStart,
                availabilityEnd = availabilityEnd
            )
            firestore.collection(AuthCollections.PROVIDER_PROFILES).document(uid).set(profile).await()
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
    private suspend fun commitFirestoreUser(uid: String, role: String) {
        val docRef = firestore.collection(AuthCollections.USERS).document(uid)
        val snap = docRef.get().await()
        val data = mutableMapOf<String, Any>(
            "role" to role,
            "updatedAt" to Timestamp.now()
        )
        if (!snap.exists()) {
            data["createdAt"] = Timestamp.now()
        }
        docRef.set(data, SetOptions.merge()).await()
    }

}
