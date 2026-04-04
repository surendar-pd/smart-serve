package com.smartserve.providerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RequestDetailUiState(
    val request: ServiceRequest? = null,
    val isLoading: Boolean = true,
    val isActing: Boolean = false,
    val currentRating: Float = 0f,
    val errorMessage: String? = null,
)

sealed interface RequestDetailEvent {
    object NavigateBack : RequestDetailEvent
    data class NavigateToActiveJob(val bookingId: String) : RequestDetailEvent
}

// ── Change 1: AssistedInject instead of @HiltViewModel ────────────────────────
class RequestDetailViewModel @AssistedInject constructor(
    @Assisted private val bookingId: String,   // ← injected directly, no SavedStateHandle
    private val repository: BookingRepository,
    private val auth: FirebaseAuth,
) : ViewModel() {

    // ── Change 2: Declare an AssistedFactory ──────────────────────────────────
    @AssistedFactory
    interface Factory {
        fun create(bookingId: String): RequestDetailViewModel
    }

    private val _uiState = MutableStateFlow(RequestDetailUiState())
    val uiState: StateFlow<RequestDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<RequestDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadRequest()
    }

    private fun loadRequest() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.getIncomingRequests(uid)
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.localizedMessage)
                    }
                }
                .collect { list ->
                    val req = list.firstOrNull { it.id == bookingId }
                    _uiState.update { it.copy(isLoading = false, request = req) }
                }
        }
    }

    fun accept() = viewModelScope.launch {
        _uiState.update { it.copy(isActing = true) }
        runCatching { repository.acceptRequest(bookingId) }
            .onSuccess {
                _uiState.update { it.copy(isActing = false, errorMessage = null) }
                _events.send(RequestDetailEvent.NavigateToActiveJob(bookingId))
            }
            .onFailure { e ->
                _uiState.update { it.copy(isActing = false, errorMessage = e.localizedMessage) }
            }
    }

    fun decline() = viewModelScope.launch {
        _uiState.update { it.copy(isActing = true) }
        runCatching { repository.declineRequest(bookingId) }
            .onSuccess { _events.send(RequestDetailEvent.NavigateBack) }
            .onFailure { e ->
                _uiState.update { it.copy(isActing = false, errorMessage = e.localizedMessage) }
            }
    }

    fun setRating(rating: Float) {
        _uiState.update { it.copy(currentRating = rating) }
    }

    fun completeService() = viewModelScope.launch {
        val rating = _uiState.value.currentRating
        if (rating <= 0f) {
            _uiState.update { it.copy(errorMessage = "Please rate the customer first") }
            return@launch
        }

        _uiState.update { it.copy(isActing = true, errorMessage = null) }

        runCatching {
            repository.rateCustomer(bookingId, rating)
            repository.markDone(bookingId)
        }.onSuccess {
            _uiState.update { it.copy(isActing = false) }
            _events.send(RequestDetailEvent.NavigateBack)
        }.onFailure { e ->
            _uiState.update { it.copy(isActing = false, errorMessage = e.localizedMessage) }
        }
    }
}