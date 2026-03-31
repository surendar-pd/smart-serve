package com.smartserve.customerapp.ui.app

/**
 * One cart line in the customer app. [lineDocumentId] is set when loaded from Firestore
 * (`customer_profiles/{uid}/cart_items/{lineDocumentId}`).
 */
data class CartItem(
    val lineDocumentId: String? = null,
    val providerUid: String = "",
    val serviceId: String = "",
    /** Category document id when known (home flow); empty for cross-category search. */
    val categoryId: String = "",
    val providerName: String,
    val serviceName: String,
    /** Display string e.g. "$45/hr". */
    val price: String,
    /** Hourly rate when added; used for provider booking `price` and audits. */
    val hourlyRate: Double = 0.0,
    /** Customer-selected location for service (Ottawa area). */
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val address: String = "",
    val addressLat: Double = 0.0,
    val addressLng: Double = 0.0,
    val date: String = "",
    val time: String = "",
    val timeRange: String = "",
    val addedAtMillis: Long = 0L,
)
