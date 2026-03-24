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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.smartserve.sharedui.SharedChip
import com.smartserve.sharedui.SharedProgress
import com.smartserve.sharedui.SharedScaffold
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextArea
import com.smartserve.sharedui.SharedTextField
import com.smartserve.sharedui.SharedTextVariant
import com.smartserve.sharedui.SharedTopAppBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    providerName: String,
    serviceName: String,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("123 Main St, Ottawa") }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
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

    SharedScaffold(
        modifier = modifier,
        topBar = { SharedTopAppBar(title = "Book Service", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            SharedProgress(progress = 0.5f)

            SharedText(text = serviceName, variant = SharedTextVariant.Title)
            SharedText(text = "with $providerName", variant = SharedTextVariant.Body)

            SharedText(text = "Select Date", variant = SharedTextVariant.Label)
            SharedButton(
                text = if (selectedDate.isEmpty()) "Choose a date" else selectedDate,
                onClick = { showDatePicker = true },
                leadingIcon = Icons.Filled.DateRange,
                modifier = Modifier.fillMaxWidth(),
                variant = SharedButtonVariant.Outline,
            )

            SharedText(text = "Select Time", variant = SharedTextVariant.Label)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("9 AM", "12 PM", "3 PM").forEach { time ->
                    SharedChip(
                        label = time,
                        selected = selectedTime == time,
                        onSelectedChange = { checked ->
                            selectedTime = if (checked) time else ""
                        },
                    )
                }
            }

            SharedTextField(
                value = address,
                onValueChange = { address = it },
                label = "Address",
            )

            SharedTextArea(
                value = notes,
                onValueChange = { notes = it },
                placeholder = "Any special instructions...",
            )

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
                        SharedText(text = "⭐ 4.8 · Ottawa, ON", variant = SharedTextVariant.Body)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            SharedButton(
                text = "Confirm Booking",
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
