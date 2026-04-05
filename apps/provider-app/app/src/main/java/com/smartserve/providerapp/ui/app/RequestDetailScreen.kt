package com.smartserve.providerapp.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel          // ← ADD this import
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedButtonVariant
import com.smartserve.sharedui.SharedBottomSheet
import com.smartserve.sharedui.SharedLoading
import com.smartserve.sharedui.SharedRating
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextArea
import com.smartserve.sharedui.SharedTextVariant
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private fun streetAddressOnly(address: String): String {
    val value = address.trim()
    if (value.isBlank()) return ""
    return value.substringBefore(",").trim().ifBlank { value }
}

@Composable
fun RequestDetailScreen(
    bookingId: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onNavigateToActiveJob: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
) {
    // ── Get the AssistedFactory via Hilt, then build VM with bookingId ───────
    val holder: RequestDetailAssistedFactoryHolder = hiltViewModel()
    val viewModel: RequestDetailViewModel = viewModel(
        key     = "RequestDetail_$bookingId",
        factory = provideRequestDetailViewModel(holder.factory, bookingId),
    )

    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                RequestDetailEvent.NavigateBack        -> onBack()
                is RequestDetailEvent.NavigateToActiveJob -> onNavigateToActiveJob(event.bookingId)
            }
        }
    }

    // ── rest of your UI is UNCHANGED from here down ───────────────────────────
    Column(modifier = modifier.fillMaxSize()) {
        ProviderStackHeader(
            title = "Booking details",
            subtitle = "Review and respond to the customer",
            onBack = onBack,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

        when {
            state.isLoading -> Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { SharedLoading() }

            state.request == null -> Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { SharedText(text = "Request not found", variant = SharedTextVariant.Body) }

            else -> {
                val req = state.request!!
                val streetAddress = streetAddressOnly(
                    req.homeAddress.ifBlank { req.neighborhood }
                )
                var showCompleteSheet by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
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
                                                .clickable(enabled = !state.isActing) {
                                                    viewModel.setRating(star.toFloat())
                                                },
                                        )
                                    }
                                }

                                SharedButton(
                                    text = if (state.isActing) "Completing…" else "Complete service",
                                    onClick = { viewModel.completeService() },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = state.currentRating > 0f && !state.isActing,
                                    loading = state.isActing,
                                )

                                SharedButton(
                                    text = "Cancel",
                                    onClick = { showCompleteSheet = false },
                                    modifier = Modifier.fillMaxWidth(),
                                    variant = SharedButtonVariant.Ghost,
                                    enabled = !state.isActing,
                                )
                            }
                        },
                        content = { _ ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                            ) {
                        val hideLocation = req.status == RequestStatus.COMPLETED

                        if (!hideLocation) {
                            CustomerLocationMap(
                                lat = req.location?.latitude ?: req.customerLat,
                                lon = req.location?.longitude ?: req.customerLng,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        DetailRow(label = "Type", value = req.categoryLabel.ifBlank { req.categoryId })
                        DetailRow(label = "Service", value = req.serviceType)
                        DetailRow(label = "Customer", value = req.customerFirstName)
                        DetailRow(label = "Date",     value = req.date)
                        DetailRow(label = "Time",     value = req.time)
                        if (!hideLocation) {
                            DetailRow(label = "Street", value = streetAddress)
                        }
                        if (req.status == RequestStatus.COMPLETED && req.customerRating != null) {
                            Spacer(Modifier.height(6.dp))
                            SharedText(
                                text = "Your rating",
                                variant = SharedTextVariant.Subtitle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                            )
                            SharedRating(rating = req.customerRating, starSize = 24.dp)
                        }

                        if (!hideLocation && req.specialInstructions.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            SharedText(text = "Special Instructions", variant = SharedTextVariant.BodyStrong)
                            Spacer(Modifier.height(4.dp))
                            SharedTextArea(
                                value         = req.specialInstructions,
                                onValueChange = {},
                                readOnly      = true,
                                modifier      = Modifier.fillMaxWidth(),
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        when (req.status) {
                            RequestStatus.PENDING -> {
                                SharedButton(
                                    text = "Accept",
                                    onClick = { viewModel.accept() },
                                    modifier = Modifier.fillMaxWidth(),
                                    loading = state.isActing,
                                    enabled = !state.isActing,
                                )

                                Spacer(Modifier.height(8.dp))

                                SharedButton(
                                    text = "Decline",
                                    onClick = { viewModel.decline() },
                                    modifier = Modifier.fillMaxWidth(),
                                    variant = SharedButtonVariant.Destructive,
                                    enabled = !state.isActing,
                                )
                            }

                            RequestStatus.ACTIVE -> {
                                SharedButton(
                                    text = "Mark Done",
                                    onClick = { showCompleteSheet = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !state.isActing,
                                )
                            }

                            else -> Unit
                        }

                        // ── Show chat button for PENDING and ACTIVE requests ─────────────────
                        if (state.request?.status in listOf(RequestStatus.PENDING, RequestStatus.ACTIVE)) {
                            Spacer(Modifier.height(8.dp))
                            SharedButton(
                                text     = "Open Chat",
                                onClick  = { onNavigateToChat(state.request!!.id) },
                                modifier = Modifier.fillMaxWidth(),
                                variant  = SharedButtonVariant.Outline,
                                leadingIcon = Icons.Outlined.ChatBubbleOutline,
                            )
                        }

                        if (state.errorMessage != null) {
                            Spacer(Modifier.height(8.dp))
                            SharedText(text = state.errorMessage!!, variant = SharedTextVariant.Caption)
                        }
                            }
                        },
                    )

                    if (state.isActing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center,
                        ) { SharedLoading() }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerLocationMap(
    lat: Double?,
    lon: Double?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    if (lat == null || lon == null || (lat == 0.0 && lon == 0.0)) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector        = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(32.dp),
                )
                Spacer(Modifier.height(4.dp))
                SharedText(text = "Customer Location", variant = SharedTextVariant.Caption)
            }
        }
        return
    }

    Configuration.getInstance().userAgentValue = "SmartServe/1.0"

    val mapView = androidx.compose.runtime.remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(OsmGeoPoint(lat, lon))
        }
    }
    val marker = androidx.compose.runtime.remember { Marker(mapView) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(marker)
    }
    androidx.compose.runtime.LaunchedEffect(lat, lon) {
        val p = OsmGeoPoint(lat, lon)
        marker.position = p
        mapView.controller.animateTo(p)
        mapView.invalidate()
    }
    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { mapView },
        modifier = modifier,
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        SharedText(
            text = label,
            variant = SharedTextVariant.Subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        SharedText(text = value, variant = SharedTextVariant.Body)
    }
}