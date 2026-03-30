package com.smartserve.providerapp.ui.app

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.DocumentSnapshot
import com.smartserve.sharedauth.DefaultServiceAvailability

/**
 * Fields the app reads from `services/{id}`.
 *
 * **Refs + listing:** `provider`, `category`, `title`, `description`, `hourlyRate`, `isActive`,
 * `availabilityDays`, `availabilityStart`, `availabilityEnd`, timestamps.
 *
 * Phone, map center, and service radius are on [com.smartserve.sharedauth.ProviderServiceProfile] only.
 */
data class ProviderServiceRow(
    val id: String,
    val title: String,
    val description: String,
    val hourlyRate: Double,
    val categoryId: String,
    val isActive: Boolean,
    val availabilityDays: List<String>,
    val availabilityStart: String,
    val availabilityEnd: String,
    val photoUrls: List<String>,
    val serviceCenter: GeoPoint?,
    val serviceRadiusKm: Double,
)

data class ServiceDraft(
    val title: String,
    val description: String,
    val hourlyRate: Double,
    val categoryId: String,
    val isActive: Boolean,
    val availabilityDays: List<String>,
    val availabilityStart: String,
    val availabilityEnd: String,
    val photoUrls: List<String> = emptyList(),
    val serviceCenter: GeoPoint? = null,
    val serviceRadiusKm: Double? = null,
)

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.readStringList(field: String): List<String> {
    val raw = get(field) as? List<*> ?: return emptyList()
    return raw.mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() } }
}

fun DocumentSnapshot.toProviderServiceRow(): ProviderServiceRow? {
    val providerRef = getDocumentReference("provider") ?: return null
    if (providerRef.id.isBlank()) return null

    val categoryId = getDocumentReference("category")?.id
        ?: getString("category")?.takeIf { it.isNotBlank() }
        ?: ""

    val title = getString("title")?.trim().orEmpty().ifBlank { "Service" }

    val days = readStringList("availabilityDays").ifEmpty { DefaultServiceAvailability.DAYS }
    val start = getString("availabilityStart")?.trim()?.takeIf { it.isNotEmpty() }
        ?: DefaultServiceAvailability.START
    val end = getString("availabilityEnd")?.trim()?.takeIf { it.isNotEmpty() }
        ?: DefaultServiceAvailability.END

    return ProviderServiceRow(
        id = id,
        title = title,
        description = getString("description").orEmpty(),
        hourlyRate = getDouble("hourlyRate") ?: (getLong("hourlyRate")?.toDouble() ?: 0.0),
        categoryId = categoryId,
        isActive = getBoolean("isActive") ?: true,
        availabilityDays = days,
        availabilityStart = start,
        availabilityEnd = end,
        photoUrls = readStringList("photoUrls"),
        serviceCenter = getGeoPoint("serviceCenter"),
        serviceRadiusKm = getDouble("serviceRadiusKm") ?: (getLong("serviceRadiusKm")?.toDouble() ?: 10.0),
    )
}
