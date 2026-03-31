package com.smartserve.providerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActiveJobUiState(
    val request: ServiceRequest? = null,
    val isLoading: Boolean = true,
    val isMarkingDone: Boolean = false,
    val currentRating: Float = 0f,
    val errorMessage: String? = null,
)

sealed interface ActiveJobEvent {
    object NavigateToBookings : ActiveJobEvent
    data class ShowSnackbar(val message: String) : ActiveJobEvent
}

// ✅ No @HiltViewModel, no SavedStateHandle
class ActiveJobViewModel @AssistedInject constructor(
    @Assisted private val bookingId: String,
    private val repository: BookingRepository,
    private val auth: FirebaseAuth,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(bookingId: String): ActiveJobViewModel
    }

    private val _uiState = MutableStateFlow(ActiveJobUiState())
    val uiState: StateFlow<ActiveJobUiState> = _uiState.asStateFlow()

    private val _events = Channel<ActiveJobEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { loadJob() }

    private fun loadJob() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.getIncomingRequests(uid)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
                }
                .collect { list ->
                    val req = list.firstOrNull { it.id == bookingId }
                    _uiState.update { it.copy(isLoading = false, request = req) }
                }
        }
    }

    fun markDone() = viewModelScope.launch {
        _uiState.update { it.copy(isMarkingDone = true) }
        runCatching { repository.markDone(bookingId) }
            .onSuccess {
                _events.send(ActiveJobEvent.ShowSnackbar("Job marked complete!"))
                _events.send(ActiveJobEvent.NavigateToBookings)
            }
            .onFailure { e ->
                _uiState.update { it.copy(isMarkingDone = false, errorMessage = e.localizedMessage) }
            }
    }

    fun logCall() = viewModelScope.launch {
        runCatching { repository.logCall(bookingId) }
    }

    fun setRating(rating: Float) {
        _uiState.update { it.copy(currentRating = rating) }
    }

    fun completeService() = viewModelScope.launch {
        val rating = _uiState.value.currentRating
        if (rating <= 0f) {
            _events.send(ActiveJobEvent.ShowSnackbar("Please rate the customer first"))
            return@launch
        }

        _uiState.update { it.copy(isMarkingDone = true) }

        runCatching {
            repository.rateCustomer(bookingId, rating)
            repository.markDone(bookingId)
        }.onSuccess {
            _events.send(ActiveJobEvent.ShowSnackbar("Job marked complete!"))
            _events.send(ActiveJobEvent.NavigateToBookings)
        }.onFailure { e ->
            _uiState.update { it.copy(isMarkingDone = false, errorMessage = e.localizedMessage) }
        }
    }
}