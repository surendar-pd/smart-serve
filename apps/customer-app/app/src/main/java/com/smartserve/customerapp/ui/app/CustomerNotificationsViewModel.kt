package com.smartserve.customerapp.ui.app

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CustomerNotificationsViewModel @Inject constructor(
    private val repository: CustomerServicesRepository,
    @ApplicationContext context: Context,
) : ViewModel() {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("customer_notifications", Context.MODE_PRIVATE)

    init {
        CustomerNotificationCenter.ensureChannel(appContext)
        observeBookingsForNotifications()
    }

    private fun observeBookingsForNotifications() {
        viewModelScope.launch {
            repository.observeMyBookings()
                .catch { /* keep UI resilient; no-op for notifications */ }
                .collect { bookings ->
                    bookings.forEach { booking ->
                        processStatusTransition(booking)
                        processServiceDayReminder(booking)
                    }
                }
        }
    }

    private fun processStatusTransition(booking: CustomerBooking) {
        val key = "status_${booking.id}"
        val previous = prefs.getString(key, null)
        val current = booking.status.lowercase().trim()

        if (previous == null) {
            prefs.edit().putString(key, current).apply()
            return
        }

        if (previous == current) return

        when (current) {
            "active" -> CustomerNotificationCenter.notify(
                context = appContext,
                notificationId = booking.id.hashCode(),
                title = "Provider Accepted",
                body = "${booking.providerName} accepted your ${booking.serviceName} request.",
            )

            "declined" -> CustomerNotificationCenter.notify(
                context = appContext,
                notificationId = booking.id.hashCode(),
                title = "Request Declined",
                body = "${booking.providerName} declined your ${booking.serviceName} request.",
            )
        }

        prefs.edit().putString(key, current).apply()
    }

    private fun processServiceDayReminder(booking: CustomerBooking) {
        val status = booking.status.lowercase().trim()
        if (status != "active") return
        if (booking.scheduledAtMillis <= 0L) return

        val scheduled = Calendar.getInstance().apply { timeInMillis = booking.scheduledAtMillis }
        val today = Calendar.getInstance()
        if (!isSameDate(scheduled, today)) return

        val dayToken = SimpleDateFormat("yyyyMMdd", Locale.US).format(today.time)
        val reminderKey = "day_reminder_${booking.id}_$dayToken"
        if (prefs.getBoolean(reminderKey, false)) return

        CustomerNotificationCenter.notify(
            context = appContext,
            notificationId = booking.id.hashCode() + 73,
            title = "Service Day Reminder",
            body = "Today: ${booking.serviceName} with ${booking.providerName} at ${booking.time}.",
        )

        prefs.edit().putBoolean(reminderKey, true).apply()
    }

    private fun isSameDate(first: Calendar, second: Calendar): Boolean {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
            first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
    }
}
