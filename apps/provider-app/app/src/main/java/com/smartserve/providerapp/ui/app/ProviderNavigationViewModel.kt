package com.smartserve.providerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartserve.sharedauth.NominatimGeocoder
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

data class NavigationUiState(
    val providerLocation: GeoPoint? = null,
    val customerLocation: GeoPoint? = null,
    val routePoints: List<GeoPoint> = emptyList(),
    val distanceText: String = "",
    val etaText: String = "",
    val customerAddress: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class ProviderNavigationViewModel @AssistedInject constructor(
    @Assisted("customerLat")     private val customerLat: Double,
    @Assisted("customerLng")     private val customerLng: Double,
    @Assisted("customerAddress") private val customerAddress: String,
    private val locationRepository: ProviderLocationRepository,
    private val routeRepository: RouteRepository,
    private val geocoder: NominatimGeocoder,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("customerLat")     customerLat: Double,
            @Assisted("customerLng")     customerLng: Double,
            @Assisted("customerAddress") customerAddress: String,
        ): ProviderNavigationViewModel
    }

    private val _uiState = MutableStateFlow(
        NavigationUiState(
            customerLocation = if (customerLat != 0.0 || customerLng != 0.0) GeoPoint(customerLat, customerLng) else null,
            customerAddress  = customerAddress,
        )
    )
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

    private var routeRefreshJob: Job? = null
    private var refreshStarted = false

    init {
        observeProviderLocation()
        if (_uiState.value.customerLocation == null && customerAddress.isNotBlank()) {
            resolveCustomerAddress()
        }
    }

    private fun resolveCustomerAddress() {
        viewModelScope.launch {
            val result = geocoder.forwardGeocode(customerAddress)
            if (result != null) {
                _uiState.update {
                    it.copy(
                        customerLocation = GeoPoint(result.lat, result.lon),
                        customerAddress  = result.shortLabel,
                        errorMessage     = null,
                    )
                }
                _uiState.value.providerLocation?.let { fetchRoute(it) }
            } else {
                _uiState.update {
                    it.copy(errorMessage = "Unable to resolve address for map")
                }
            }
        }
    }

    private fun observeProviderLocation() {
        viewModelScope.launch {
            locationRepository.observeLocation()
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = "Location unavailable: ${e.message}") }
                }
                .collect { location ->
                    val providerPoint = GeoPoint(location.latitude, location.longitude)
                    _uiState.update { it.copy(providerLocation = providerPoint) }
                    if (_uiState.value.customerLocation != null) {
                        fetchRoute(providerPoint)
                    }
                    if (!refreshStarted) {
                        startRouteRefresh()
                        refreshStarted = true
                    }
                }
        }
    }

    private fun startRouteRefresh() {
        routeRefreshJob?.cancel()
        routeRefreshJob = viewModelScope.launch {
            while (true) {
                delay(30_000L)
                _uiState.value.providerLocation?.let { fetchRoute(it) }
            }
        }
    }

    fun refreshRoute() {
        _uiState.value.providerLocation?.let { fetchRoute(it) }
    }

    private fun fetchRoute(providerPoint: GeoPoint) {
        viewModelScope.launch {
            val customerPoint = _uiState.value.customerLocation
            if (customerPoint == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Destination coordinates unavailable") }
                return@launch
            }

            android.util.Log.d("ProviderNavVM", "fetchRoute called provider=$providerPoint customer=$customerPoint")
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            runCatching {
                routeRepository.getRoute(
                    providerLat = providerPoint.latitude,
                    providerLng = providerPoint.longitude,
                    customerLat = customerPoint.latitude,
                    customerLng = customerPoint.longitude,
                )
            }.onSuccess { result ->
                _uiState.update {
                    val routePoints = result.points.ifEmpty() {
                        listOf(providerPoint, customerPoint)
                    }
                    it.copy(
                        isLoading    = false,
                        routePoints  = routePoints,
                        distanceText = "%.1f km".format(result.distanceKm),
                        etaText      = "${result.durationMin} mins",
                        errorMessage = if (routePoints.isEmpty()) "Route data unavailable" else null,
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        routePoints = listOf(providerPoint, customerPoint),
                        distanceText = "--",
                        etaText = "--",
                        errorMessage = "Route error: ${e.message}",
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        routeRefreshJob?.cancel()
    }
}