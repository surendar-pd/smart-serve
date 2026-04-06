package com.smartserve.customerapp.ui.app

data class CustomerProviderSummary(
    val uid: String,
    val displayName: String,
    val avgRating: Double,
    val totalReviews: Int,
    val serviceTitles: List<String> = emptyList(),
    val serviceDescription: String,
    val hourlyRate: Double = 0.0,
    // Populated when loaded in the context of a specific category
    val categoryServiceRate: Double = 0.0,
    val categoryAvailabilityDays: List<String> = emptyList(),
    val categoryAvailabilityStart: String = "",
    val categoryAvailabilityEnd: String = "",
)

data class CustomerServiceListing(
    val serviceId: String,
    val title: String,
    val description: String,
    val hourlyRate: Double,
    val providerUid: String,
    val providerName: String,
    val availabilityDays: List<String>,
    val availabilityStart: String,
    val availabilityEnd: String,
    val providerAvgRating: Double = 0.0,
    val providerTotalReviews: Int = 0,
    /** Hosted image URLs from the provider listing (e.g. ImageKit); empty if none. */
    val photoUrls: List<String> = emptyList(),
)

data class CustomerBooking(
    val id: String,
    val providerName: String,
    /** Category label (service type), e.g. "Cleaning". */
    val typeLabel: String = "",
    /** Set by customer after completion; null by default (field may be absent in Firestore). */
    val providerRating: Float? = null,
    /** Set by provider; null by default (field may be absent in Firestore). */
    val customerRating: Float? = null,
    val serviceName: String,
    val price: String,
    val date: String,
    val time: String,
    val status: String,          // "pending" | "active" | "completed" | "declined"
    val address: String = "",
    val scheduledAtMillis: Long = 0L,
    val createdAtMillis: Long = 0L,
)
