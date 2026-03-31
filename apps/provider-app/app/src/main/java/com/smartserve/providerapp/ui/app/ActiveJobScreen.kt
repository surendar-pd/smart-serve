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
import androidx.compose.runtime.remember
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
import com.smartserve.sharedui.SharedLoading
import com.smartserve.sharedui.SharedScaffold
import com.smartserve.sharedui.SharedText
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
                val mapAddress = req.homeAddress
                    .ifBlank { req.neighborhood }
                    .ifBlank { req.customerFirstName }
                val isComplete = req.status == RequestStatus.COMPLETED

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
                            SharedText(
                                text    = "Destination: ${mapAddress.ifBlank { "N/A" }}",
                                variant = SharedTextVariant.Caption,
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

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
                                    title = mapAddress.ifBlank { "Customer Location" }
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
                                    if (isComplete) {
                                        snackbarScope.launch {
                                            snackbarHostState.showSnackbar("Job completed: navigation disabled")
                                        }
                                        return@clickable
                                    }

                                    val destinationLabel = mapAddress.ifBlank { "Customer" }
                                    if (req.customerLat != 0.0 && req.customerLng != 0.0) {
                                        onNavigateToMap(
                                            req.customerLat,
                                            req.customerLng,
                                            destinationLabel,
                                        )
                                    } else if (mapAddress.isNotBlank()) {
                                        // Use in-app OSM map screen; ViewModel handles address geocoding when coordinates are not available.
                                        onNavigateToMap(0.0, 0.0, mapAddress)
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
                                text = "Tap to navigate",
                                variant = SharedTextVariant.Caption,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Star rating ───────────────────────────────────────────
                    SharedText(text = "Rate Customer", variant = SharedTextVariant.BodyStrong)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { star ->
                            Icon(
                                imageVector        = Icons.Filled.Star,
                                contentDescription = "Star $star",
                                tint = if (star <= state.currentRating)
                                           MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { viewModel.rateCustomer(star.toFloat()) },
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

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
                        onClick  = { viewModel.markDone() },
                        modifier = Modifier.fillMaxWidth(),
                        loading  = state.isMarkingDone,
                        enabled  = !state.isMarkingDone,
                    )



                    // Add a "Chat with Customer" button in ActiveJobScreen:

                    SharedButton(
                        text     = "Chat with Customer",
                        onClick  = { onNavigateToChat(bookingId) },
                        modifier = Modifier.fillMaxWidth(),
                        variant  = SharedButtonVariant.Secondary,
                    )
                }
            }
        }
    }
}