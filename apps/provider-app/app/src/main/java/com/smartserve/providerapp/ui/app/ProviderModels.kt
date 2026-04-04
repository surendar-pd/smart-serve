package com.smartserve.providerapp.ui.app

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.GeoPoint
import java.text.SimpleDateFormat
import java.util.Locale

// ── Status enum ───────────────────────────────────────────────────────────────

enum class RequestStatus(val value: String) {
    NEW("new"),
    PENDING("pending"),
    ACTIVE("active"),
    COMPLETED("completed"),
    DECLINED("declined");

    companion object {
        fun from(raw: String?): RequestStatus =
            entries.firstOrNull { it.value == raw?.lowercase()?.trim() } ?: NEW
    }
}

// ── Domain model ──────────────────────────────────────────────────────────────

data class ServiceRequest(
    val id: String,
    val providerId: String,
    val customerId: String,
    val serviceId: String,
    val categoryId: String,
    val categoryLabel: String,
    val customerFirstName: String,
    val customerInitials: String,
    val serviceType: String,
    val date: String,
    val time: String,
    val neighborhood: String,
    val homeAddress: String,
    val specialInstructions: String,
    val location: GeoPoint?,
    val status: RequestStatus,
    val customerRating: Float?,
    val earnings: Int,
    val scheduledAt: Timestamp?,
    val createdAt: Timestamp?,
    val completedAt: Timestamp?,
    val callLoggedAt: Timestamp?,
    val customerLat: Double = 0.0,
    val customerLng: Double = 0.0,
)

// ── Firestore deserializer ────────────────────────────────────────────────────

fun DocumentSnapshot.toServiceRequest(): ServiceRequest? = runCatching {
    // Canonical schema: reference fields + minimal primitives.
    val bookingDate = getTimestamp("bookingDate")
    val dateString = bookingDate?.let {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it.toDate())
    } ?: ""

    val customerRef = get("customer") as? DocumentReference ?: return null
    val providerRef = get("provider") as? DocumentReference ?: return null
    val serviceRef = get("service") as? DocumentReference ?: return null
    val categoryId = getDocumentReference("category")?.id
        ?: getString("category")?.trim().orEmpty()

    val customerId = customerRef.id
    val providerId = providerRef.id
    val serviceId = serviceRef.id

    // Name + service title are resolved in repository enrichment.
    val displayName = "Customer"
    val initials = displayName
        .split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")
        .ifBlank { customerId.take(2).uppercase() }

    val parsedEarnings = getDouble("hourlyRate")?.toInt()
        ?: getLong("hourlyRate")?.toInt()
        ?: 0

    val normalizedStatus = getString("status")

    val locationResolved = getGeoPoint("location")

    ServiceRequest(
        id                  = id,
        providerId          = providerId,
        customerId          = customerId,
        serviceId           = serviceId,
        categoryId          = categoryId,
        categoryLabel       = "",
        customerFirstName   = displayName,
        customerInitials    = initials,
        serviceType         = "",
        date                = dateString,
        time                = getString("timeRange")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: (getString("timeSlot") ?: ""),
        neighborhood        = getString("address") ?: "",
        homeAddress         = getString("address") ?: "",
        specialInstructions = getString("specialInstructions") ?: "",
        location            = locationResolved,
        status              = RequestStatus.from(normalizedStatus),
        customerRating      = getDouble("customerRating")?.toFloat(),
        earnings            = parsedEarnings,
        scheduledAt         = getTimestamp("scheduledAt") ?: getTimestamp("bookingDate"),
        createdAt           = getTimestamp("createdAt"),
        completedAt         = getTimestamp("completedAt"),
        callLoggedAt        = getTimestamp("callLoggedAt"),
        customerLat = locationResolved?.latitude ?: 0.0,
        customerLng = locationResolved?.longitude ?: 0.0,
    )
}.getOrNull()