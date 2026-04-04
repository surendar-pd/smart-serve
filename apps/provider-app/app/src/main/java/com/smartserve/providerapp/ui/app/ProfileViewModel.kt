package com.smartserve.providerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import com.smartserve.sharedauth.AuthCollections
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val displayName: String = "Provider",
    val memberSince: String = "--",
    val serviceOptions: List<ProviderServiceRow> = emptyList(),
    val selectedServiceId: String = "",
    val areaSheetOpen: Boolean = false,
    val availabilitySheetOpen: Boolean = false,
    val areaLat: Double = 45.4215,
    val areaLng: Double = -75.6972,
    val areaRadiusKm: String = "10",
    val availabilityDays: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri"),
    val availabilityStart: String = "09:00",
    val availabilityEnd: String = "18:00",
    val notificationSheetOpen: Boolean = false,
    val pushNotifications: Boolean = true,
    val requestNotifications: Boolean = true,
    val serviceReminderNotifications: Boolean = true,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val servicesRepository: ProviderServicesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var providerUid: String? = null
    private var selectedServiceId: String? = null
    private var selectedService: ProviderServiceRow? = null

    init {
        load()
    }

    private fun load() {
        val user = auth.currentUser
        val uid = user?.uid ?: run {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Not signed in") }
            return
        }
        providerUid = uid

        val memberSince = user.metadata?.creationTimestamp?.let { ts ->
            Calendar.getInstance().also { it.timeInMillis = ts }.get(Calendar.YEAR).toString()
        } ?: "--"

        _uiState.update {
            it.copy(
                displayName = user.displayName?.takeIf { n -> n.isNotBlank() } ?: "Provider",
                memberSince = memberSince,
            )
        }

        viewModelScope.launch {
            runCatching {
                firestore.collection(AuthCollections.PROVIDER_PROFILES).document(uid).get().await()
            }.onSuccess { doc ->
                _uiState.update {
                    it.copy(
                        pushNotifications = doc.getBoolean("pushNotifications") ?: true,
                        requestNotifications = doc.getBoolean("providerRequestNotifications") ?: true,
                        serviceReminderNotifications = doc.getBoolean("providerServiceReminderNotifications") ?: true,
                    )
                }
            }
        }

        viewModelScope.launch {
            servicesRepository.observeServicesForProvider(uid).collect { rows ->
                val service = selectedServiceId?.let { id -> rows.firstOrNull { it.id == id } } ?: rows.firstOrNull()
                selectedService = service
                selectedServiceId = service?.id
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        serviceOptions = rows,
                        selectedServiceId = service?.id.orEmpty(),
                        availabilityDays = service?.availabilityDays ?: it.availabilityDays,
                        availabilityStart = service?.availabilityStart ?: it.availabilityStart,
                        availabilityEnd = service?.availabilityEnd ?: it.availabilityEnd,
                        areaLat = service?.serviceCenter?.latitude ?: it.areaLat,
                        areaLng = service?.serviceCenter?.longitude ?: it.areaLng,
                        areaRadiusKm = service?.serviceRadiusKm?.toString() ?: it.areaRadiusKm,
                    )
                }
            }
        }
    }

    fun onSelectedServiceChange(serviceId: String) {
        selectedServiceId = serviceId
        val service = _uiState.value.serviceOptions.firstOrNull { it.id == serviceId }
        selectedService = service
        if (service != null) {
            _uiState.update {
                it.copy(
                    selectedServiceId = service.id,
                    availabilityDays = service.availabilityDays,
                    availabilityStart = service.availabilityStart,
                    availabilityEnd = service.availabilityEnd,
                    areaLat = service.serviceCenter?.latitude ?: it.areaLat,
                    areaLng = service.serviceCenter?.longitude ?: it.areaLng,
                    areaRadiusKm = service.serviceRadiusKm.toString(),
                )
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun openNotificationSheet() = _uiState.update { it.copy(notificationSheetOpen = true, errorMessage = null) }
    fun closeNotificationSheet() = _uiState.update { it.copy(notificationSheetOpen = false) }
    fun onPushNotificationsChange(v: Boolean) = _uiState.update { it.copy(pushNotifications = v) }
    fun onRequestNotificationsChange(v: Boolean) = _uiState.update { it.copy(requestNotifications = v) }
    fun onServiceReminderNotificationsChange(v: Boolean) = _uiState.update { it.copy(serviceReminderNotifications = v) }

    fun saveNotificationSettings() = viewModelScope.launch {
        val uid = providerUid ?: return@launch
        val s = _uiState.value
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        runCatching {
            firestore.collection(AuthCollections.PROVIDER_PROFILES).document(uid)
                .set(
                    mapOf(
                        "pushNotifications" to s.pushNotifications,
                        "providerRequestNotifications" to s.requestNotifications,
                        "providerServiceReminderNotifications" to s.serviceReminderNotifications,
                    ),
                    SetOptions.merge(),
                )
                .await()
        }.onSuccess {
            _uiState.update { it.copy(isSaving = false, notificationSheetOpen = false) }
        }.onFailure { e ->
            _uiState.update { it.copy(isSaving = false, errorMessage = e.localizedMessage ?: "Failed to save notification settings") }
        }
    }

    fun openAreaSheet() = _uiState.update { it.copy(areaSheetOpen = true, errorMessage = null) }
    fun closeAreaSheet() = _uiState.update { it.copy(areaSheetOpen = false) }

    fun openAvailabilitySheet() =
        _uiState.update { it.copy(availabilitySheetOpen = true, errorMessage = null) }

    fun closeAvailabilitySheet() = _uiState.update { it.copy(availabilitySheetOpen = false) }

    fun setMapPoint(lat: Double, lng: Double) {
        _uiState.update { it.copy(areaLat = lat, areaLng = lng) }
    }

    fun onRadiusChange(value: String) = _uiState.update { it.copy(areaRadiusKm = value) }
    fun onAvailabilityStartChange(value: String) = _uiState.update { it.copy(availabilityStart = value) }
    fun onAvailabilityEndChange(value: String) = _uiState.update { it.copy(availabilityEnd = value) }

    fun toggleAvailabilityDay(day: String) {
        val current = _uiState.value.availabilityDays
        val next = if (day in current) current - day else current + day
        _uiState.update { it.copy(availabilityDays = next) }
    }

    fun saveArea() = viewModelScope.launch {
        val uid = providerUid ?: return@launch
        val service = selectedService ?: return@launch
        val radius = _uiState.value.areaRadiusKm.toDoubleOrNull()
        if (radius == null || radius <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Enter a valid radius") }
            return@launch
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        servicesRepository.updateService(
            uid,
            service.id,
            ServiceDraft(
                title = service.title,
                description = service.description,
                hourlyRate = service.hourlyRate,
                categoryId = service.categoryId,
                isActive = service.isActive,
                availabilityDays = service.availabilityDays,
                availabilityStart = service.availabilityStart,
                availabilityEnd = service.availabilityEnd,
                photoUrls = service.photoUrls,
                serviceCenter = GeoPoint(_uiState.value.areaLat, _uiState.value.areaLng),
                serviceRadiusKm = radius,
            )
        ).onSuccess {
            _uiState.update { it.copy(isSaving = false, areaSheetOpen = false) }
        }.onFailure { e ->
            _uiState.update { it.copy(isSaving = false, errorMessage = e.localizedMessage ?: "Failed to save area") }
        }
    }

    fun saveAvailability() = viewModelScope.launch {
        val uid = providerUid ?: return@launch
        val service = selectedService ?: return@launch
        val s = _uiState.value

        if (s.availabilityDays.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Select at least one day") }
            return@launch
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        servicesRepository.updateService(
            uid,
            service.id,
            ServiceDraft(
                title = service.title,
                description = service.description,
                hourlyRate = service.hourlyRate,
                categoryId = service.categoryId,
                isActive = service.isActive,
                availabilityDays = s.availabilityDays,
                availabilityStart = s.availabilityStart,
                availabilityEnd = s.availabilityEnd,
                photoUrls = service.photoUrls,
                serviceCenter = service.serviceCenter,
                serviceRadiusKm = service.serviceRadiusKm,
            )
        ).onSuccess {
            _uiState.update { it.copy(isSaving = false, availabilitySheetOpen = false) }
        }.onFailure { e ->
            _uiState.update { it.copy(isSaving = false, errorMessage = e.localizedMessage ?: "Failed to save availability") }
        }
    }
}
