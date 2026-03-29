package com.smartserve.sharedauth

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

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
 * Listing name, phone (until Phone Auth), service area (map + radius), ratings.
 * **Availability** (days + hours) is stored per listing on [AuthCollections.SERVICES], not here.
 * Use [FirebaseUser] for email and profile photo when linked to Auth.
 *
 * In Firestore, the category link is stored as a **reference** field `category` → [AuthCollections.CATEGORIES]
 * (written from [AuthRepository.saveProviderProfile]); not as a plain string on the document.
 *
 * [serviceCategory] is still the category document id for app/UI only; it is not written to Firestore (see `Exclude`).
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
    @get:Exclude
    val serviceCategory: String = "",
    val serviceDescription: String = "",
    val hourlyRate: Double = 0.0,
    val serviceCenter: com.google.firebase.firestore.GeoPoint? = null,
    val serviceRadiusKm: Double = 10.0,
)

/** Defaults for a new `services` listing when the user has not set hours yet (e.g. onboarding). */
object DefaultServiceAvailability {
    val DAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
    const val START = "09:00"
    const val END = "18:00"
}

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
    const val CATEGORIES = "categories"
    /**
     * Listings: **reference** fields `provider` → [PROVIDER_PROFILES], `category` → [CATEGORIES];
     * plus `title`, `description`, `hourlyRate`, `isActive`,
     * `availabilityDays`, `availabilityStart`, `availabilityEnd`, `createdAt`, `updatedAt`.
     * Phone, map center, and service radius stay on [PROVIDER_PROFILES] only.
     */
    const val SERVICES = "services"
    const val BOOKINGS = "bookings"
}

/**
 * One row from [AuthCollections.CATEGORIES]; [id] is the document id (stored in [ProviderServiceProfile.serviceCategory]).
 */
data class ServiceCategoryOption(
    val id: String,
    val label: String,
)
