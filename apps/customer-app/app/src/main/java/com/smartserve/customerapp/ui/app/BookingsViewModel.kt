package com.smartserve.customerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BookingsUiState {
    data object Loading : BookingsUiState()
    data class Success(val bookings: List<CustomerBooking>) : BookingsUiState()
    data class Error(val message: String) : BookingsUiState()
}

@HiltViewModel
class BookingsViewModel @Inject constructor(
    private val repo: CustomerServicesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<BookingsUiState>(BookingsUiState.Loading)
    val state: StateFlow<BookingsUiState> = _state

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = BookingsUiState.Loading
            try {
                val bookings = repo.getMyBookings()
                _state.value = BookingsUiState.Success(bookings)
            } catch (e: Exception) {
                _state.value = BookingsUiState.Error(
                    e.localizedMessage ?: "Could not load bookings"
                )
            }
        }
    }
}
