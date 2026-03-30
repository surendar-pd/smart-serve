package com.smartserve.customerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ServiceListUiState {
    data object Loading : ServiceListUiState()
    data class Success(val services: List<CustomerServiceListing>) : ServiceListUiState()
    data class Error(val message: String) : ServiceListUiState()
}

@HiltViewModel
class ServiceListViewModel @Inject constructor(
    private val repo: CustomerServicesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ServiceListUiState>(ServiceListUiState.Loading)
    val state: StateFlow<ServiceListUiState> = _state

    fun load(providerUid: String, providerName: String, categoryId: String = "") {
        _state.value = ServiceListUiState.Loading
        viewModelScope.launch {
            try {
                val services = repo.getServicesForProvider(providerUid, providerName, categoryId)
                _state.value = ServiceListUiState.Success(services)
            } catch (e: Exception) {
                _state.value = ServiceListUiState.Error(
                    e.localizedMessage ?: "Could not load services"
                )
            }
        }
    }
}
