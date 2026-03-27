package com.smartserve.providerapp.ui.app

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

fun DocumentSnapshot.toServiceRequest(): ServiceRequest? = runCatching {
    ServiceRequest(
        id                  = id,
        providerId          = getString("providerId") ?: return null,
        customerId          = getString("customerId") ?: return null,
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
}.getOrNull()