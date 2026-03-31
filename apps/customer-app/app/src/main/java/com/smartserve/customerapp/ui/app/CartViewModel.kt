package com.smartserve.customerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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

    val cartItems: StateFlow<List<CartItem>> = repo.observeCartItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clearError() { _errorMessage.value = null }

    fun addToCart(item: CartItem, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val dup = cartItems.value.any { existing ->
                existing.providerUid == item.providerUid &&
                    existing.serviceId == item.serviceId &&
                    existing.date == item.date &&
                    existing.time == item.time
            }
            if (dup) return@launch
            repo.addCartLine(item)
                .onSuccess { onSuccess() }
                .onFailure { e ->
                    _errorMessage.value = e.localizedMessage ?: "Could not add to cart"
                }
        }
    }

    fun removeFromCart(item: CartItem) {
        viewModelScope.launch {
            val id = item.lineDocumentId ?: return@launch
            repo.removeCartLine(id).onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "Could not remove item"
            }
        }
    }

    fun confirm(items: List<CartItem>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isConfirming.value = true
            repo.confirmBookings(items)
                .onSuccess {
                    repo.clearCustomerCart()
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
