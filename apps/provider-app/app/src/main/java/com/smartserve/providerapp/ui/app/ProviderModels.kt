/*package com.smartserve.providerapp.ui.app

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

// ── Status enum ───────────────────────────────────────────────────────────────

enum class RequestStatus(val value: String) {
    NEW("new"),
    PENDING("pending"),
    ACTIVE("active"),
    COMPLETED("completed"),
    DECLINED("declined");

    companion object {
        fun from(raw: String?): RequestStatus =
            entries.firstOrNull { it.value == raw } ?: NEW
    }
}

// ── Domain model ──────────────────────────────────────────────────────────────

data class ServiceRequest(
    val id: String,
    val providerId: String,
    val customerId: String,
    val customerFirstName: String,
    val customerInitials: String,
    val serviceType: String,
    val date: String,
    val time: String,
    val neighborhood: String,
    val specialInstructions: String,
    val status: RequestStatus,
    val customerRating: Float?,
    val earnings: Int,
    val createdAt: Timestamp?,
    val completedAt: Timestamp?,
    val callLoggedAt: Timestamp?,
)

// ── Firestore deserializer ────────────────────────────────────────────────────

private fun DocumentSnapshot.providerIdResolved(): String? =
    getString("providerId")?.takeIf { it.isNotBlank() }
        ?: getDocumentReference("provider")?.id
        ?: getString("provider_id")?.takeIf { it.isNotBlank() }

private fun DocumentSnapshot.customerIdResolved(): String? =
    getString("customerId")?.takeIf { it.isNotBlank() }
        ?: getDocumentReference("customer")?.id
        ?: getDocumentReference("customer_id")?.id
        ?: getString("customer_id")?.takeIf { it.isNotBlank() }

fun DocumentSnapshot.toServiceRequest(): ServiceRequest? = runCatching {
    ServiceRequest(
        id                  = id,
        providerId          = getString("provider_id") ?: return null,
        customerId          = getString("customer_id") ?: return null,
        customerFirstName   = getString("customerFirstName") ?: "",
        customerInitials    = getString("customerInitials") ?: "",
        serviceType         = getString("serviceType") ?: "",
        date                = getString("date") ?: "",
        time                = getString("time") ?: "",
        neighborhood        = getString("neighborhood") ?: "",
        specialInstructions = getString("specialInstructions") ?: "",
        status              = RequestStatus.from(getString("status")),
        customerRating      = getDouble("customerRating")?.toFloat(),
        earnings            = getLong("earnings")?.toInt() ?: 0,
        createdAt           = getTimestamp("createdAt"),
        completedAt         = getTimestamp("completedAt"),
        callLoggedAt        = getTimestamp("callLoggedAt"),
    )
}.getOrNull()*/
package com.smartserve.providerapp.ui.app

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
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
    val customerFirstName: String,
    val customerInitials: String,
    val serviceType: String,
    val date: String,
    val time: String,
    val neighborhood: String,
    val specialInstructions: String,
    val status: RequestStatus,
    val customerRating: Float?,
    val earnings: Int,
    val createdAt: Timestamp?,
    val completedAt: Timestamp?,
    val callLoggedAt: Timestamp?,
)

// ── Firestore deserializer ────────────────────────────────────────────────────

fun DocumentSnapshot.toServiceRequest(): ServiceRequest? = runCatching {
    val dateString = getString("date")?.takeIf { it.isNotBlank() }
        ?: getTimestamp("bookingDate")?.let {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it.toDate())
        }
        ?: ""

    val serviceDisplay = getString("serviceName")?.takeIf { it.isNotBlank() }
        ?: getString("serviceType")?.takeIf { it.isNotBlank() }
        ?: getString("serviceId")
            ?.replace("_", " ")
            ?.split(" ")
            ?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        ?: ""

    val customerId = getString("customerId")
        ?: getString("customer_id")
        ?: return null

    val displayName = getString("customerName")
        ?.takeIf { it.isNotBlank() }
        ?: getString("customerFirstName")?.takeIf { it.isNotBlank() }
        ?: "Customer"
    val initials = displayName
        .split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")
        .ifBlank { customerId.take(2).uppercase() }

    val parsedEarnings = getLong("earnings")?.toInt()
        ?: getDouble("earnings")?.toInt()
        ?: getString("price")
            ?.filter { it.isDigit() }
            ?.toIntOrNull()
        ?: getLong("price")?.toInt()
        ?: getDouble("price")?.toInt()
        ?: 0

    val statusRaw = getString("status")
    val normalizedStatus = when (statusRaw?.trim()?.lowercase()) {
        "confirmed" -> "active"
        else -> statusRaw
    }

    ServiceRequest(
        id                  = id,
        providerId          = getString("providerUid") ?: getString("provider_id") ?: "",
        customerId          = customerId,
        customerFirstName   = displayName,
        customerInitials    = initials,
        serviceType         = serviceDisplay,
        date                = dateString,
        time                = getString("time") ?: getString("timeSlot") ?: "",
        neighborhood        = getString("address") ?: getString("neighborhood") ?: "",
        specialInstructions = getString("specialInstructions") ?: "",
        status              = RequestStatus.from(normalizedStatus),
        customerRating      = getDouble("customerRating")?.toFloat(),
        earnings            = parsedEarnings,
        createdAt           = getTimestamp("createdAt"),
        completedAt         = getTimestamp("completedAt"),
        callLoggedAt        = getTimestamp("callLoggedAt"),
    )
}.getOrNull()