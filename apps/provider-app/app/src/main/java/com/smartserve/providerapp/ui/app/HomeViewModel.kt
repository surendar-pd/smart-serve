package com.smartserve.providerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.smartserve.sharedauth.AuthCollections
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val activeFilter: RequestStatus = RequestStatus.PENDING,
    val requests: List<ServiceRequest> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: BookingRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadOnlineStatus()
        loadRequests()
    }

    private fun loadOnlineStatus() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            runCatching {
                firestore.collection(AuthCollections.PROVIDER_PROFILES)
                    .document(uid)
                    .get()
                    .await()
            }.onSuccess { doc ->
                val online = doc.getBoolean("isOnline")
                    ?: doc.getBoolean("online")
                    ?: true
                _uiState.update { it.copy(isOnline = online) }
            }
        }
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

    fun toggleOnline() {
        val uid = auth.currentUser?.uid ?: return
        val nextOnline = !_uiState.value.isOnline
        _uiState.update { it.copy(isOnline = nextOnline) }

        viewModelScope.launch {
            runCatching {
                firestore.collection(AuthCollections.PROVIDER_PROFILES)
                    .document(uid)
                    .set(mapOf("isOnline" to nextOnline, "online" to nextOnline), com.google.firebase.firestore.SetOptions.merge())
                    .await()
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isOnline = !nextOnline,
                        errorMessage = it.errorMessage ?: "Unable to update online status",
                    )
                }
            }
        }
    }

    fun setFilter(status: RequestStatus) =
        _uiState.update { it.copy(activeFilter = status) }

    fun filteredRequests(): List<ServiceRequest> {
        val s = _uiState.value
        return s.requests.filter { req ->
            when (s.activeFilter) {
                RequestStatus.PENDING -> req.status == RequestStatus.PENDING || req.status == RequestStatus.NEW
                else -> req.status == s.activeFilter
            }
        }
    }
}