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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
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

private fun formatSelectedDateFromUtcMillis(utcMillis: Long): String {
    val formatter = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return formatter.format(Date(utcMillis))
}

private fun parseUiDateToMillis(dateStr: String): Long? {
    if (dateStr.isBlank()) return null
    return runCatching {
        SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).parse(dateStr)?.time
    }.getOrNull()
}

private fun currentUtcDayStartMillis(): Long {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun parseAvailabilityTimeToMinutes(raw: String, fallbackHour24: Int): Int {
    val text = raw.trim()
    if (text.isBlank()) return fallbackHour24 * 60

    val patterns = listOf("h:mm a", "hh:mm a", "H:mm", "HH:mm")
    for (pattern in patterns) {
        val parsed = runCatching {
            val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
            sdf.parse(text)
        }.getOrNull() ?: continue

        val cal = Calendar.getInstance().apply { time = parsed }
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    return fallbackHour24 * 60
}

private data class HourlySlot(
    val startMinutes: Int,
    val endMinutes: Int,
    val startLabel: String,
    val rangeLabel: String,
)

private fun buildHourlySlots(startMinutes: Int, endMinutes: Int): List<HourlySlot> {
    if (endMinutes <= startMinutes) return emptyList()
    val slots = mutableListOf<HourlySlot>()
    var cursor = startMinutes
    while (cursor + 60 <= endMinutes) {
        val startHour = cursor / 60
        val startMinute = cursor % 60
        val end = cursor + 60
        val endHour = end / 60
        val endMinute = end % 60
        val startLabel = formatTime(startHour, startMinute)
        slots.add(
            HourlySlot(
                startMinutes = cursor,
                endMinutes = end,
                startLabel = startLabel,
                rangeLabel = "$startLabel - ${formatTime(endHour, endMinute)}",
            )
        )
        cursor += 60
    }
    return slots
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
    cartItems: List<CartItem> = emptyList(),
    categoryId: String = "",
    prefillItem: CartItem? = null,
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
    val startMinutes = parseAvailabilityTimeToMinutes(service.availabilityStart, fallbackHour24 = 9)
    val endMinutes   = parseAvailabilityTimeToMinutes(service.availabilityEnd, fallbackHour24 = 17)
    val startHour    = startMinutes / 60
    val startMinute  = startMinutes % 60
    val endHour      = endMinutes / 60
    val endMinute    = endMinutes % 60
    val hasValidWindow = endMinutes > startMinutes
    val windowLabel  = "${formatTime(startHour, startMinute)} – ${formatTime(endHour, endMinute)}"

    var selectedDate    by remember(prefillItem?.lineDocumentId) { mutableStateOf(prefillItem?.date.orEmpty()) }
    var selectedDateUtcMillis by remember(prefillItem?.lineDocumentId) {
        mutableStateOf(parseUiDateToMillis(prefillItem?.date.orEmpty()))
    }
    var selectedTimeStart by remember(prefillItem?.lineDocumentId) { mutableStateOf(prefillItem?.time.orEmpty()) }
    var selectedTimeRange by remember(prefillItem?.lineDocumentId) { mutableStateOf(prefillItem?.timeRange.orEmpty()) }
    var timeError       by remember { mutableStateOf("") }
    var addressQuery    by remember(prefillItem?.lineDocumentId) { mutableStateOf(prefillItem?.address.orEmpty()) }
    var notes           by remember(prefillItem?.lineDocumentId) { mutableStateOf(prefillItem?.specialInstructions.orEmpty()) }
    var showDatePicker  by remember { mutableStateOf(false) }
    var showTimePicker  by remember { mutableStateOf(false) }

    val alreadyInCart = remember(cartItems, service.serviceId, service.providerUid) {
        cartItems.any { it.serviceId == service.serviceId && it.providerUid == service.providerUid }
    }
    val editingLineId = prefillItem?.lineDocumentId
    val duplicateInCart = remember(cartItems, service.serviceId, service.providerUid, editingLineId) {
        cartItems.any { existing ->
            existing.providerUid == service.providerUid &&
                existing.serviceId == service.serviceId &&
                existing.lineDocumentId != editingLineId
        }
    }

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
    val hourlySlots = remember(startMinutes, endMinutes) { buildHourlySlots(startMinutes, endMinutes) }
    val bookedStarts by remember(selectedDate, service.providerUid) {
        viewModel.observeBookedSlotStarts(service.providerUid, selectedDate)
    }.collectAsState(initial = emptySet())
    val todayUtcStart = currentUtcDayStartMillis()
    val selectedIsToday = selectedDateUtcMillis == todayUtcStart
    val currentLocalMinutes = Calendar.getInstance().let {
        it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
    }
    val selectedStartMinutes = if (selectedTimeStart.isBlank()) {
        -1
    } else {
        parseAvailabilityTimeToMinutes(selectedTimeStart, fallbackHour24 = 0)
    }
    val selectedTimeIsPast =
        selectedIsToday && selectedStartMinutes >= 0 && selectedStartMinutes < currentLocalMinutes

    // ── Date picker ──────────────────────────────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    if (utcTimeMillis < todayUtcStart) return false
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
                        selectedDate = formatSelectedDateFromUtcMillis(millis)
                        selectedDateUtcMillis = millis
                        selectedTimeStart = ""
                        selectedTimeRange = ""
                        timeError = ""
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
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time Slot") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text  = "Available: $windowLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (selectedDate.isBlank()) {
                        SharedText(
                            text = "Choose a date first.",
                            variant = SharedTextVariant.Caption,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else if (hourlySlots.isEmpty()) {
                        SharedText(
                            text = "No hourly slots available for this service window.",
                            variant = SharedTextVariant.Caption,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        hourlySlots.chunked(2).forEach { rowSlots ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowSlots.forEach { slot ->
                                    val booked = slot.startLabel.trim().lowercase() in bookedStarts
                                    val past = selectedIsToday && slot.startMinutes < currentLocalMinutes
                                    val unavailable = booked || past
                                    val selected = selectedTimeStart == slot.startLabel
                                    SharedButton(
                                        text = when {
                                            booked -> "${slot.rangeLabel} (Booked)"
                                            past -> "${slot.rangeLabel} (Past)"
                                            else -> slot.rangeLabel
                                        },
                                        onClick = {
                                            selectedTimeStart = slot.startLabel
                                            selectedTimeRange = slot.rangeLabel
                                            timeError = ""
                                            showTimePicker = false
                                        },
                                        enabled = !unavailable,
                                        modifier = Modifier.weight(1f),
                                        variant = if (selected) SharedButtonVariant.Secondary else SharedButtonVariant.Outline,
                                    )
                                }
                                if (rowSlots.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
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

            if (service.photoUrls.isNotEmpty()) {
                SharedText(text = "Photos", variant = SharedTextVariant.Label)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(service.photoUrls, key = { it }) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Service photo",
                            modifier = Modifier
                                .width(200.dp)
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }

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
                text        = if (selectedTimeRange.isEmpty()) "Choose a time" else selectedTimeRange,
                onClick     = {
                    if (selectedDate.isBlank()) {
                        timeError = "Please choose a date first"
                    } else {
                        showTimePicker = true
                        timeError = ""
                    }
                },
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
            } else if (selectedTimeIsPast) {
                SharedText(
                    text    = "Selected time is in the past. Please choose a future slot.",
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

            val canAddToCart = selectedDate.isNotBlank() &&
                selectedTimeStart.isNotBlank() &&
                !selectedTimeIsPast &&
                        !duplicateInCart

            SharedButton(
                text    = if (prefillItem != null) "Save Changes" else if (duplicateInCart) "Added to cart" else "Add to Cart",
                onClick = {
                    onAddToCart(
                        CartItem(
                            lineDocumentId = prefillItem?.lineDocumentId,
                            providerUid  = service.providerUid,
                            serviceId    = service.serviceId,
                            categoryId   = categoryId,
                            providerName = providerName,
                            serviceName  = serviceName,
                            price        = priceLabel,
                            hourlyRate   = service.hourlyRate,
                            lat          = pinLat,
                            lon          = pinLon,
                            address      = confirmedAddress,
                            date         = selectedDate,
                            time         = selectedTimeStart,
                            timeRange    = selectedTimeRange,
                            specialInstructions = notes.trim(),
                        )
                    )
                },
                enabled = canAddToCart,
                modifier = Modifier.fillMaxWidth(),
            )
            if (duplicateInCart) {
                SharedText(
                    text = "This service is already in your cart",
                    variant = SharedTextVariant.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
