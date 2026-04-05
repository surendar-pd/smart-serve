package com.smartserve.providerapp.ui.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedButtonVariant
import com.smartserve.sharedui.SharedBottomSheet
import com.smartserve.sharedui.SharedLoading
import com.smartserve.sharedui.SharedScaffold
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextArea
import com.smartserve.sharedui.SharedTextVariant
import com.smartserve.sharedui.SharedTopAppBar
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ROUTE_WINDOW_MS = 3L * 60L * 60L * 1000L

private fun routeWindowLabel(scheduledAt: Date?): String {
    if (scheduledAt == null) return ""
    val formatter = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
    val unlockAt = Date(scheduledAt.time - ROUTE_WINDOW_MS)
    return "Directions unlock at ${formatter.format(unlockAt)} (3 hours before meeting)."
}

private fun isRouteEnabledForMeeting(scheduledAt: Date?): Boolean {
    if (scheduledAt == null) return true
    val now = System.currentTimeMillis()
    val start = scheduledAt.time - ROUTE_WINDOW_MS
    return now >= start
}

private fun resolveMeetingDate(req: ServiceRequest): Date? {
    val parsedFromDateAndTime = runCatching {
        val dateText = req.date.trim()
        if (dateText.isBlank()) return@runCatching null

        val timeToken = req.time
            .substringBefore("-")
            .trim()
            .ifBlank { return@runCatching null }

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).apply {
            isLenient = false
        }
        val dateOnly = dateFormat.parse(dateText) ?: return@runCatching null

        val timeFormat = SimpleDateFormat("h:mm a", Locale.US).apply {
            isLenient = false
        }
        val parsedTime = timeFormat.parse(timeToken) ?: return@runCatching null

        val dateCal = java.util.Calendar.getInstance().apply { time = dateOnly }
        val timeCal = java.util.Calendar.getInstance().apply { time = parsedTime }
        dateCal.set(java.util.Calendar.HOUR_OF_DAY, timeCal.get(java.util.Calendar.HOUR_OF_DAY))
        dateCal.set(java.util.Calendar.MINUTE, timeCal.get(java.util.Calendar.MINUTE))
        dateCal.set(java.util.Calendar.SECOND, 0)
        dateCal.set(java.util.Calendar.MILLISECOND, 0)
        dateCal.time
    }.getOrNull()

    return parsedFromDateAndTime ?: req.scheduledAt?.toDate()
}

private fun streetAddressOnly(address: String): String {
    val value = address.trim()
    if (value.isBlank()) return ""
    return value.substringBefore(",").trim().ifBlank { value }
}

@Composable
fun ActiveJobScreen(
    bookingId: String,
    modifier: Modifier = Modifier,
    onNavigateToBookings: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToMap: (lat: Double, lng: Double, address: String) -> Unit,  
) {
    // ── Build VM via assisted inject, scoped to this bookingId ───────────────
    val holder: ActiveJobAssistedFactoryHolder = hiltViewModel()
    val viewModel: ActiveJobViewModel = viewModel(
        key     = "ActiveJob_$bookingId",
        factory = provideActiveJobViewModel(holder.factory, bookingId),
    )
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val context = LocalContext.current

    // ── OSMDroid config — must happen before MapView is created ───────────────
    DisposableEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue   = context.packageName
            osmdroidBasePath = context.cacheDir
            osmdroidTileCache = File(context.cacheDir, "osmdroid")
        }
        onDispose { }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ActiveJobEvent.ShowSnackbar    -> snackbarHostState.showSnackbar(event.message)
                ActiveJobEvent.NavigateToBookings -> onNavigateToBookings()
            }
        }
    }

    SharedScaffold(
        modifier     = modifier,
        topBar       = {
            ProviderStackHeader(
                title = "Active job",
                subtitle = "Job in progress",
                onBack = onNavigateToBookings,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            state.isLoading -> Box(
                modifier         = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { SharedLoading() }

            state.request == null -> Box(
                modifier         = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { SharedText(text = "Job not found", variant = SharedTextVariant.Body) }

            else -> {
                val req = state.request!!
                val fullAddress = req.homeAddress
                    .ifBlank { req.neighborhood }
                    .ifBlank { req.customerFirstName }
                val streetAddress = streetAddressOnly(fullAddress)
                val isComplete = req.status == RequestStatus.COMPLETED
                val meetingDate = resolveMeetingDate(req)
                val routeEnabled = isRouteEnabledForMeeting(meetingDate)
                val routeHintText = routeWindowLabel(meetingDate)
                var showCompleteSheet by remember { mutableStateOf(false) }

                SharedBottomSheet(
                    isOpen = showCompleteSheet,
                    onOpenChange = { showCompleteSheet = it },
                    skipPartiallyExpanded = true,
                    sheetContent = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SharedText(text = "Complete service", variant = SharedTextVariant.Subtitle)
                            SharedText(
                                text = "Rate the customer, then complete the job.",
                                variant = SharedTextVariant.Caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            SharedText(text = "Rating", variant = SharedTextVariant.Label)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                (1..5).forEach { star ->
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "Star $star",
                                        tint = if (star <= state.currentRating)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clickable(enabled = !state.isMarkingDone) {
                                                viewModel.setRating(star.toFloat())
                                            },
                                    )
                                }
                            }

                            SharedButton(
                                text = if (state.isMarkingDone) "Completing…" else "Complete service",
                                onClick = { viewModel.completeService() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state.currentRating > 0f && !state.isMarkingDone,
                                loading = state.isMarkingDone,
                            )

                            SharedButton(
                                text = "Cancel",
                                onClick = { showCompleteSheet = false },
                                modifier = Modifier.fillMaxWidth(),
                                variant = SharedButtonVariant.Ghost,
                                enabled = !state.isMarkingDone,
                            )
                        }
                    },
                    content = { _ ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                        ) {
                    // ── Status banner ─────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(12.dp),
                    ) {
                        SharedText(
                            text = "● Status: ${req.status.name.lowercase().replaceFirstChar(Char::uppercase)}",
                            variant = SharedTextVariant.BodyStrong,
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Customer card ─────────────────────────────────────────
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.fillMaxWidth(),
                    ) {
                        SharedAvatar(name = req.customerFirstName, size = 48.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            SharedText(text = req.customerFirstName, variant = SharedTextVariant.BodyStrong)
                            SharedText(text = req.serviceType, variant = SharedTextVariant.Body)
                            SharedText(
                                text    = "${req.date} · ${req.time}",
                                variant = SharedTextVariant.Caption,
                            )
                            if (!isComplete) {
                                SharedText(
                                    text    = "Street: ${streetAddress.ifBlank { "N/A" }}",
                                    variant = SharedTextVariant.Caption,
                                )
                            }
                        }
                    }

                    if (!isComplete && req.specialInstructions.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        SharedText(text = "Special Instructions", variant = SharedTextVariant.Subtitle)
                        Spacer(Modifier.height(4.dp))
                        SharedTextArea(
                            value = req.specialInstructions,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    if (!isComplete) {
                        // ── Route preview map ─────────────────────────────────────
                        val customerGeoPoint = remember(req.customerLat, req.customerLng) {
                            if (req.customerLat != 0.0 && req.customerLng != 0.0) {
                                GeoPoint(req.customerLat, req.customerLng)
                            } else {
                                GeoPoint(45.4215, -75.6972) // Ottawa fallback
                            }
                        }

                        val mapView = remember {
                            MapView(context).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(false) // Disable zoom/pan for preview
                                setUseDataConnection(true)
                                controller.setZoom(15.0)
                                controller.setCenter(customerGeoPoint)
                            }
                        }

                        // ── MapView lifecycle — required for tiles to load ────────────────────────
                        val lifecycleOwner = LocalLifecycleOwner.current
                        DisposableEffect(lifecycleOwner) {
                            val observer = LifecycleEventObserver { _, event ->
                                when (event) {
                                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                                    Lifecycle.Event.ON_PAUSE  -> mapView.onPause()
                                    else -> {}
                                }
                            }
                            lifecycleOwner.lifecycle.addObserver(observer)
                            onDispose {
                                lifecycleOwner.lifecycle.removeObserver(observer)
                                mapView.onDetach()
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        ) {
                            AndroidView(
                                factory = { mapView },
                                modifier = Modifier.fillMaxSize(),
                                update = { map ->
                                    map.overlays.clear()

                                    // Customer marker
                                    map.overlays.add(Marker(map).apply {
                                        position = customerGeoPoint
                                        title = streetAddress.ifBlank { "Customer Location" }
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    })

                                    map.invalidate()
                                },
                            )

                            // Transparent overlay to capture taps above the MapView
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable {
                                        if (!routeEnabled) {
                                            snackbarScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    routeHintText.ifBlank {
                                                        "Directions are available only within 3 hours before meeting time"
                                                    }
                                                )
                                            }
                                            return@clickable
                                        }

                                        val destinationLabel = streetAddress.ifBlank { "Customer" }
                                        if (req.customerLat != 0.0 && req.customerLng != 0.0) {
                                            onNavigateToMap(
                                                req.customerLat,
                                                req.customerLng,
                                                destinationLabel,
                                            )
                                        } else if (streetAddress.isNotBlank()) {
                                            // Use in-app OSM map screen; ViewModel handles address geocoding when coordinates are not available.
                                            onNavigateToMap(0.0, 0.0, streetAddress)
                                        } else {
                                            snackbarScope.launch {
                                                snackbarHostState.showSnackbar("No destination available")
                                            }
                                        }
                                    }
                            )

                            // Overlay text (keeps user hint visible)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(8.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                SharedText(
                                    text = if (routeEnabled) "Tap to navigate" else "Directions locked",
                                    variant = SharedTextVariant.Caption,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }

                        if (!routeEnabled && routeHintText.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            SharedText(
                                text = routeHintText,
                                variant = SharedTextVariant.Caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Call Customer ─────────────────────────────────────────
                    SharedButton(
                        text        = "Call Customer",
                        onClick     = {
                            viewModel.logCall()
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:") }
                            )
                        },
                        modifier    = Modifier.fillMaxWidth(),
                        variant     = SharedButtonVariant.Outline,
                        leadingIcon = Icons.Filled.Phone,
                    )

                    Spacer(Modifier.height(8.dp))

                    // ── Mark Done ─────────────────────────────────────────────
                    SharedButton(
                        text     = "Mark Done",
                        onClick  = { showCompleteSheet = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled  = !state.isMarkingDone && !isComplete,
                    )



                    // Add a "Chat with Customer" button in ActiveJobScreen:

                    if (!isComplete) {
                        SharedButton(
                            text     = "Chat with Customer",
                            onClick  = { onNavigateToChat(bookingId) },
                            modifier = Modifier.fillMaxWidth(),
                            variant  = SharedButtonVariant.Secondary,
                        )
                    }
                        }
                    },
                )
            }
        }
    }
}