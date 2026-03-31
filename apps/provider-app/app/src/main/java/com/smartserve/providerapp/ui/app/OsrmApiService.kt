package com.smartserve.providerapp.ui.app

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ── Valhalla Response data classes ────────────────────────────────────────────

data class ValhallaResponse(
    @SerializedName("trip") val trip: ValhallaTrip,
)

data class ValhallaTrip(
    @SerializedName("legs")    val legs: List<ValhallaLeg>,
    @SerializedName("summary") val summary: ValhallaSummary,
)

data class ValhallaLeg(
    @SerializedName("shape")   val shape: String,      // encoded polyline
    @SerializedName("summary") val summary: ValhallaSummary,
)

data class ValhallaSummary(
    @SerializedName("time")   val time: Double,        // seconds
    @SerializedName("length") val length: Double,      // kilometres
)

// ── Keep these for compilation compatibility ───────────────────────────────────
data class OsrmResponse(val routes: List<OsrmRoute> = emptyList(), val code: String = "")
data class OsrmRoute(val distance: Double = 0.0, val duration: Double = 0.0, val geometry: OsrmGeometry = OsrmGeometry())
data class OsrmGeometry(val coordinates: List<List<Double>> = emptyList())

// ── Valhalla API interface ─────────────────────────────────────────────────────

interface ValhallaApiService {

    @GET("route")
    suspend fun getRoute(
        @Query("json") json: String,
    ): ValhallaResponse

    companion object {
        private const val BASE_URL = "https://valhalla1.openstreetmap.de/"

        fun create(): ValhallaApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ValhallaApiService::class.java)
        }
    }
}