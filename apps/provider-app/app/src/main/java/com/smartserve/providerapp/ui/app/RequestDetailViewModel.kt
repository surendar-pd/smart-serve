package com.smartserve.providerapp.ui.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RequestDetailUiState(
    val request: ServiceRequest? = null,
    val isLoading: Boolean = true,
    val isActing: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface RequestDetailEvent {
    data class NavigateToActive(val bookingId: String) : RequestDetailEvent
    object NavigateBack : RequestDetailEvent
}

@HiltViewModel
class RequestDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: BookingRepository,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val bookingId: String = checkNotNull(savedStateHandle["bookingId"])

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
            .onSuccess { _events.send(RequestDetailEvent.NavigateToActive(bookingId)) }
            .onFailure { e ->
                _uiState.update {
                    it.copy(isActing = false, errorMessage = e.localizedMessage)
                }
            }
    }

    fun decline() = viewModelScope.launch {
        _uiState.update { it.copy(isActing = true) }
        runCatching { repository.declineRequest(bookingId) }
            .onSuccess { _events.send(RequestDetailEvent.NavigateBack) }
            .onFailure { e ->
                _uiState.update {
                    it.copy(isActing = false, errorMessage = e.localizedMessage)
                }
            }
    }
}