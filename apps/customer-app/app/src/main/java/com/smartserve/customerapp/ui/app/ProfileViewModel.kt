package com.smartserve.customerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.smartserve.sharedauth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() = viewModelScope.launch {
        try {
            val user = auth.currentUser ?: return@launch
            val doc = firestore.collection("customer_profiles").document(user.uid).get().await()
            _state.update {
                it.copy(
                    isLoading = false,
                    name = user.displayName ?: "",
                    email = user.email ?: "",
                    phone = doc.getString("phone") ?: "",
                    homeAddress = doc.getString("homeAddress") ?: "",
                    locationAwareness = doc.getBoolean("locationAwareness") ?: true,
                    pushNotifications = doc.getBoolean("pushNotifications") ?: true,
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
        }
    }

    fun onNameChange(v: String) = _state.update { it.copy(name = v) }
    fun onPhoneChange(v: String) = _state.update { it.copy(phone = v) }
    fun onAddressChange(v: String) = _state.update { it.copy(homeAddress = v) }
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
            firestore.collection("customer_profiles").document(user.uid)
                .set(
                    mapOf(
                        "phone" to s.phone,
                        "homeAddress" to s.homeAddress,
                        "locationAwareness" to s.locationAwareness,
                        "pushNotifications" to s.pushNotifications,
                    ),
                    SetOptions.merge(),
                ).await()
            _state.update { it.copy(isSaving = false, savedOk = true) }
        } catch (e: Exception) {
            _state.update { it.copy(isSaving = false, errorMessage = e.localizedMessage) }
        }
    }

    fun signOut() = authRepository.signOut()
}
