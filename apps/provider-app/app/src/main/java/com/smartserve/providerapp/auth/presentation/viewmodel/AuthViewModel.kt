//package com.smartserve.auth.presentation.viewmodel
package com.smartserve.providerapp.auth.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.GeoPoint
import com.smartserve.providerapp.auth.data.AuthRepository
import com.smartserve.providerapp.auth.data.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ──────────────────────────────────────────────
// UI State
// ──────────────────────────────────────────────
data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Login fields
    val loginEmail: String = "",
    val loginPassword: String = "",

    // Sign-up fields
    val signUpFullName: String = "",
    val signUpEmail: String = "",
    val signUpPassword: String = "",
    val signUpConfirmPassword: String = "",
    val signUpPhone: String = "",            // required for provider

    // Forgot password
    val forgotEmail: String = "",

    // Navigation
    val navigateTo: AuthNavDestination? = null
)

sealed class AuthNavDestination {
    data class CustomerHome(val uid: String) : AuthNavDestination()
    data class ProviderHome(val uid: String) : AuthNavDestination()
    data class CustomerProfileSetup(val uid: String) : AuthNavDestination()
    data class ProviderProfileSetup(val uid: String) : AuthNavDestination()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // ──────────────────────────────────────────────
    // Field updaters
    // ──────────────────────────────────────────────
    fun onLoginEmailChange(v: String)          = _uiState.update { it.copy(loginEmail = v) }
    fun onLoginPasswordChange(v: String)       = _uiState.update { it.copy(loginPassword = v) }
    fun onSignUpNameChange(v: String)          = _uiState.update { it.copy(signUpFullName = v) }
    fun onSignUpEmailChange(v: String)         = _uiState.update { it.copy(signUpEmail = v) }
    fun onSignUpPasswordChange(v: String)      = _uiState.update { it.copy(signUpPassword = v) }
    fun onSignUpConfirmPasswordChange(v: String) = _uiState.update { it.copy(signUpConfirmPassword = v) }
    fun onSignUpPhoneChange(v: String)         = _uiState.update { it.copy(signUpPhone = v) }
    fun onForgotEmailChange(v: String)         = _uiState.update { it.copy(forgotEmail = v) }
    fun clearError()                           = _uiState.update { it.copy(errorMessage = null) }
    fun clearNavigation()                      = _uiState.update { it.copy(navigateTo = null) }

    // ──────────────────────────────────────────────
    // Login
    // ──────────────────────────────────────────────
    fun login() = viewModelScope.launch {
        val state = _uiState.value
        if (state.loginEmail.isBlank() || state.loginPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill in all fields") }
            return@launch
        }
        _uiState.update { it.copy(isLoading = true) }
        when (val result = repository.signInWithEmail(state.loginEmail, state.loginPassword)) {
            is AuthResult.Success -> {
                val uid = result.user.uid
                val role = repository.getUserRole(uid)
                routeAfterAuth(uid, role)
            }
            is AuthResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            else -> {}
        }
    }

    // ──────────────────────────────────────────────
    // Sign Up – Customer
    // ──────────────────────────────────────────────
    fun signUpCustomer() = viewModelScope.launch {
        val s = _uiState.value
        if (!validateSignUp(requirePhone = false)) return@launch
        _uiState.update { it.copy(isLoading = true) }
        when (val result = repository.signUpWithEmail(s.signUpEmail, s.signUpPassword, s.signUpFullName, "customer")) {
            is AuthResult.Success -> _uiState.update {
                it.copy(isLoading = false, navigateTo = AuthNavDestination.CustomerProfileSetup(result.user.uid))
            }
            is AuthResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            else -> {}
        }
    }

    // ──────────────────────────────────────────────
    // Sign Up – Provider
    // ──────────────────────────────────────────────
    fun signUpProvider() = viewModelScope.launch {
        val s = _uiState.value
        if (s.signUpPhone.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Phone number is required for providers") }
            return@launch
        }
        if (!validateSignUp(requirePhone = true)) return@launch
        _uiState.update { it.copy(isLoading = true) }
        when (val result = repository.signUpWithEmail(
            s.signUpEmail, s.signUpPassword, s.signUpFullName, "provider", s.signUpPhone
        )) {
            is AuthResult.Success -> _uiState.update {
                it.copy(isLoading = false, navigateTo = AuthNavDestination.ProviderProfileSetup(result.user.uid))
            }
            is AuthResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            else -> {}
        }
    }

    // ──────────────────────────────────────────────
    // Google Sign In
    // ──────────────────────────────────────────────
    fun signInWithGoogle(idToken: String, role: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        when (val result = repository.signInWithGoogle(idToken, role)) {
            is AuthResult.Success -> routeAfterAuth(result.user.uid, role)
            is AuthResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            else -> {}
        }
    }

    // ──────────────────────────────────────────────
    // Forgot Password
    // ──────────────────────────────────────────────
    fun sendPasswordReset() = viewModelScope.launch {
        val email = _uiState.value.forgotEmail
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email") }
            return@launch
        }
        _uiState.update { it.copy(isLoading = true) }
        repository.sendPasswordReset(email)
            .onSuccess {
                _uiState.update {
                    it.copy(isLoading = false, successMessage = "Reset link sent — it expires in 15 minutes")
                }
            }
            .onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
    }

    // ──────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────
    private suspend fun routeAfterAuth(uid: String, role: String) {
        val profileDone = repository.isProfileSetupComplete(uid, role)
        val destination = if (!profileDone) {
            if (role == "provider") AuthNavDestination.ProviderProfileSetup(uid)
            else AuthNavDestination.CustomerProfileSetup(uid)
        } else {
            if (role == "provider") AuthNavDestination.ProviderHome(uid)
            else AuthNavDestination.CustomerHome(uid)
        }
        _uiState.update { it.copy(isLoading = false, navigateTo = destination) }
    }

    private fun validateSignUp(requirePhone: Boolean): Boolean {
        val s = _uiState.value
        return when {
            s.signUpFullName.isBlank()    -> { _uiState.update { it.copy(errorMessage = "Full name is required") }; false }
            s.signUpEmail.isBlank()       -> { _uiState.update { it.copy(errorMessage = "Email is required") }; false }
            s.signUpPassword.length < 6   -> { _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters") }; false }
            s.signUpPassword != s.signUpConfirmPassword -> { _uiState.update { it.copy(errorMessage = "Passwords do not match") }; false }
            requirePhone && s.signUpPhone.isBlank() -> { _uiState.update { it.copy(errorMessage = "Phone number is required") }; false }
            else -> true
        }
    }
}

// ──────────────────────────────────────────────
// Customer Profile Setup ViewModel
// ──────────────────────────────────────────────
data class CustomerProfileUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val photoUri: Uri? = null,
    val phone: String = "",
    val homeAddress: String = "",
    val locationAwareness: Boolean = true,
    val pushNotifications: Boolean = true,
    val navigateToHome: Boolean = false
)

@HiltViewModel
class CustomerProfileViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerProfileUiState())
    val state: StateFlow<CustomerProfileUiState> = _state.asStateFlow()

    fun onPhotoSelected(uri: Uri?)      = _state.update { it.copy(photoUri = uri) }
    fun onPhoneChange(v: String)        = _state.update { it.copy(phone = v) }
    fun onHomeAddressChange(v: String)  = _state.update { it.copy(homeAddress = v) }
    fun onLocationToggle(v: Boolean)    = _state.update { it.copy(locationAwareness = v) }
    fun onNotifToggle(v: Boolean)       = _state.update { it.copy(pushNotifications = v) }
    fun clearNavigation()               = _state.update { it.copy(navigateToHome = false) }

    fun saveAndStart(uid: String) = viewModelScope.launch {
        val s = _state.value
        if (s.homeAddress.isBlank()) {
            _state.update { it.copy(errorMessage = "Please enter your home address") }
            return@launch
        }
        _state.update { it.copy(isLoading = true) }
        repository.saveCustomerProfile(
            uid = uid,
            phone = s.phone.ifBlank { null },
            homeAddress = s.homeAddress,
            locationAwareness = s.locationAwareness,
            pushNotifications = s.pushNotifications,
            photoUrl = s.photoUri?.toString()
        ).onSuccess {
            _state.update { it.copy(isLoading = false, navigateToHome = true) }
        }.onFailure { e ->
            _state.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
        }
    }
}

// ──────────────────────────────────────────────
// Provider Profile Setup ViewModel
// ──────────────────────────────────────────────
data class ProviderProfileUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val photoUri: Uri? = null,
    val serviceCategory: String = "",
    val serviceDescription: String = "",
    val hourlyRate: String = "",
    val serviceCenter: GeoPoint? = null,
    val serviceRadiusKm: Double = 10.0,
    val availabilityDays: List<String> = emptyList(),
    val availabilityStart: String = "09:00",
    val availabilityEnd: String = "18:00",
    val navigateToHome: Boolean = false
)

@HiltViewModel
class ProviderProfileViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProviderProfileUiState())
    val state: StateFlow<ProviderProfileUiState> = _state.asStateFlow()

    fun onPhotoSelected(uri: Uri?)          = _state.update { it.copy(photoUri = uri) }
    fun onCategoryChange(v: String)         = _state.update { it.copy(serviceCategory = v) }
    fun onDescriptionChange(v: String)      = _state.update { it.copy(serviceDescription = v) }
    fun onHourlyRateChange(v: String)       = _state.update { it.copy(hourlyRate = v) }
    fun onServiceCenterChange(gp: GeoPoint) = _state.update { it.copy(serviceCenter = gp) }
    fun onRadiusChange(r: Double)           = _state.update { it.copy(serviceRadiusKm = r) }
    fun onAvailabilityDaysChange(days: List<String>) = _state.update { it.copy(availabilityDays = days) }
    fun onAvailabilityStartChange(v: String) = _state.update { it.copy(availabilityStart = v) }
    fun onAvailabilityEndChange(v: String)   = _state.update { it.copy(availabilityEnd = v) }
    fun clearNavigation()                    = _state.update { it.copy(navigateToHome = false) }

    fun saveAndStart(uid: String, displayName: String, phone: String) = viewModelScope.launch {
        val s = _state.value
        when {
            s.serviceCategory.isBlank()    -> { _state.update { it.copy(errorMessage = "Select a service category") }; return@launch }
            s.serviceDescription.isBlank() -> { _state.update { it.copy(errorMessage = "Add a service description") }; return@launch }
            s.hourlyRate.toDoubleOrNull() == null -> { _state.update { it.copy(errorMessage = "Enter a valid hourly rate") }; return@launch }
            s.availabilityDays.isEmpty()   -> { _state.update { it.copy(errorMessage = "Select at least one availability day") }; return@launch }
        }
        _state.update { it.copy(isLoading = true) }
        repository.saveProviderProfile(
            uid = uid,
            displayName = displayName,
            phone = phone,
            photoUrl = s.photoUri?.toString(),
            serviceCategory = s.serviceCategory,
            serviceDescription = s.serviceDescription,
            hourlyRate = s.hourlyRate.toDouble(),
            serviceCenter = s.serviceCenter,
            serviceRadiusKm = s.serviceRadiusKm,
            availabilityDays = s.availabilityDays,
            availabilityStart = s.availabilityStart,
            availabilityEnd = s.availabilityEnd
        ).onSuccess {
            _state.update { it.copy(isLoading = false, navigateToHome = true) }
        }.onFailure { e ->
            _state.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
        }
    }
}
