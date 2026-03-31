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
 * Provider identity and reputation at `provider_profiles/{uid}`.
 *
 * **Listing details** (description, hourly rate, service area, availability) live only on
 * [AuthCollections.SERVICES]; do not duplicate them on the profile document.
 *
 * Onboarding may set a **reference** field `category` → [AuthCollections.CATEGORIES] so
 * we can tell profile setup is complete before the first listing exists.
 *
 * [serviceCategory] is app/UI only; it is not written to Firestore (see `Exclude`).
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
     * `availabilityDays`, `availabilityStart`, `availabilityEnd`,
     * optional `serviceCenter`, `serviceRadiusKm`, `photoUrls`, `createdAt`, `updatedAt`.
     */
    const val SERVICES = "services"
    const val BOOKINGS = "bookings"
    /**
     * Shopping cart lines for a signed-in customer, as a subcollection:
     * `customer_profiles/{customerUid}/cart_items/{lineId}`.
     *
     * Each document stores [SERVICES], [PROVIDER_PROFILES], and optionally [CATEGORIES]
     * as **DocumentReference** fields, plus denormalized labels and scheduling fields.
     */
    const val CUSTOMER_CART_ITEMS = "cart_items"
}

/**
 * One row from [AuthCollections.CATEGORIES]; [id] is the document id (stored in [ProviderServiceProfile.serviceCategory]).
 */
data class ServiceCategoryOption(
    val id: String,
    val label: String,
)
