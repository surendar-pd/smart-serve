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
import java.util.Calendar
import javax.inject.Inject

data class BookingsUiState(
    val isLoading: Boolean = true,
    val upcomingBookings: List<ServiceRequest> = emptyList(),  // new/pending/active
    val pastBookings: List<ServiceRequest> = emptyList(),      // completed/declined
    val weekEarnings: Int = 0,
    val monthEarnings: Int = 0,
    val errorMessage: String? = null,
)

@HiltViewModel
class BookingsViewModel @Inject constructor(
    private val repository: BookingRepository,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingsUiState())
    val uiState: StateFlow<BookingsUiState> = _uiState.asStateFlow()

    init {
        loadBookings()
    }

    private fun loadBookings() {
        val uid = auth.currentUser?.uid ?: run {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        // ── Load upcoming: new, pending, active ───────────────────────────────
        viewModelScope.launch {
            repository.getIncomingRequests(uid)
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.localizedMessage) }
                }
                .collect { list ->
                    _uiState.update { it.copy(
                        isLoading        = false,
                        upcomingBookings = list,
                    )}
                }
        }

        // ── Load past: completed, declined ────────────────────────────────────
        viewModelScope.launch {
            repository.getPastBookings(uid)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
                }
                .collect { list ->
                    _uiState.update { it.copy(
                        isLoading     = false,
                        pastBookings  = list,
                        weekEarnings  = computeWeekEarnings(list),
                        monthEarnings = computeMonthEarnings(list),
                    )}
                }
        }
    }

    private fun computeWeekEarnings(list: List<ServiceRequest>): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val weekStart = cal.timeInMillis
        return list
            .filter {
                it.status == RequestStatus.COMPLETED &&
                (it.completedAt?.toDate()?.time ?: 0L) >= weekStart
            }
            .sumOf { it.earnings }
    }

    private fun computeMonthEarnings(list: List<ServiceRequest>): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthStart = cal.timeInMillis
        return list
            .filter {
                it.status == RequestStatus.COMPLETED &&
                (it.completedAt?.toDate()?.time ?: 0L) >= monthStart
            }
            .sumOf { it.earnings }
    }
}