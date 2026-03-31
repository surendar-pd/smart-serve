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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedButtonVariant
import com.smartserve.sharedui.SharedCard
import com.smartserve.sharedui.SharedProgress
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextArea
import com.smartserve.sharedui.SharedTextField
import com.smartserve.sharedui.SharedTextVariant
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ── Helpers ───────────────────────────────────────────────────────────────────

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

private fun formatTime(hour: Int, minute: Int): String {
    val period = if (hour < 12) "AM" else "PM"
    val h = when { hour == 0 -> 12; hour <= 12 -> hour; else -> hour - 12 }
    return "$h:${minute.toString().padStart(2, '0')} $period"
}

// ── OSM address picker composable ─────────────────────────────────────────────

@Composable
private fun OsmAddressPicker(
    pinLat: Double,
    pinLon: Double,
    onTap: (lat: Double, lon: Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Initialise OSMDroid user-agent (required by library)
    Configuration.getInstance().userAgentValue = "SmartServe/1.0"

    val onTapRef = rememberUpdatedState(onTap)

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isClickable = true
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(pinLat, pinLon))
        }
    }

    // Stable tap overlay — always delegates to the latest onTap lambda
    val tapOverlay = remember {
        MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                onTapRef.value(p.latitude, p.longitude)
                return true
            }
            override fun longPressHelper(p: GeoPoint) = false
        })
    }

    val marker = remember { Marker(mapView) }

    // Wire up overlays once
    LaunchedEffect(Unit) {
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(tapOverlay)
        mapView.overlays.add(marker)
    }

    // Update pin whenever the position changes
    LaunchedEffect(pinLat, pinLon) {
        val point = GeoPoint(pinLat, pinLon)
        marker.position = point
        mapView.controller.animateTo(point)
        mapView.invalidate()
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update  = { mv ->
            // Prevent the parent ScrollView from stealing map touch events
            mv.setOnTouchListener { v, _ ->
                v.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
        },
    )
}

// ── BookingScreen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    service: CustomerServiceListing,
    onBack: () -> Unit,
    onAddToCart: (CartItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingViewModel = hiltViewModel(),
) {
    val homeAddress  by viewModel.homeAddress.collectAsState()
    val pinLat       by viewModel.pinLat.collectAsState()
    val pinLon       by viewModel.pinLon.collectAsState()
    val geoResult    by viewModel.geoResult.collectAsState()
    val isGeocoding  by viewModel.isGeocoding.collectAsState()

    val providerName = service.providerName
    val serviceName  = service.title
    val priceLabel   = "$${service.hourlyRate.toInt()}/hr"
    val availDays    = service.availabilityDays
    val startHour    = service.availabilityStart.substringBefore(":").toIntOrNull() ?: 9
    val endHour      = service.availabilityEnd.substringBefore(":").toIntOrNull()   ?: 17
    val windowLabel  = "${formatTime(startHour, 0)} – ${formatTime(endHour, 0)}"

    var selectedDate    by remember { mutableStateOf("") }
    var selectedTime    by remember { mutableStateOf("") }
    var timeError       by remember { mutableStateOf("") }
    var addressQuery    by remember { mutableStateOf("") }
    var notes           by remember { mutableStateOf("") }
    var showDatePicker  by remember { mutableStateOf(false) }
    var showTimePicker  by remember { mutableStateOf(false) }

    // Pre-fill search field from saved profile address once it loads
    LaunchedEffect(homeAddress) {
        if (addressQuery.isBlank() && homeAddress.isNotBlank()) {
            addressQuery = homeAddress
        }
    }

    // Keep search field in sync when a geocode result comes back
    LaunchedEffect(geoResult) {
        geoResult?.let { addressQuery = it.shortLabel }
    }

    // Derived: confirmed address to write to CartItem
    val confirmedAddress = geoResult?.fullAddress ?: addressQuery
    val addressValid     = geoResult?.isInOttawa ?: false   // null = not yet resolved

    // ── Date picker ──────────────────────────────────────────────────────────
    if (showDatePicker) {
        val todayUtc = run {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        val datePickerState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    if (utcTimeMillis < todayUtc) return false
                    if (availDays.isEmpty()) return true
                    return utcTimeMillis.toDayOfWeekShort() in availDays
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

    // ── Time picker ───────────────────────────────────────────────────────────
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = startHour, initialMinute = 0, is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = timePickerState)
                    Text(
                        text  = "Available: $windowLabel",
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
                        timeError    = ""
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

    // ── Layout ────────────────────────────────────────────────────────────────
    Column(modifier = modifier.fillMaxSize()) {
        CustomerStackHeader(
            title    = "Book service",
            subtitle = "$serviceName with $providerName · $priceLabel",
            onBack   = onBack,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            SharedProgress(progress = 0.5f)

            // ── Date ──────────────────────────────────────────────────────────
            SharedText(text = "Select Date", variant = SharedTextVariant.Label)
            if (availDays.isNotEmpty()) {
                SharedText(
                    text    = "Available: ${availDays.joinToString(", ")}",
                    variant = SharedTextVariant.Caption,
                    color   = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SharedButton(
                text        = if (selectedDate.isEmpty()) "Choose a date" else selectedDate,
                onClick     = { showDatePicker = true },
                leadingIcon = Icons.Filled.DateRange,
                modifier    = Modifier.fillMaxWidth(),
                variant     = SharedButtonVariant.Outline,
            )

            // ── Time ──────────────────────────────────────────────────────────
            SharedText(text = "Select Time", variant = SharedTextVariant.Label)
            SharedText(
                text    = "Available: $windowLabel",
                variant = SharedTextVariant.Caption,
                color   = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SharedButton(
                text        = if (selectedTime.isEmpty()) "Choose a time" else selectedTime,
                onClick     = { showTimePicker = true; timeError = "" },
                leadingIcon = Icons.Filled.DateRange,
                modifier    = Modifier.fillMaxWidth(),
                variant     = SharedButtonVariant.Outline,
            )
            if (timeError.isNotBlank()) {
                SharedText(
                    text    = timeError,
                    variant = SharedTextVariant.Caption,
                    color   = MaterialTheme.colorScheme.error,
                )
            }

            // ── Address ───────────────────────────────────────────────────────
            SharedText(text = "Service Address", variant = SharedTextVariant.Label)
            SharedText(
                text    = "Tap the map to pin your location, or search below",
                variant = SharedTextVariant.Caption,
                color   = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // OSM map
            OsmAddressPicker(
                pinLat   = pinLat,
                pinLon   = pinLon,
                onTap    = { lat, lon -> viewModel.onMapTap(lat, lon) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )

            // Search field + button
            Row(
                modifier             = Modifier.fillMaxWidth(),
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SharedTextField(
                    value          = addressQuery,
                    onValueChange  = { addressQuery = it; viewModel.clearGeoResult() },
                    label          = "Search address",
                    modifier       = Modifier.weight(1f),
                )
                SharedButton(
                    text    = "Search",
                    onClick = { viewModel.searchAddress(addressQuery) },
                    enabled = !isGeocoding && addressQuery.isNotBlank(),
                )
            }

            // Validation status
            when {
                isGeocoding -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    SharedText(
                        text    = "Locating…",
                        variant = SharedTextVariant.Caption,
                        color   = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                geoResult != null && addressValid -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    SharedText(
                        text    = geoResult!!.shortLabel,
                        variant = SharedTextVariant.Caption,
                        color   = MaterialTheme.colorScheme.primary,
                    )
                }
                geoResult != null && !addressValid -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    SharedText(
                        text    = "Address must be in the Ottawa region",
                        variant = SharedTextVariant.Caption,
                        color   = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // ── Notes ─────────────────────────────────────────────────────────
            SharedTextArea(
                value         = notes,
                onValueChange = { notes = it },
                placeholder   = "Any special instructions…",
                modifier      = Modifier.fillMaxWidth(),
            )

            // ── Provider summary ──────────────────────────────────────────────
            SharedText(text = "Provider", variant = SharedTextVariant.Label)
            SharedCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SharedAvatar(name = providerName, size = 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        SharedText(text = providerName, variant = SharedTextVariant.BodyStrong)
                        SharedText(
                            text    = priceLabel,
                            variant = SharedTextVariant.Body,
                            color   = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Warn if address is outside Ottawa.
            if (geoResult != null && !addressValid) {
                SharedText(
                    text    = "Note: this address is outside the Ottawa service area",
                    variant = SharedTextVariant.Caption,
                    color   = MaterialTheme.colorScheme.error,
                )
            }

            val isReadyToAdd = selectedDate.isNotBlank() && selectedTime.isNotBlank() && confirmedAddress.isNotBlank() && addressValid

            if (!isReadyToAdd) {
                SharedText(
                    text    = "Select a date, time and a valid Ottawa address before adding to cart.",
                    variant = SharedTextVariant.Caption,
                    color   = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SharedButton(
                text    = "Add to Cart",
                onClick = {
                    onAddToCart(
                        CartItem(
                            providerUid  = service.providerUid,
                            serviceId    = service.serviceId,
                            providerName = providerName,
                            serviceName  = serviceName,
                            price        = priceLabel,
                            address      = confirmedAddress,
                            addressLat   = geoResult?.lat ?: pinLat,
                            addressLng   = geoResult?.lon ?: pinLon,
                            date         = selectedDate,
                            time         = selectedTime,
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled  = isReadyToAdd,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
