package com.smartserve.sharedauth

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentId

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
    data object Loading : AuthResult()
}

/**
 * Extended customer data for `customer_profiles/{uid}`.
 * Use [FirebaseUser] for email, name, photo; optional [phone] when not using Phone Auth.
 */
data class CustomerProfile(
    @DocumentId val uid: String = "",
    val phone: String? = null,
    val homeAddress: String = "",
    val homeLocation: com.google.firebase.firestore.GeoPoint? = null,
    val preferredTimeSlot: String = "",
    val budgetMin: Double = 0.0,
    val budgetMax: Double = 200.0,
    val locationAwareness: Boolean = true,
    val smartSuggestions: Boolean = true,
    val pushNotifications: Boolean = true
)

/**
 * Provider marketplace details at `provider_profiles/{uid}`.
 * Listing name, phone (until Phone Auth), services, availability, ratings.
 * Use [FirebaseUser] for email and profile photo when linked to Auth.
 */
data class ProviderServiceProfile(
    @DocumentId val uid: String = "",
    val displayName: String = "",
    val phone: String = "",
    val isVerified: Boolean = false,
    val isOnline: Boolean = false,
    val avgRating: Double = 0.0,
    val totalReviews: Int = 0,
    val totalEarnings: Double = 0.0,
    val createdAt: Timestamp = Timestamp.now(),
    val serviceCategory: String = "",
    val serviceDescription: String = "",
    val hourlyRate: Double = 0.0,
    val serviceCenter: com.google.firebase.firestore.GeoPoint? = null,
    val serviceRadiusKm: Double = 10.0,
    val availabilityDays: List<String> = emptyList(),
    val availabilityStart: String = "09:00",
    val availabilityEnd: String = "18:00"
)

enum class UserRole(val value: String) {
    CUSTOMER("customer"),
    PROVIDER("provider"),
    BOTH("both")
}

/** Firestore collection ids for auth-related data (snake_case). */
object AuthCollections {
    const val USERS = "users"
    const val CUSTOMER_PROFILES = "customer_profiles"
    const val PROVIDER_PROFILES = "provider_profiles"
}
