package com.smartserve.sharedauth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

// ── Ottawa-region bounding box ────────────────────────────────────────────────
private const val LAT_MIN = 44.96
private const val LAT_MAX = 45.60
private const val LON_MIN = -76.40
private const val LON_MAX = -75.10

private val OTTAWA_KEYWORDS = listOf(
    "Ottawa", "Gatineau", "Kanata", "Nepean", "Gloucester",
    "Barrhaven", "Stittsville", "Orléans", "Orleans", "Vanier",
)

private const val USER_AGENT = "SmartServe/1.0 (smartserve-app)"

data class GeoResult(
    val shortLabel: String,   // e.g. "123 Main St" – shown in text field
    val fullAddress: String,  // full Nominatim display_name stored in booking
    val lat: Double,
    val lon: Double,
    val isInOttawa: Boolean,
)

@Singleton
class NominatimGeocoder @Inject constructor() {

    /**
     * Reverse-geocode [lat]/[lon] → address.
     * Returns null on network error or if Nominatim returns no result.
     */
    suspend fun reverseGeocode(lat: Double, lon: Double): GeoResult? =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL(
                    "https://nominatim.openstreetmap.org/reverse" +
                    "?lat=$lat&lon=$lon&format=json&addressdetails=1"
                )
                val conn = url.openConnection()
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.connectTimeout = 8_000
                conn.readTimeout    = 8_000
                val json  = JSONObject(conn.getInputStream().bufferedReader().readText())
                val full  = json.optString("display_name", "")
                val addr  = json.optJSONObject("address")
                val short = buildShortLabel(addr, full)
                GeoResult(
                    shortLabel  = short,
                    fullAddress = full,
                    lat         = lat,
                    lon         = lon,
                    isInOttawa  = isOttawa(lat, lon, full),
                )
            }.getOrNull()
        }

    /**
     * Forward-geocode a [query] text → lat/lon.
     * Automatically appends "Ottawa, Ontario" to bias results to the region.
     */
    suspend fun forwardGeocode(query: String): GeoResult? =
        withContext(Dispatchers.IO) {
            runCatching {
                val q   = URLEncoder.encode("$query, Ottawa, Ontario, Canada", "UTF-8")
                val url = URL(
                    "https://nominatim.openstreetmap.org/search" +
                    "?q=$q&format=json&limit=1&addressdetails=1"
                )
                val conn = url.openConnection()
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.connectTimeout = 8_000
                conn.readTimeout    = 8_000
                val arr = JSONArray(conn.getInputStream().bufferedReader().readText())
                if (arr.length() == 0) return@runCatching null
                val obj  = arr.getJSONObject(0)
                val lat  = obj.getString("lat").toDouble()
                val lon  = obj.getString("lon").toDouble()
                val full = obj.optString("display_name", "")
                val addr = obj.optJSONObject("address")
                val short = buildShortLabel(addr, full)
                GeoResult(
                    shortLabel  = short,
                    fullAddress = full,
                    lat         = lat,
                    lon         = lon,
                    isInOttawa  = isOttawa(lat, lon, full),
                )
            }.getOrNull()
        }

    /** Returns up to [limit] address candidates for [query] — used for autocomplete. */
    suspend fun searchSuggestions(query: String, limit: Int = 5): List<GeoResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val q = URLEncoder.encode("$query, Ottawa, Ontario, Canada", "UTF-8")
                val url = URL(
                    "https://nominatim.openstreetmap.org/search" +
                    "?q=$q&format=json&limit=$limit&addressdetails=1"
                )
                val conn = url.openConnection()
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.connectTimeout = 8_000
                conn.readTimeout    = 8_000
                val arr = JSONArray(conn.getInputStream().bufferedReader().readText())
                (0 until arr.length()).map { i ->
                    val obj  = arr.getJSONObject(i)
                    val lat  = obj.getString("lat").toDouble()
                    val lon  = obj.getString("lon").toDouble()
                    val full = obj.optString("display_name", "")
                    val addr = obj.optJSONObject("address")
                    GeoResult(
                        shortLabel  = buildShortLabel(addr, full),
                        fullAddress = full,
                        lat         = lat,
                        lon         = lon,
                        isInOttawa  = isOttawa(lat, lon, full),
                    )
                }
            }.getOrDefault(emptyList())
        }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Build a human-readable short label: "123 Main St, Kanata" */
    private fun buildShortLabel(addr: JSONObject?, fallback: String): String {
        if (addr == null) return fallback.substringBefore(",").trim()
        val number = addr.optString("house_number", "")
        val road   = addr.optString("road", "")
        val suburb = addr.optString("suburb", "")
            .ifBlank { addr.optString("neighbourhood", "") }
            .ifBlank { addr.optString("quarter", "") }
            .ifBlank { addr.optString("city_district", "") }
        return buildString {
            if (number.isNotBlank() && road.isNotBlank()) append("$number $road")
            else if (road.isNotBlank()) append(road)
            else append(fallback.substringBefore(",").trim())
            if (suburb.isNotBlank()) append(", $suburb")
        }
    }

    private fun isOttawa(lat: Double, lon: Double, displayName: String): Boolean {
        val inBox = lat in LAT_MIN..LAT_MAX && lon in LON_MIN..LON_MAX
        val nameMatch = OTTAWA_KEYWORDS.any { kw ->
            displayName.contains(kw, ignoreCase = true)
        }
        return inBox || nameMatch
    }
}
