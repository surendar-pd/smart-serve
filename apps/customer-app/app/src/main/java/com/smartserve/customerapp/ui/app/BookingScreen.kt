package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedButtonVariant
import com.smartserve.sharedui.SharedCard
import com.smartserve.sharedui.SharedProgress
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextArea
import com.smartserve.sharedui.SharedTextField
import com.smartserve.sharedui.SharedTextVariant
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Convert UTC-midnight millis (from DatePicker) to a short day name: "Mon", "Tue", etc. */
private fun Long.toDayOfWeekShort(): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.timeInMillis = this
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY    -> "Mon"
        Calendar.TUESDAY   -> "Tue"
        Calendar.WEDNESDAY -> "Wed"
        Calendar.THURSDAY  -> "Thu"
        Calendar.FRIDAY    -> "Fri"
        Calendar.SATURDAY  -> "Sat"
        Calendar.SUNDAY    -> "Sun"
        else               -> ""
    }
}

/** Format an hour/minute pair to "9:30 AM" style. */
private fun formatTime(hour: Int, minute: Int): String {
    val period = if (hour < 12) "AM" else "PM"
    val h = when {
        hour == 0  -> 12
        hour <= 12 -> hour
        else       -> hour - 12
    }
    val m = minute.toString().padStart(2, '0')
    return "$h:$m $period"
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    service: CustomerServiceListing,
    onBack: () -> Unit,
    onAddToCart: (CartItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val providerName = service.providerName
    val serviceName  = service.title
    val priceLabel   = "$${service.hourlyRate.toInt()}/hr"

    val availDays  = service.availabilityDays   // e.g. ["Mon", "Wed", "Fri"]
    val startHour  = service.availabilityStart.substringBefore(":").toIntOrNull() ?: 9
    val endHour    = service.availabilityEnd.substringBefore(":").toIntOrNull()   ?: 17
    val windowLabel = "${formatTime(startHour, 0)} – ${formatTime(endHour, 0)}"

    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var timeError    by remember { mutableStateOf("") }
    var address      by remember { mutableStateOf("123 Main St, Ottawa") }
    var notes        by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // ── Date picker ──────────────────────────────────────────────────────────
    if (showDatePicker) {
        val todayUtc = run {
            // UTC midnight of today so we never block today itself
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        val datePickerState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    if (utcTimeMillis < todayUtc) return false           // no past dates
                    if (availDays.isEmpty()) return true                  // no restriction
                    return utcTimeMillis.toDayOfWeekShort() in availDays  // only provider days
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
                            .format(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ── Time picker dialog ────────────────────────────────────────────────────
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour   = startHour,
            initialMinute = 0,
            is24Hour      = false,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Text("Select Time")
            },
            text = {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    TimePicker(state = timePickerState)
                    Text(
                        text = "Available: $windowLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val h = timePickerState.hour
                    if (h in startHour until endHour) {
                        selectedTime = formatTime(h, timePickerState.minute)
                        timeError = ""
                        showTimePicker = false
                    } else {
                        timeError = "Please choose a time between $windowLabel"
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
        )
    }

    // ── Layout ───────────────────────────────────────────────────────────────
    Column(modifier = modifier.fillMaxSize()) {
        CustomerStackHeader(
            title = "Book service",
            subtitle = "$serviceName with $providerName · $priceLabel",
            onBack = onBack,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            SharedProgress(progress = 0.5f)

            // ── Date ────────────────────────────────────────────────────────
            SharedText(text = "Select Date", variant = SharedTextVariant.Label)
            if (availDays.isNotEmpty()) {
                SharedText(
                    text = "Available: ${availDays.joinToString(", ")}",
                    variant = SharedTextVariant.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SharedButton(
                text = if (selectedDate.isEmpty()) "Choose a date" else selectedDate,
                onClick = { showDatePicker = true },
                leadingIcon = Icons.Filled.DateRange,
                modifier = Modifier.fillMaxWidth(),
                variant = SharedButtonVariant.Outline,
            )

            // ── Time ────────────────────────────────────────────────────────
            SharedText(text = "Select Time", variant = SharedTextVariant.Label)
            SharedText(
                text = "Available: $windowLabel",
                variant = SharedTextVariant.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SharedButton(
                text = if (selectedTime.isEmpty()) "Choose a time" else selectedTime,
                onClick = { showTimePicker = true; timeError = "" },
                leadingIcon = Icons.Filled.DateRange,
                modifier = Modifier.fillMaxWidth(),
                variant = SharedButtonVariant.Outline,
            )
            if (timeError.isNotBlank()) {
                SharedText(
                    text = timeError,
                    variant = SharedTextVariant.Caption,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // ── Address ─────────────────────────────────────────────────────
            SharedTextField(
                value = address,
                onValueChange = { address = it },
                label = "Address",
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Notes ───────────────────────────────────────────────────────
            SharedTextArea(
                value = notes,
                onValueChange = { notes = it },
                placeholder = "Any special instructions...",
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Provider summary ─────────────────────────────────────────────
            SharedText(text = "Provider", variant = SharedTextVariant.Label)
            SharedCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SharedAvatar(name = providerName, size = 40.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        SharedText(text = providerName, variant = SharedTextVariant.BodyStrong)
                        SharedText(
                            text = priceLabel,
                            variant = SharedTextVariant.Body,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            SharedButton(
                text = "Add to Cart",
                onClick = {
                    onAddToCart(
                        CartItem(
                            providerName = providerName,
                            serviceName  = serviceName,
                            price        = priceLabel,
                            date         = selectedDate,
                            time         = selectedTime,
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
