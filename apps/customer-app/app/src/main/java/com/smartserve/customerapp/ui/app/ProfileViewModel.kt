package com.smartserve.customerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.smartserve.sharedauth.AuthCollections
import com.smartserve.sharedauth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.smartserve.sharedauth.AddressValidState
import com.smartserve.sharedauth.GeoResult
import com.smartserve.sharedauth.NominatimGeocoder
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val homeAddress: String = "",
    val locationAwareness: Boolean = true,
    val pushNotifications: Boolean = true,
    val errorMessage: String? = null,
    val savedOk: Boolean = false,
    val addressValidState: AddressValidState = AddressValidState.Idle,
    val addressGeoResult: GeoResult? = null,
    val addressSuggestions: List<GeoResult> = emptyList(),
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val geocoder: NominatimGeocoder,
    private val repo: CustomerServicesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() = viewModelScope.launch {
        try {
            val user = auth.currentUser ?: return@launch
            val doc = firestore.collection(AuthCollections.CUSTOMER_PROFILES).document(user.uid).get().await()
            _state.update {
                it.copy(
                    isLoading = false,
                    name = user.displayName ?: "",
                    email = user.email ?: "",
                    phone = doc.getString("phone") ?: "",
                    homeAddress = doc.getString("homeAddress") ?: "",
                    locationAwareness = doc.getBoolean("locationAwareness") ?: true,
                    pushNotifications = doc.getBoolean("pushNotifications") ?: true,
                    addressValidState = AddressValidState.Idle,
                    addressGeoResult = null,
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
        }
    }

    fun onNameChange(v: String) = _state.update { it.copy(name = v) }
    fun onPhoneChange(v: String) = _state.update { it.copy(phone = v) }
    private var suggestionJob: Job? = null

    fun onAddressChange(v: String) {
        _state.update {
            it.copy(
                homeAddress       = v,
                addressValidState = AddressValidState.Idle,
                addressGeoResult  = null,
                addressSuggestions = emptyList(),
            )
        }
        suggestionJob?.cancel()
        if (v.trim().length >= 4) {
            suggestionJob = viewModelScope.launch {
                delay(450L)
                val results = geocoder.searchSuggestions(v.trim())
                _state.update { it.copy(addressSuggestions = results) }
            }
        }
    }

    fun onSuggestionSelected(result: GeoResult) {
        suggestionJob?.cancel()
        _state.update {
            it.copy(
                homeAddress        = result.shortLabel,
                addressSuggestions = emptyList(),
                addressValidState  = if (result.isInOttawa) AddressValidState.Valid
                                     else AddressValidState.NotOttawa,
                addressGeoResult   = result,
            )
        }
    }

    fun validateAddress() = viewModelScope.launch {
        val addr = _state.value.homeAddress.trim()
        if (addr.isBlank()) return@launch
        _state.update { it.copy(addressValidState = AddressValidState.Validating, addressGeoResult = null) }
        val result = geocoder.forwardGeocode(addr)
        _state.update {
            when {
                result == null -> it.copy(
                    addressValidState = AddressValidState.NotFound,
                    addressGeoResult  = null,
                )
                !result.isInOttawa -> it.copy(
                    addressValidState = AddressValidState.NotOttawa,
                    addressGeoResult  = result,
                )
                else -> it.copy(
                    addressValidState = AddressValidState.Valid,
                    addressGeoResult  = result,
                    homeAddress       = result.shortLabel,
                )
            }
        }
    }
    fun onLocationToggle(v: Boolean) = _state.update { it.copy(locationAwareness = v) }
    fun onNotifToggle(v: Boolean) = _state.update { it.copy(pushNotifications = v) }
    fun clearSaved() = _state.update { it.copy(savedOk = false) }
    fun clearError() = _state.update { it.copy(errorMessage = null) }

    fun save() = viewModelScope.launch {
        val s = _state.value
        val user = auth.currentUser ?: return@launch
        _state.update { it.copy(isSaving = true) }
        try {
            if (s.name.isNotBlank() && s.name != (user.displayName ?: "")) {
                val request = UserProfileChangeRequest.Builder().setDisplayName(s.name).build()
                user.updateProfile(request).await()
            }
            firestore.collection(AuthCollections.CUSTOMER_PROFILES).document(user.uid)
                .set(
                    mapOf(
                        "phone" to s.phone,
                        "homeAddress" to s.homeAddress,
                        "locationAwareness" to s.locationAwareness,
                        "pushNotifications" to s.pushNotifications,
                        // Keep a denormalized name on the profile for easy lookup.
                        "displayName" to (auth.currentUser?.displayName ?: s.name).orEmpty(),
                    ),
                    SetOptions.merge(),
                ).await()
            // Provider app resolves customer name from `customer_profiles` then `users`.
            // Ensure `users/{uid}.displayName` is present too.
            val resolvedName = (auth.currentUser?.displayName ?: s.name).trim()
            if (resolvedName.isNotBlank()) {
                firestore.collection(AuthCollections.USERS).document(user.uid)
                    .set(mapOf("displayName" to resolvedName), SetOptions.merge())
                    .await()
            }
            repo.setHomeAddressCache(s.homeAddress)
            _state.update { it.copy(isSaving = false, savedOk = true) }
        } catch (e: Exception) {
            _state.update { it.copy(isSaving = false, errorMessage = e.localizedMessage) }
        }
    }

    fun signOut() = authRepository.signOut()
}
