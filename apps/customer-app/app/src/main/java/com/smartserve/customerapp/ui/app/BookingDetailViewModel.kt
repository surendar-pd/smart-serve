package com.smartserve.customerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookingDetailViewModel @Inject constructor(
    private val repo: CustomerServicesRepository,
) : ViewModel() {

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isRating = MutableStateFlow(false)
    val isRating: StateFlow<Boolean> = _isRating

    fun clearError() { _errorMessage.value = null }

    fun deleteBooking(bookingId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isDeleting.value = true
            repo.deleteBooking(bookingId)
                .onSuccess {
                    _isDeleting.value = false
                    onSuccess()
                }
                .onFailure { e ->
                    _isDeleting.value = false
                    _errorMessage.value = e.localizedMessage ?: "Could not delete booking"
                }
        }
    }

    fun rateProvider(bookingId: String, rating: Float, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isRating.value = true
            repo.rateProvider(bookingId, rating)
                .onSuccess {
                    _isRating.value = false
                    onSuccess()
                }
                .onFailure { e ->
                    _isRating.value = false
                    _errorMessage.value = e.localizedMessage ?: "Could not rate provider"
                }
        }
    }
}

