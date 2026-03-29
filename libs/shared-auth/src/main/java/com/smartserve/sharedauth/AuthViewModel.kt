package com.smartserve.sharedauth

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SMARTSERVE_AUTH"

private fun wrongAppRoleMessage(expectedAppRole: String): String =
    when (expectedAppRole) {
        UserRole.CUSTOMER.value ->
            "This account isn't a customer account. Sign in with the SmartServe Provider app instead."
        UserRole.PROVIDER.value ->
            "This account isn't a provider account. Sign in with the SmartServe Customer app instead."
        else -> "This account can't be used in this app."
    }

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val loginEmail: String = "",
    val loginPassword: String = "",
    val signUpFullName: String = "",
    val signUpEmail: String = "",
    val signUpPassword: String = "",
    val signUpConfirmPassword: String = "",
    val signUpPhone: String = "",
    val forgotEmail: String = "",
    val navigateTo: AuthNavDestination? = null,
)

sealed class AuthNavDestination {
    data class CustomerHome(val uid: String) : AuthNavDestination()
    data class ProviderHome(val uid: String) : AuthNavDestination()
    data class CustomerProfileSetup(val uid: String) : AuthNavDestination()
    data class ProviderProfileSetup(val uid: String) : AuthNavDestination()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    @ExpectedAppRole private val expectedAppRole: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(navigateTo = null) }
        Log.d(TAG, "AuthViewModel created — expectedAppRole=$expectedAppRole")
    }

    fun onLoginEmailChange(v: String) = _uiState.update { it.copy(loginEmail = v) }
    fun onLoginPasswordChange(v: String) = _uiState.update { it.copy(loginPassword = v) }
    fun onSignUpNameChange(v: String) = _uiState.update { it.copy(signUpFullName = v) }
    fun onSignUpEmailChange(v: String) = _uiState.update { it.copy(signUpEmail = v) }
    fun onSignUpPasswordChange(v: String) = _uiState.update { it.copy(signUpPassword = v) }
    fun onSignUpConfirmPasswordChange(v: String) =
        _uiState.update { it.copy(signUpConfirmPassword = v) }
    fun onSignUpPhoneChange(v: String) = _uiState.update { it.copy(signUpPhone = v) }
    fun onForgotEmailChange(v: String) = _uiState.update { it.copy(forgotEmail = v) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun clearNavigation() = _uiState.update { it.copy(navigateTo = null) }

    fun login() = viewModelScope.launch {
        Log.d(TAG, "login() called — expectedAppRole=$expectedAppRole")
        val state = _uiState.value
        if (state.loginEmail.isBlank() || state.loginPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill in all fields") }
            return@launch
        }
        _uiState.update { it.copy(isLoading = true) }
        when (val result = repository.signInWithEmail(state.loginEmail, state.loginPassword)) {
            is AuthResult.Success -> {
                val uid = result.user.uid
                Log.d(TAG, "signInWithEmail SUCCESS — uid=$uid")
                val role = repository.getUserRole(uid)
                Log.d(TAG, "getUserRole returned — role=$role")
                val allowed = AppRoleGate.isAllowed(expectedAppRole, role)
                Log.d(TAG, "AppRoleGate.isAllowed(expected=$expectedAppRole, actual=$role) = $allowed")
                if (!allowed) {
                    Log.e(TAG, "ROLE MISMATCH — calling signOut()")
                    repository.signOut()
                    _uiState.update {
                        it.copy(
                            isLoading    = false,
                            errorMessage = wrongAppRoleMessage(expectedAppRole),
                        )
                    }
                    return@launch
                }
                Log.d(TAG, "Role OK — calling routeAfterAuth()")
                routeAfterAuth(uid, role)
            }
            is AuthResult.Error -> {
                Log.e(TAG, "signInWithEmail FAILED — ${result.message}")
                _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
            is AuthResult.Loading -> {}
        }
    }

    fun signUpCustomer() = viewModelScope.launch {
        Log.d(TAG, "signUpCustomer() called")
        val s = _uiState.value
        if (!validateSignUp(requirePhone = false)) return@launch
        _uiState.update { it.copy(isLoading = true) }
        when (val result = repository.signUpWithEmail(
            s.signUpEmail, s.signUpPassword, s.signUpFullName, "customer"
        )) {
            is AuthResult.Success -> {
                Log.d(TAG, "signUpCustomer SUCCESS — uid=${result.user.uid}")
                _uiState.update {
                    it.copy(
                        isLoading  = false,
                        navigateTo = AuthNavDestination.CustomerProfileSetup(result.user.uid),
                    )
                }
            }
            is AuthResult.Error -> {
                Log.e(TAG, "signUpCustomer FAILED — ${result.message}")
                _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
            is AuthResult.Loading -> {}
        }
    }

    fun signUpProvider() = viewModelScope.launch {
        if (_uiState.value.isLoading) return@launch
        Log.d(TAG, "signUpProvider() called")
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
            is AuthResult.Success -> {
                Log.d(TAG, "signUpProvider SUCCESS — uid=${result.user.uid}")
                _uiState.update {
                    it.copy(
                        isLoading  = false,
                        navigateTo = AuthNavDestination.ProviderProfileSetup(result.user.uid),
                    )
                }
            }
            is AuthResult.Error -> {
                Log.e(TAG, "signUpProvider FAILED — ${result.message}")
                _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
            is AuthResult.Loading -> {}
        }
    }

    fun signInWithGoogle(idToken: String, role: String) = viewModelScope.launch {
        Log.d(TAG, "signInWithGoogle() called — role=$role")
        _uiState.update { it.copy(isLoading = true) }
        when (val result = repository.signInWithGoogle(idToken, role)) {
            is AuthResult.Success -> {
                val uid = result.user.uid
                Log.d(TAG, "signInWithGoogle SUCCESS — uid=$uid")
                val actualRole = repository.getUserRole(uid)
                Log.d(TAG, "getUserRole returned — actualRole=$actualRole")
                val allowed = AppRoleGate.isAllowed(expectedAppRole, actualRole)
                Log.d(TAG, "AppRoleGate.isAllowed(expected=$expectedAppRole, actual=$actualRole) = $allowed")
                if (!allowed) {
                    Log.e(TAG, "ROLE MISMATCH (Google) — calling signOut()")
                    repository.signOut()
                    _uiState.update {
                        it.copy(
                            isLoading    = false,
                            errorMessage = wrongAppRoleMessage(expectedAppRole),
                        )
                    }
                    return@launch
                }
                routeAfterAuth(uid, actualRole)
            }
            is AuthResult.Error -> {
                Log.e(TAG, "signInWithGoogle FAILED — ${result.message}")
                _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
            is AuthResult.Loading -> {}
        }
    }

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
                    it.copy(
                        isLoading      = false,
                        successMessage = "Reset link sent — it expires in 15 minutes",
                    )
                }
            }
            .onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
    }

    private suspend fun routeAfterAuth(uid: String, resolvedRole: String) {
        Log.d(TAG, "routeAfterAuth() — uid=$uid resolvedRole=$resolvedRole expectedAppRole=$expectedAppRole")
        val roleForRouting =
            if (resolvedRole == UserRole.BOTH.value) expectedAppRole else resolvedRole
        val profileDone = repository.isProfileSetupComplete(uid, roleForRouting)
        Log.d(TAG, "isProfileSetupComplete=$profileDone for roleForRouting=$roleForRouting")
        val destination = if (!profileDone) {
            if (roleForRouting == UserRole.PROVIDER.value)
                AuthNavDestination.ProviderProfileSetup(uid)
            else
                AuthNavDestination.CustomerProfileSetup(uid)
        } else {
            if (roleForRouting == UserRole.PROVIDER.value)
                AuthNavDestination.ProviderHome(uid)
            else
                AuthNavDestination.CustomerHome(uid)
        }
        Log.d(TAG, "navigateTo set to: $destination")
        _uiState.update { it.copy(isLoading = false, navigateTo = destination) }
    }

    private fun validateSignUp(requirePhone: Boolean): Boolean {
        val s = _uiState.value
        return when {
            s.signUpFullName.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Full name is required") }
                false
            }
            s.signUpEmail.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Email is required") }
                false
            }
            s.signUpPassword.length < 6 -> {
                _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters") }
                false
            }
            s.signUpPassword != s.signUpConfirmPassword -> {
                _uiState.update { it.copy(errorMessage = "Passwords do not match") }
                false
            }
            requirePhone && s.signUpPhone.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Phone number is required") }
                false
            }
            else -> true
        }
    }

    fun signOut() {
        Log.d(TAG, "signOut() called")
        repository.signOut()
    }
}

// ── CustomerProfileViewModel ──────────────────────────────────────────────────

data class CustomerProfileUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val photoUri: Uri? = null,
    val phone: String = "",
    val homeAddress: String = "",
    val locationAwareness: Boolean = true,
    val pushNotifications: Boolean = true,
)

@HiltViewModel
class CustomerProfileViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerProfileUiState())
    val state: StateFlow<CustomerProfileUiState> = _state.asStateFlow()

    private val _onboardingCompleted = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow    = BufferOverflow.DROP_OLDEST,
    )
    val onboardingCompleted: SharedFlow<Unit> = _onboardingCompleted.asSharedFlow()

    fun onPhotoSelected(uri: Uri?) = _state.update { it.copy(photoUri = uri) }
    fun onPhoneChange(v: String) = _state.update { it.copy(phone = v) }
    fun onHomeAddressChange(v: String) = _state.update { it.copy(homeAddress = v) }
    fun onLocationToggle(v: Boolean) = _state.update { it.copy(locationAwareness = v) }
    fun onNotifToggle(v: Boolean) = _state.update { it.copy(pushNotifications = v) }
    fun clearError() = _state.update { it.copy(errorMessage = null) }

    fun completeOnboarding(uid: String) = viewModelScope.launch {
        val s = _state.value
        if (s.homeAddress.isBlank()) {
            _state.update { it.copy(errorMessage = "Please enter your home address") }
            return@launch
        }
        _state.update { it.copy(isLoading = true) }
        repository.saveCustomerProfile(
            uid               = uid,
            phone             = s.phone.ifBlank { null },
            homeAddress       = s.homeAddress,
            locationAwareness = s.locationAwareness,
            pushNotifications = s.pushNotifications,
            photoUrl          = s.photoUri?.toString(),
        ).onSuccess {
            _state.update { it.copy(isLoading = false) }
            _onboardingCompleted.emit(Unit)
        }.onFailure { e ->
            _state.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
        }
    }
}

// ── ProviderProfileViewModel ──────────────────────────────────────────────────

data class ProviderProfileUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val photoUri: Uri? = null,
    /** Firestore `categories` document id. */
    val serviceCategory: String = "",
    val categoryOptions: List<ServiceCategoryOption> = emptyList(),
    val categoriesLoading: Boolean = true,
    val isSavingCategory: Boolean = false,
    val serviceDescription: String = "",
    val hourlyRate: String = "",
    val serviceCenter: GeoPoint? = null,
    val serviceRadiusKm: Double = 10.0,
)

@HiltViewModel
class ProviderProfileViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProviderProfileUiState())
    val state: StateFlow<ProviderProfileUiState> = _state.asStateFlow()

    private val _onboardingCompleted = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow    = BufferOverflow.DROP_OLDEST,
    )
    val onboardingCompleted: SharedFlow<Unit> = _onboardingCompleted.asSharedFlow()

    private val _addCategoryCompleted = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    /** Emitted when [addServiceCategory] succeeds (e.g. to close the add-category dialog). */
    val addCategoryCompleted: SharedFlow<Unit> = _addCategoryCompleted.asSharedFlow()

    init {
        loadCategories()
    }

    fun loadCategories() = viewModelScope.launch {
        _state.update { it.copy(categoriesLoading = true) }
        repository.listServiceCategories()
            .onSuccess { list ->
                _state.update { it.copy(categoryOptions = list, categoriesLoading = false) }
            }
            .onFailure { e ->
                _state.update {
                    it.copy(
                        categoriesLoading = false,
                        errorMessage = e.localizedMessage ?: "Could not load categories",
                    )
                }
            }
    }

    fun addServiceCategory(label: String) = viewModelScope.launch {
        _state.update { it.copy(isSavingCategory = true, errorMessage = null) }
        repository.addServiceCategory(label)
            .onSuccess { option ->
                val merged = (_state.value.categoryOptions + option).sortedBy { o -> o.label.lowercase() }
                _state.update {
                    it.copy(
                        categoryOptions = merged,
                        serviceCategory = option.id,
                        isSavingCategory = false,
                    )
                }
                _addCategoryCompleted.emit(Unit)
            }
            .onFailure { e ->
                _state.update {
                    it.copy(
                        isSavingCategory = false,
                        errorMessage = e.localizedMessage ?: "Could not add category",
                    )
                }
            }
    }

    fun onPhotoSelected(uri: Uri?) = _state.update { it.copy(photoUri = uri) }
    fun onCategoryChange(v: String) = _state.update { it.copy(serviceCategory = v) }
    fun onDescriptionChange(v: String) = _state.update { it.copy(serviceDescription = v) }
    fun onHourlyRateChange(v: String) = _state.update { it.copy(hourlyRate = v) }
    fun onServiceCenterChange(gp: GeoPoint) = _state.update { it.copy(serviceCenter = gp) }
    fun onRadiusChange(r: Double) = _state.update { it.copy(serviceRadiusKm = r) }
    fun clearError() = _state.update { it.copy(errorMessage = null) }

    fun completeOnboarding(uid: String, displayName: String, phone: String) =
        viewModelScope.launch {
            Log.d(TAG, "ProviderProfileViewModel.completeOnboarding() — uid=$uid")
            val s = _state.value
            when {
                s.serviceCategory.isBlank() -> {
                    _state.update { it.copy(errorMessage = "Select a service category") }
                    return@launch
                }
                s.serviceDescription.isBlank() -> {
                    _state.update { it.copy(errorMessage = "Add a service description") }
                    return@launch
                }
                s.hourlyRate.toDoubleOrNull() == null -> {
                    _state.update { it.copy(errorMessage = "Enter a valid hourly rate") }
                    return@launch
                }
            }
            _state.update { it.copy(isLoading = true) }
            repository.saveProviderProfile(
                uid                = uid,
                displayName        = displayName,
                phone              = phone,
                photoUrl           = s.photoUri?.toString(),
                serviceCategory    = s.serviceCategory,
                serviceDescription = s.serviceDescription,
                hourlyRate         = s.hourlyRate.toDouble(),
                serviceCenter      = s.serviceCenter,
                serviceRadiusKm    = s.serviceRadiusKm,
            ).onSuccess {
                Log.d(TAG, "saveProviderProfile SUCCESS")
                _state.update { it.copy(isLoading = false) }
                _onboardingCompleted.emit(Unit)
            }.onFailure { e ->
                Log.e(TAG, "saveProviderProfile FAILED — ${e.localizedMessage}")
                _state.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
}