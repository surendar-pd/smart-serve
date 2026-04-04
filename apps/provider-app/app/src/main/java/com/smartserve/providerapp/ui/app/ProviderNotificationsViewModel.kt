package com.smartserve.providerapp.ui.app

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.smartserve.sharedauth.AuthCollections
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ProviderNotificationsViewModel @Inject constructor(
    private val repository: BookingRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext context: Context,
) : ViewModel() {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("provider_notifications", Context.MODE_PRIVATE)
    private var settingsRegistration: ListenerRegistration? = null
    private val pushNotificationsEnabled = MutableStateFlow(true)
    private val requestNotificationsEnabled = MutableStateFlow(true)
    private val serviceReminderNotificationsEnabled = MutableStateFlow(true)

    init {
        ProviderNotificationCenter.ensureChannel(appContext)
        observeNotificationSettings()
        observeRequestsForNotifications()
    }

    private fun observeNotificationSettings() {
        val uid = auth.currentUser?.uid ?: return
        settingsRegistration = firestore.collection(AuthCollections.PROVIDER_PROFILES)
            .document(uid)
            .addSnapshotListener { snap, _ ->
                pushNotificationsEnabled.value = snap?.getBoolean("pushNotifications") ?: true
                requestNotificationsEnabled.value = snap?.getBoolean("providerRequestNotifications") ?: true
                serviceReminderNotificationsEnabled.value = snap?.getBoolean("providerServiceReminderNotifications") ?: true
            }
    }

    private fun observeRequestsForNotifications() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            combine(
                repository.getIncomingRequests(uid),
                repository.getPastBookings(uid),
            ) { incoming, past ->
                (incoming + past).associateBy { it.id }.values.toList()
            }
                .catch { /* keep UI resilient; no-op for notifications */ }
                .collect { requests ->
                    requests.forEach { request ->
                        processNewRequest(request)
                        processServiceDayReminder(request)
                    }
                }
        }
    }

    private fun processNewRequest(request: ServiceRequest) {
        if (!pushNotificationsEnabled.value || !requestNotificationsEnabled.value) return

        val key = "status_${request.id}"
        val previous = prefs.getString(key, null)
        val current = request.status.value

        if (previous == null && (current == RequestStatus.NEW.value || current == RequestStatus.PENDING.value)) {
            val service = request.serviceType.ifBlank { "service" }
            val customer = request.customerFirstName.ifBlank { "A customer" }
            ProviderNotificationCenter.notify(
                context = appContext,
                notificationId = request.id.hashCode(),
                title = "New Service Request",
                body = "$customer requested $service.",
            )
        }

        if (previous != current) {
            prefs.edit().putString(key, current).apply()
        }
    }

    private fun processServiceDayReminder(request: ServiceRequest) {
        if (!pushNotificationsEnabled.value || !serviceReminderNotificationsEnabled.value) return

        val status = request.status
        if (status != RequestStatus.ACTIVE) return

        val dateText = request.date.trim()
        if (dateText.isBlank()) return

        val scheduled = runCatching {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).parse(dateText)
        }.getOrNull() ?: return

        val scheduledCal = Calendar.getInstance().apply { time = scheduled }
        val today = Calendar.getInstance()
        val isSameDate =
            scheduledCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                scheduledCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        if (!isSameDate) return

        val dayToken = SimpleDateFormat("yyyyMMdd", Locale.US).format(today.time)
        val reminderKey = "day_reminder_${request.id}_$dayToken"
        if (prefs.getBoolean(reminderKey, false)) return

        val service = request.serviceType.ifBlank { "service" }
        ProviderNotificationCenter.notify(
            context = appContext,
            notificationId = request.id.hashCode() + 91,
            title = "Today's Service Reminder",
            body = "Today: $service for ${request.customerFirstName} at ${request.time}.",
        )

        prefs.edit().putBoolean(reminderKey, true).apply()
    }

    override fun onCleared() {
        settingsRegistration?.remove()
        super.onCleared()
    }
}
