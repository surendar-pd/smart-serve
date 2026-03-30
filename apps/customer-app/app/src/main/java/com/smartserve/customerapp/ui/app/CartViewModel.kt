package com.smartserve.customerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val repo: CustomerServicesRepository,
) : ViewModel() {

    private val _isConfirming = MutableStateFlow(false)
    val isConfirming: StateFlow<Boolean> = _isConfirming

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun clearError() { _errorMessage.value = null }

    fun confirm(items: List<CartItem>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isConfirming.value = true
            repo.confirmBookings(items)
                .onSuccess {
                    _isConfirming.value = false
                    onSuccess()
                }
                .onFailure { e ->
                    _isConfirming.value = false
                    _errorMessage.value = e.localizedMessage ?: "Could not confirm bookings"
                }
        }
    }
}
