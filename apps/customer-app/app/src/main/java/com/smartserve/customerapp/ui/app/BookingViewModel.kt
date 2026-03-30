package com.smartserve.customerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

// Default map center: Parliament Hill, Ottawa
private const val OTTAWA_LAT = 45.4215
private const val OTTAWA_LON = -75.6972

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val repo: CustomerServicesRepository,
    private val geocoder: NominatimGeocoder,
) : ViewModel() {

    /** Home address pre-loaded from customer profile (used to seed the address field). */
    private val _homeAddress = MutableStateFlow("")
    val homeAddress: StateFlow<String> = _homeAddress

    /** Current map pin position. */
    private val _pinLat = MutableStateFlow(OTTAWA_LAT)
    val pinLat: StateFlow<Double> = _pinLat

    private val _pinLon = MutableStateFlow(OTTAWA_LON)
    val pinLon: StateFlow<Double> = _pinLon

    /** Result of the most recent geocoding call (null while not yet resolved). */
    private val _geoResult = MutableStateFlow<GeoResult?>(null)
    val geoResult: StateFlow<GeoResult?> = _geoResult

    /** True while a Nominatim call is in flight. */
    private val _isGeocoding = MutableStateFlow(false)
    val isGeocoding: StateFlow<Boolean> = _isGeocoding

    init {
        viewModelScope.launch {
            // Initial load from Firestore (also seeds repo.homeAddressFlow)
            val saved = repo.getCustomerHomeAddress()
            _homeAddress.value = saved
            if (saved.isNotBlank()) geocodeAndPin(saved)

            // React to address changes made from ProfileScreen while this VM is alive
            repo.homeAddressFlow
                .drop(1)                // skip the value we just set above
                .distinctUntilChanged()
                .collect { newAddr ->
                    if (newAddr.isNotBlank() && newAddr != _homeAddress.value) {
                        _homeAddress.value = newAddr
                        geocodeAndPin(newAddr)
                    }
                }
        }
    }

    private suspend fun geocodeAndPin(address: String) {
        _isGeocoding.value = true
        val result = geocoder.forwardGeocode(address)
        if (result != null) {
            _pinLat.value    = result.lat
            _pinLon.value    = result.lon
            _geoResult.value = result
        }
        _isGeocoding.value = false
    }

    /** Called when the user taps the map — triggers reverse geocoding. */
    fun onMapTap(lat: Double, lon: Double) {
        _pinLat.value    = lat
        _pinLon.value    = lon
        _geoResult.value = null
        viewModelScope.launch {
            _isGeocoding.value = true
            _geoResult.value   = geocoder.reverseGeocode(lat, lon)
            _isGeocoding.value = false
        }
    }

    /** Called when the user types an address and taps Search — triggers forward geocoding. */
    fun searchAddress(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isGeocoding.value = true
            _geoResult.value   = null
            val result = geocoder.forwardGeocode(query)
            if (result != null) {
                _pinLat.value    = result.lat
                _pinLon.value    = result.lon
                _geoResult.value = result
            }
            _isGeocoding.value = false
        }
    }

    fun clearGeoResult() {
        _geoResult.value = null
    }
}
