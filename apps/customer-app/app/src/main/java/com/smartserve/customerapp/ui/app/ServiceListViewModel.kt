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
    data class Success(
        val services: List<CustomerServiceListing>,
        val favoriteServiceIds: Set<String> = emptySet(),
    ) : ServiceListUiState()
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
                val favoriteIds = repo.getFavoriteServiceIds()
                _state.value = ServiceListUiState.Success(services, favoriteIds)
            } catch (e: Exception) {
                _state.value = ServiceListUiState.Error(
                    e.localizedMessage ?: "Could not load services"
                )
            }
        }
    }

    fun toggleFavorite(service: CustomerServiceListing) {
        val current = _state.value as? ServiceListUiState.Success ?: return
        val currentlyFav = service.serviceId in current.favoriteServiceIds

        _state.value = current.copy(
            favoriteServiceIds = if (currentlyFav) {
                current.favoriteServiceIds - service.serviceId
            } else {
                current.favoriteServiceIds + service.serviceId
            },
        )

        viewModelScope.launch {
            val result = if (currentlyFav) {
                repo.removeFavoriteService(service.serviceId)
            } else {
                repo.upsertFavoriteService(service)
            }

            if (result.isFailure) {
                val fallback = _state.value as? ServiceListUiState.Success ?: return@launch
                _state.value = fallback.copy(
                    favoriteServiceIds = if (currentlyFav) {
                        fallback.favoriteServiceIds + service.serviceId
                    } else {
                        fallback.favoriteServiceIds - service.serviceId
                    },
                )
            }
        }
    }
}
