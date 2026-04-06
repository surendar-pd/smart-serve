package com.smartserve.providerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.smartserve.sharedauth.AuthCollections
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val totalBookings: Int = 0,
    val totalServices: Int = 0,
    val totalReviews: Int = 0,
    val totalEarningsCad: Double = 0.0,
    val monthlyRevenue: List<MonthlyRevenuePoint> = emptyList(),
    val errorMessage: String? = null,
)

data class MonthlyRevenuePoint(
    val month: String,
    val amount: Double,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: BookingRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var incomingRequests: List<ServiceRequest> = emptyList()
    private var pastRequests: List<ServiceRequest> = emptyList()
    private var cachedTotalServices: Int = 0
    private var cachedProfileTotalReviews: Int? = null

    init {
        loadOnlineStatus()
        observeRequests()
        loadServiceStats()
        loadReviewStats()
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

    private fun observeRequests() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.getIncomingRequests(uid)
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.localizedMessage)
                    }
                }
                .collect { list ->
                    incomingRequests = list
                    updateDashboard()
                }
        }

        viewModelScope.launch {
            repository.getPastBookings(uid)
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.localizedMessage)
                    }
                }
                .collect { list ->
                    pastRequests = list
                    updateDashboard()
                }
        }
    }

    private fun loadServiceStats() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            runCatching {
                val providerRef = firestore.collection(AuthCollections.PROVIDER_PROFILES).document(uid)
                firestore.collection(AuthCollections.SERVICES)
                    .whereEqualTo("provider", providerRef)
                    .get()
                    .await()
            }.onSuccess { snapshot ->
                cachedTotalServices = snapshot.size()
                updateDashboard()
            }
        }
    }

    private fun loadReviewStats() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            runCatching {
                firestore.collection(AuthCollections.PROVIDER_PROFILES)
                    .document(uid)
                    .get()
                    .await()
            }.onSuccess { doc ->
                cachedProfileTotalReviews = doc.getLong("totalReviews")?.toInt()
                updateDashboard()
            }
        }
    }

    private fun updateDashboard() {
        val allUniqueBookings = (incomingRequests + pastRequests)
            .distinctBy { it.id }

        val completed = allUniqueBookings
            .filter { it.status == RequestStatus.COMPLETED }

        val dynamicMonthlyRevenue = buildMonthlyRevenue(completed)
        val reviewsFromBookings = allUniqueBookings.count { (it.providerRating ?: 0f) > 0f }
        val totalReviews = cachedProfileTotalReviews ?: reviewsFromBookings

        _uiState.update {
            it.copy(
                isLoading = false,
                totalBookings = allUniqueBookings.size,
                totalServices = cachedTotalServices,
                totalReviews = totalReviews,
                totalEarningsCad = completed.sumOf { req -> req.earnings.toDouble() },
                monthlyRevenue = dynamicMonthlyRevenue,
            )
        }

        val uid = auth.currentUser?.uid
        if (uid != null && cachedProfileTotalReviews != reviewsFromBookings) {
            cachedProfileTotalReviews = reviewsFromBookings
            viewModelScope.launch {
                runCatching {
                    firestore.collection(AuthCollections.PROVIDER_PROFILES)
                        .document(uid)
                        .set(mapOf("totalReviews" to reviewsFromBookings), SetOptions.merge())
                        .await()
                }
            }
        }
    }

    private fun buildMonthlyRevenue(completed: List<ServiceRequest>): List<MonthlyRevenuePoint> {
        val formatter = SimpleDateFormat("MMM", Locale.getDefault())
        val monthBuckets = mutableListOf<Calendar>()
        val monthKeys = mutableListOf<String>()

        val cursor = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        repeat(6) {
            val month = (cursor.clone() as Calendar)
            monthBuckets.add(month)
            monthKeys.add("${month.get(Calendar.YEAR)}-${month.get(Calendar.MONTH)}")
            cursor.add(Calendar.MONTH, -1)
        }

        monthBuckets.reverse()
        monthKeys.reverse()

        val earningsByMonth = monthKeys.associateWith { 0.0 }.toMutableMap()
        completed.forEach { request ->
            val date = request.completedAt?.toDate() ?: request.scheduledAt?.toDate() ?: return@forEach
            val cal = Calendar.getInstance().apply { time = date }
            val key = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}"
            if (earningsByMonth.containsKey(key)) {
                earningsByMonth[key] = (earningsByMonth[key] ?: 0.0) + request.earnings.toDouble()
            }
        }

        return monthBuckets.mapIndexed { index, month ->
            MonthlyRevenuePoint(
                month = formatter.format(month.time),
                amount = earningsByMonth[monthKeys[index]] ?: 0.0,
            )
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

}