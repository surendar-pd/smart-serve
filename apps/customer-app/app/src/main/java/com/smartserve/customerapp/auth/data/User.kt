//package com.smartserve.auth.data
package com.smartserve.customerapp.auth.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

// ──────────────────────────────────────────────
// Firestore: users/{uid}
// ──────────────────────────────────────────────
data class User(
    @DocumentId val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val phone: String? = null,          // optional for customers
    val photoUrl: String = "",
    val role: String = "customer",      // "customer" | "provider" | "both"
    val activeRole: String = "customer",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

// ──────────────────────────────────────────────
// Firestore: customerPreferences/{uid}
// ──────────────────────────────────────────────
data class CustomerPreferences(
    @DocumentId val uid: String = "",
    val homeAddress: String = "",
    val homeLocation: com.google.firebase.firestore.GeoPoint? = null,
    val preferredTimeSlot: String = "",
    val budgetMin: Double = 0.0,
    val budgetMax: Double = 200.0,
    val locationAwareness: Boolean = true,
    val smartSuggestions: Boolean = true,
    val pushNotifications: Boolean = true
)

// ──────────────────────────────────────────────
// Firestore: providerProfiles/{uid}
// ──────────────────────────────────────────────
data class ProviderProfile(
    @DocumentId val uid: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val phone: String = "",
    val isVerified: Boolean = false,
    val isOnline: Boolean = false,
    val avgRating: Double = 0.0,
    val totalReviews: Int = 0,
    val totalEarnings: Double = 0.0,
    val createdAt: Timestamp = Timestamp.now(),
    // extended profile setup fields stored here
    val serviceCategory: String = "",   // "home" | "education" | "studentLife"
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
