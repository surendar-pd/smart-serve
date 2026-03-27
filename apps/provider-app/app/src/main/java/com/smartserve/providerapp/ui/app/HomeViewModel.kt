package com.smartserve.providerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val activeFilter: RequestStatus? = null,
    val requests: List<ServiceRequest> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: BookingRepository,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadRequests()
    }

    private fun loadRequests() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.getIncomingRequests(uid)
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.localizedMessage)
                    }
                }
                .collect { list ->
                    _uiState.update { it.copy(isLoading = false, requests = list) }
                }
        }
    }

    fun toggleOnline() = _uiState.update { it.copy(isOnline = !it.isOnline) }

    fun setFilter(status: RequestStatus?) =
        _uiState.update { it.copy(activeFilter = status) }

    fun filteredRequests(): List<ServiceRequest> {
        val s = _uiState.value
        return if (s.activeFilter == null) s.requests
        else s.requests.filter { it.status == s.activeFilter }
    }
}