package com.smartserve.providerapp.ui.app

import org.osmdroid.util.GeoPoint
import kotlin.math.pow
import javax.inject.Inject
import javax.inject.Singleton

data class RouteResult(
    val points: List<GeoPoint>,
    val distanceKm: Double,
    val durationMin: Int,
)

@Singleton
class RouteRepository @Inject constructor() {

    private val api = ValhallaApiService.create()

    private val OTTAWA_LAT = 45.4215
    private val OTTAWA_LNG = -75.6972

    suspend fun getRoute(
        providerLat: Double,
        providerLng: Double,
        customerLat: Double,
        customerLng: Double,
    ): RouteResult {
        val destLat = if (customerLat == 0.0) OTTAWA_LAT else customerLat
        val destLng = if (customerLng == 0.0) OTTAWA_LNG else customerLng

        // Build Valhalla JSON request
        val json = """
            {
              "locations": [
                {"lon": $providerLng, "lat": $providerLat},
                {"lon": $destLng, "lat": $destLat}
              ],
              "costing": "auto",
              "directions_options": {"units": "km"}
            }
        """.trimIndent()

        android.util.Log.d("RouteRepo", "Calling Valhalla: provider=$providerLat,$providerLng dest=$destLat,$destLng")

        return try {
            val response = api.getRoute(json)
            val leg      = response.trip.legs.firstOrNull()
                ?: throw Exception("No route legs found")

            // Decode the encoded polyline shape into GeoPoints
            val points = decodePolyline(leg.shape)
            android.util.Log.d("RouteRepo", "Route: ${leg.summary.length}km, ${leg.summary.time}s, ${points.size} points")

            RouteResult(
                points      = points,
                distanceKm  = leg.summary.length,
                durationMin = (leg.summary.time / 60).toInt(),
            )
        } catch (e: Exception) {
            android.util.Log.e("RouteRepo", "Valhalla route failed, using direct line fallback", e)
            val points = listOf(GeoPoint(providerLat, providerLng), GeoPoint(destLat, destLng))
            val distanceKm = haversineKm(providerLat, providerLng, destLat, destLng)
            RouteResult(
                points      = points,
                distanceKm  = distanceKm,
                durationMin = (distanceKm / 40.0 * 60).toInt().coerceAtLeast(1),
            )
        }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).pow(2.0) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2).pow(2.0)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }


    /**
     * Decodes a Valhalla encoded polyline (precision 6) into a list of GeoPoints.
     * Valhalla uses precision=6 (multiply by 1e-6) unlike Google's precision=5.
     */
    private fun decodePolyline(encoded: String): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        var index  = 0
        var lat    = 0
        var lng    = 0

        while (index < encoded.length) {
            // Decode latitude
            var shift  = 0
            var result = 0
            var byte: Int
            do {
                byte    = encoded[index++].code - 63
                result  = result or ((byte and 0x1F) shl shift)
                shift  += 5
            } while (byte >= 0x20)
            lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            // Decode longitude
            shift  = 0
            result = 0
            do {
                byte    = encoded[index++].code - 63
                result  = result or ((byte and 0x1F) shl shift)
                shift  += 5
            } while (byte >= 0x20)
            lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            // Valhalla precision is 1e-6
            points.add(GeoPoint(lat * 1e-6, lng * 1e-6))
        }

        return points
    }
}