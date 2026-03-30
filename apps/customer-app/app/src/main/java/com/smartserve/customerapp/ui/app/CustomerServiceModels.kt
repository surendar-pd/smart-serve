package com.smartserve.customerapp.ui.app

data class CustomerProviderSummary(
    val uid: String,
    val displayName: String,
    val avgRating: Double,
    val totalReviews: Int,
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
)
