package com.smartserve.providerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.smartserve.sharedauth.AuthRepository
import com.smartserve.sharedauth.DefaultServiceAvailability
import com.smartserve.sharedauth.ServiceCategoryOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServiceEditorUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val deleting: Boolean = false,
    val errorMessage: String? = null,
    val savedOk: Boolean = false,
    val deletedOk: Boolean = false,
    val title: String = "",
    val description: String = "",
    val hourlyRate: String = "",
    val categoryId: String = "",
    val isActive: Boolean = true,
    val availabilityDays: List<String> = DefaultServiceAvailability.DAYS,
    val availabilityStart: String = DefaultServiceAvailability.START,
    val availabilityEnd: String = DefaultServiceAvailability.END,
    val categories: List<ServiceCategoryOption> = emptyList(),
    val categoriesLoading: Boolean = true,
)

@HiltViewModel
class ServiceEditorViewModel @Inject constructor(
    private val repository: ProviderServicesRepository,
    private val authRepository: AuthRepository,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val _state = MutableStateFlow(ServiceEditorUiState())
    val state: StateFlow<ServiceEditorUiState> = _state.asStateFlow()

    private var editingServiceId: String? = null

    fun load(serviceId: String?) = viewModelScope.launch {
        editingServiceId = serviceId
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _state.value = ServiceEditorUiState(loading = false, errorMessage = "Not signed in", categoriesLoading = false)
            return@launch
        }

        _state.update {
            ServiceEditorUiState(
                loading = true,
                categoriesLoading = true,
                errorMessage = null,
                savedOk = false,
                deletedOk = false,
            )
        }

        authRepository.listServiceCategories()
            .onSuccess { list ->
                _state.update { it.copy(categories = list, categoriesLoading = false) }
            }
            .onFailure { e ->
                _state.update {
                    it.copy(
                        categoriesLoading = false,
                        errorMessage = e.localizedMessage ?: "Could not load categories",
                    )
                }
            }

        if (serviceId == null) {
            _state.update {
                it.copy(
                    loading = false,
                    title = "",
                    description = "",
                    hourlyRate = "",
                    categoryId = "",
                    isActive = true,
                    availabilityDays = DefaultServiceAvailability.DAYS,
                    availabilityStart = DefaultServiceAvailability.START,
                    availabilityEnd = DefaultServiceAvailability.END,
                )
            }
            return@launch
        }

        val row = repository.getService(serviceId)
        if (row == null) {
            _state.update { it.copy(loading = false, errorMessage = "Service not found") }
            return@launch
        }

        _state.update {
            it.copy(
                loading = false,
                title = row.title,
                description = row.description,
                hourlyRate = row.hourlyRate.toString(),
                categoryId = row.categoryId,
                isActive = row.isActive,
                availabilityDays = row.availabilityDays,
                availabilityStart = row.availabilityStart,
                availabilityEnd = row.availabilityEnd,
            )
        }
    }

    fun onTitleChange(v: String) = _state.update { it.copy(title = v) }
    fun onDescriptionChange(v: String) = _state.update { it.copy(description = v) }
    fun onHourlyRateChange(v: String) = _state.update { it.copy(hourlyRate = v) }
    fun onCategoryChange(id: String) = _state.update { it.copy(categoryId = id) }
    fun onActiveChange(v: Boolean) = _state.update { it.copy(isActive = v) }
    fun onAvailabilityDaysChange(days: List<String>) = _state.update { it.copy(availabilityDays = days) }
    fun onAvailabilityStartChange(v: String) = _state.update { it.copy(availabilityStart = v) }
    fun onAvailabilityEndChange(v: String) = _state.update { it.copy(availabilityEnd = v) }
    fun clearError() = _state.update { it.copy(errorMessage = null) }
    fun consumeSaved() = _state.update { it.copy(savedOk = false) }
    fun consumeDeleted() = _state.update { it.copy(deletedOk = false) }

    fun save() = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch
        val s = _state.value
        when {
            s.title.trim().isBlank() -> {
                _state.update { it.copy(errorMessage = "Add a title") }
                return@launch
            }
            s.categoryId.isBlank() -> {
                _state.update { it.copy(errorMessage = "Select a category") }
                return@launch
            }
            s.hourlyRate.toDoubleOrNull() == null -> {
                _state.update { it.copy(errorMessage = "Enter a valid hourly rate") }
                return@launch
            }
            s.availabilityDays.isEmpty() -> {
                _state.update { it.copy(errorMessage = "Select at least one availability day") }
                return@launch
            }
        }

        val draft = ServiceDraft(
            title = s.title,
            description = s.description,
            hourlyRate = s.hourlyRate.toDouble(),
            categoryId = s.categoryId,
            isActive = s.isActive,
            availabilityDays = s.availabilityDays,
            availabilityStart = s.availabilityStart,
            availabilityEnd = s.availabilityEnd,
        )

        _state.update { it.copy(saving = true, errorMessage = null) }
        val id = editingServiceId
        val result = if (id == null) {
            repository.createService(uid, draft)
        } else {
            repository.updateService(uid, id, draft)
        }

        result
            .onSuccess {
                _state.update { it.copy(saving = false, savedOk = true) }
            }
            .onFailure { e ->
                _state.update {
                    it.copy(saving = false, errorMessage = e.localizedMessage ?: "Save failed")
                }
            }
    }

    fun delete() = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch
        val id = editingServiceId ?: return@launch
        _state.update { it.copy(deleting = true, errorMessage = null) }
        repository.deleteService(uid, id)
            .onSuccess {
                _state.update { it.copy(deleting = false, deletedOk = true) }
            }
            .onFailure { e ->
                _state.update {
                    it.copy(deleting = false, errorMessage = e.localizedMessage ?: "Delete failed")
                }
            }
    }
}
