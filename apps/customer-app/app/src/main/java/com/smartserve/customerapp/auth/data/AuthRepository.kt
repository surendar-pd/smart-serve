//package com.smartserve.auth.data
package com.smartserve.customerapp.auth.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Loading : AuthResult()
}

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

    // ──────────────────────────────────────────────
    // Sign Up with email/password
    // ──────────────────────────────────────────────
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

            // Send email verification
            firebaseUser.sendEmailVerification().await()

            // Write user document to Firestore: users/{uid}
            val user = User(
                uid = firebaseUser.uid,
                email = email,
                fullName = fullName,
                phone = phone,
                role = role,
                activeRole = if (role == UserRole.PROVIDER.value) "provider" else "customer"
            )
            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(user)
                .await()

            AuthResult.Success(firebaseUser)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Sign up failed")
        }
    }

    // ──────────────────────────────────────────────
    // Sign In with email/password
    // ──────────────────────────────────────────────
    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return AuthResult.Error("Login failed")
            AuthResult.Success(firebaseUser)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Login failed")
        }
    }

    // ──────────────────────────────────────────────
    // Sign In with Google OAuth token
    // ──────────────────────────────────────────────
    suspend fun signInWithGoogle(idToken: String, role: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: return AuthResult.Error("Google sign-in failed")

            // Create user doc if new user
            if (result.additionalUserInfo?.isNewUser == true) {
                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    fullName = firebaseUser.displayName ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                    role = role,
                    activeRole = if (role == UserRole.PROVIDER.value) "provider" else "customer"
                )
                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(user)
                    .await()
            }
            AuthResult.Success(firebaseUser)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Google sign-in failed")
        }
    }

    // ──────────────────────────────────────────────
    // Forgot Password
    // ──────────────────────────────────────────────
    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ──────────────────────────────────────────────
    // Check if profile setup has been completed
    // ──────────────────────────────────────────────
    suspend fun isProfileSetupComplete(uid: String, role: String): Boolean {
        return try {
            if (role == "customer") {
                val doc = firestore.collection("customerPreferences").document(uid).get().await()
                doc.exists() && doc.getString("homeAddress")?.isNotBlank() == true
            } else {
                val doc = firestore.collection("providerProfiles").document(uid).get().await()
                doc.exists() && doc.getString("serviceCategory")?.isNotBlank() == true
            }
        } catch (e: Exception) {
            false
        }
    }

    // ──────────────────────────────────────────────
    // Fetch stored user role from Firestore
    // ──────────────────────────────────────────────
    suspend fun getUserRole(uid: String): String {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            doc.getString("activeRole") ?: "customer"
        } catch (e: Exception) {
            "customer"
        }
    }

    // ──────────────────────────────────────────────
    // Save Customer Profile Setup
    // Writes to: customerPreferences/{uid}
    // ──────────────────────────────────────────────
    suspend fun saveCustomerProfile(
        uid: String,
        phone: String?,
        homeAddress: String,
        locationAwareness: Boolean,
        pushNotifications: Boolean,
        photoUrl: String?
    ): Result<Unit> {
        return try {
            val prefs = CustomerPreferences(
                uid = uid,
                homeAddress = homeAddress,
                locationAwareness = locationAwareness,
                pushNotifications = pushNotifications,
                smartSuggestions = true
            )
            firestore.collection("customerPreferences").document(uid).set(prefs).await()

            // Update photoUrl + phone in users/{uid} if provided
            val updates = mutableMapOf<String, Any>()
            phone?.let { if (it.isNotBlank()) updates["phone"] = it }
            photoUrl?.let { if (it.isNotBlank()) updates["photoUrl"] = it }
            if (updates.isNotEmpty()) {
                firestore.collection("users").document(uid).update(updates).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ──────────────────────────────────────────────
    // Save Provider Profile Setup
    // Writes to: providerProfiles/{uid}
    // ──────────────────────────────────────────────
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
            val profile = ProviderProfile(
                uid = uid,
                displayName = displayName,
                phone = phone,
                photoUrl = photoUrl ?: "",
                serviceCategory = serviceCategory,
                serviceDescription = serviceDescription,
                hourlyRate = hourlyRate,
                serviceCenter = serviceCenter,
                serviceRadiusKm = serviceRadiusKm,
                availabilityDays = availabilityDays,
                availabilityStart = availabilityStart,
                availabilityEnd = availabilityEnd
            )
            firestore.collection("providerProfiles").document(uid).set(profile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() = auth.signOut()
}
