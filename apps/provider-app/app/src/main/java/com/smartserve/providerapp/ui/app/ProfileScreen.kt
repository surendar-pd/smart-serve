package com.smartserve.providerapp.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.firestore.GeoPoint
import com.smartserve.sharedauth.AuthViewModel
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedBottomSheet
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedButtonVariant
import com.smartserve.sharedui.SharedChip
import com.smartserve.sharedui.SharedDropdown
import com.smartserve.sharedui.SharedListItem
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextField
import com.smartserve.sharedui.SharedTextVariant
import com.smartserve.sharedui.SharedTimePickerDialog
import com.smartserve.sharedui.SharedSwitchRow
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import android.content.Intent
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class ProfileAvailabilityField { Start, End }

private fun formatTime12(hour24: Int, minute: Int): String {
    val period = if (hour24 < 12) "AM" else "PM"
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return "$hour12:${minute.toString().padStart(2, '0')} $period"
}

private fun parseTimeForPicker(value: String, defaultHour: Int, defaultMinute: Int = 0): Pair<Int, Int> {
    val text = value.trim()
    val patterns = listOf("h:mm a", "hh:mm a", "H:mm", "HH:mm")
    for (pattern in patterns) {
        runCatching {
            val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
            val parsed = sdf.parse(text) ?: return@runCatching null
            val cal = Calendar.getInstance().apply { time = parsed }
            cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
        }.getOrNull()?.let { return it }
    }
    return defaultHour to defaultMinute
}

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    onOpenServices: () -> Unit = {},
    onOpenPrivacyData: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val serviceLabels = state.serviceOptions.map { it.title.ifBlank { "Service" } }
    var areaServiceExpanded by remember { mutableStateOf(false) }
    var availabilityServiceExpanded by remember { mutableStateOf(false) }
    var activeAvailabilityField by remember { mutableStateOf<ProfileAvailabilityField?>(null) }

    Column(
        modifier            = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProviderTabHeader(
            title = "Profile",
            subtitle = "Account and settings",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SharedAvatar(name = state.displayName, size = 64.dp)

            Spacer(Modifier.height(8.dp))

            SharedText(text = state.displayName, variant = SharedTextVariant.Title)
            SharedText(text = "Member since ${state.memberSince}", variant = SharedTextVariant.Body)

            Spacer(Modifier.height(28.dp))

            SharedListItem(
                title = "Services and Details",
                leadingIcon = Icons.Filled.Build,
                supportingText = "Your listings: category, pricing, description, and photos.",
                onClick = onOpenServices,
            )
            SharedListItem(
                title = "Serviceable Areas",
                leadingIcon = Icons.Filled.LocationOn,
                supportingText = "Map center and radius for each service.",
                onClick = viewModel::openAreaSheet,
            )
            SharedListItem(
                title = "Availability Hours",
                leadingIcon = Icons.Filled.AccessTime,
                supportingText = "Days and time windows for each service.",
                onClick = viewModel::openAvailabilitySheet,
            )
            SharedListItem(
                title = "Notification Settings",
                leadingIcon = Icons.Filled.Notifications,
                onClick = viewModel::openNotificationSheet,
            )
            SharedListItem(
                title = "Privacy & Data",
                leadingIcon = Icons.Filled.PrivacyTip,
                onClick = onOpenPrivacyData,
            )

            Spacer(Modifier.height(28.dp))

            SharedButton(
                text = "Log Out",
                onClick = { authViewModel.signOut(); onLogout() },
                modifier = Modifier.fillMaxWidth(),
                variant = SharedButtonVariant.Ghost,
            )

            state.errorMessage?.let { msg ->
                Spacer(Modifier.height(8.dp))
                SharedText(text = msg, variant = SharedTextVariant.Caption)
            }
        }
    }

    SharedBottomSheet(
        isOpen = state.notificationSheetOpen,
        onOpenChange = { if (!it) viewModel.closeNotificationSheet() },
        skipPartiallyExpanded = true,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SharedText(text = "Notification Settings", variant = SharedTextVariant.BodyStrong)

                SharedSwitchRow(
                    checked = state.pushNotifications,
                    onCheckedChange = viewModel::onPushNotificationsChange,
                    title = "Push Notifications",
                    description = "Master switch for provider notifications",
                )

                SharedSwitchRow(
                    checked = state.requestNotifications,
                    onCheckedChange = viewModel::onRequestNotificationsChange,
                    title = "New Request Alerts",
                    description = "Notify when customers send new requests",
                )

                SharedSwitchRow(
                    checked = state.serviceReminderNotifications,
                    onCheckedChange = viewModel::onServiceReminderNotificationsChange,
                    title = "Service Day Reminders",
                    description = "Notify on the day of active services",
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SharedButton(
                        text = "Cancel",
                        onClick = viewModel::closeNotificationSheet,
                        modifier = Modifier.weight(1f),
                        variant = SharedButtonVariant.Ghost,
                    )
                    SharedButton(
                        text = "Save",
                        onClick = viewModel::saveNotificationSettings,
                        modifier = Modifier.weight(1f),
                        loading = state.isSaving,
                        enabled = !state.isSaving,
                    )
                }
            }
        },
        content = { },
    )

    SharedBottomSheet(
        isOpen = state.areaSheetOpen,
        onOpenChange = { if (!it) viewModel.closeAreaSheet() },
        sheetContent = {
            if (state.serviceOptions.isEmpty()) {
                ProfileServiceSheetEmpty(
                    message = "Add a service first.",
                    onAddService = {
                        viewModel.closeAreaSheet()
                        onOpenServices()
                    },
                    onClose = viewModel::closeAreaSheet,
                )
            } else {
                val context = LocalContext.current
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SharedText(text = "Serviceable Area", variant = SharedTextVariant.BodyStrong)
                    SharedDropdown(
                        expanded = areaServiceExpanded,
                        onExpandedChange = { areaServiceExpanded = it },
                        options = serviceLabels,
                        selectedOption = state.serviceOptions.firstOrNull { it.id == state.selectedServiceId }?.title,
                        onOptionSelected = { label ->
                            state.serviceOptions.firstOrNull { it.title == label }
                                ?.let { viewModel.onSelectedServiceChange(it.id) }
                        },
                        label = "Service",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OsmPointPickerMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        point = GeoPoint(state.areaLat, state.areaLng),
                        onPointPicked = { gp -> viewModel.setMapPoint(gp.latitude, gp.longitude) },
                    )
                    SharedTextField(
                        value = state.areaRadiusKm,
                        onValueChange = viewModel::onRadiusChange,
                        label = "Radius (km)",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SharedButton(
                            text = "Cancel",
                            onClick = viewModel::closeAreaSheet,
                            variant = SharedButtonVariant.Ghost,
                            modifier = Modifier.weight(1f),
                        )
                        SharedButton(
                            text = "Save",
                            onClick = viewModel::saveArea,
                            modifier = Modifier.weight(1f),
                            loading = state.isSaving,
                            enabled = !state.isSaving && state.selectedServiceId.isNotBlank(),
                        )
                    }
                }
            }
        },
        skipPartiallyExpanded = true,
        content = { },
    )

    SharedBottomSheet(
        isOpen = state.availabilitySheetOpen,
        onOpenChange = { if (!it) viewModel.closeAvailabilitySheet() },
        sheetContent = {
            if (state.serviceOptions.isEmpty()) {
                ProfileServiceSheetEmpty(
                    message = "Add a service first.",
                    onAddService = {
                        viewModel.closeAvailabilitySheet()
                        onOpenServices()
                    },
                    onClose = viewModel::closeAvailabilitySheet,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SharedText(text = "Availability Hours", variant = SharedTextVariant.BodyStrong)
                    SharedDropdown(
                        expanded = availabilityServiceExpanded,
                        onExpandedChange = { availabilityServiceExpanded = it },
                        options = serviceLabels,
                        selectedOption = state.serviceOptions.firstOrNull { it.id == state.selectedServiceId }?.title,
                        onOptionSelected = { label ->
                            state.serviceOptions.firstOrNull { it.title == label }
                                ?.let { viewModel.onSelectedServiceChange(it.id) }
                        },
                        label = "Service",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    daysOfWeek.chunked(4).forEach { rowDays ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowDays.forEach { day ->
                                SharedChip(
                                    label = day,
                                    selected = day in state.availabilityDays,
                                    onSelectedChange = { viewModel.toggleAvailabilityDay(day) },
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SharedButton(
                            text = if (state.availabilityStart.isBlank()) "From" else state.availabilityStart,
                            onClick = { activeAvailabilityField = ProfileAvailabilityField.Start },
                            modifier = Modifier.weight(1f),
                            variant = SharedButtonVariant.Outline,
                        )
                        SharedButton(
                            text = if (state.availabilityEnd.isBlank()) "To" else state.availabilityEnd,
                            onClick = { activeAvailabilityField = ProfileAvailabilityField.End },
                            modifier = Modifier.weight(1f),
                            variant = SharedButtonVariant.Outline,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SharedButton(
                            text = "Cancel",
                            onClick = viewModel::closeAvailabilitySheet,
                            variant = SharedButtonVariant.Ghost,
                            modifier = Modifier.weight(1f),
                        )
                        SharedButton(
                            text = "Save",
                            onClick = viewModel::saveAvailability,
                            modifier = Modifier.weight(1f),
                            loading = state.isSaving,
                            enabled = !state.isSaving && state.selectedServiceId.isNotBlank(),
                        )
                    }
                }
            }
        },
        skipPartiallyExpanded = true,
        content = { },
    )

    val (initialHour, initialMinute) = when (activeAvailabilityField) {
        ProfileAvailabilityField.Start -> parseTimeForPicker(state.availabilityStart, defaultHour = 9)
        ProfileAvailabilityField.End -> parseTimeForPicker(state.availabilityEnd, defaultHour = 17)
        null -> 9 to 0
    }
    SharedTimePickerDialog(
        isOpen = activeAvailabilityField != null,
        title = if (activeAvailabilityField == ProfileAvailabilityField.Start) "Start time" else "End time",
        initialHour = initialHour,
        initialMinute = initialMinute,
        onDismiss = { activeAvailabilityField = null },
        onConfirm = { hour, minute ->
            when (activeAvailabilityField) {
                ProfileAvailabilityField.Start -> viewModel.onAvailabilityStartChange(formatTime12(hour, minute))
                ProfileAvailabilityField.End -> viewModel.onAvailabilityEndChange(formatTime12(hour, minute))
                null -> Unit
            }
            activeAvailabilityField = null
        },
    )
}

@Composable
private fun ProfileServiceSheetEmpty(
    message: String,
    onAddService: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SharedText(text = message, variant = SharedTextVariant.BodyStrong)
        SharedButton(
            text = "Add service",
            onClick = onAddService,
            modifier = Modifier.fillMaxWidth(),
        )
        SharedButton(
            text = "Close",
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            variant = SharedButtonVariant.Ghost,
        )
    }
}

@Composable
private fun OsmPointPickerMap(
    modifier: Modifier,
    point: GeoPoint,
    onPointPicked: (GeoPoint) -> Unit,
) {
    val context = LocalContext.current

    // OSMDroid requires a user agent; cache dirs help tile loading.
    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().osmdroidBasePath = context.cacheDir
        Configuration.getInstance().osmdroidTileCache = File(context.cacheDir, "osmdroid")
        onDispose { }
    }

    val osmPoint = remember(point.latitude, point.longitude) { OsmGeoPoint(point.latitude, point.longitude) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { mv ->
            // Center camera reliably
            mv.controller.setCenter(osmPoint)

            // Clear + add marker + tap overlay
            mv.overlays.removeAll { it is Marker || it is MapEventsOverlay }
            val marker = Marker(mv).apply {
                position = osmPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Service center"
            }
            mv.overlays.add(marker)
            mv.overlays.add(
                MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: OsmGeoPoint?): Boolean {
                        p ?: return false
                        onPointPicked(GeoPoint(p.latitude, p.longitude))
                        return true
                    }

                    override fun longPressHelper(p: OsmGeoPoint?): Boolean {
                        p ?: return false
                        onPointPicked(GeoPoint(p.latitude, p.longitude))
                        return true
                    }
                }),
            )
            mv.invalidate()
        },
    )

    // Required MapView lifecycle for tiles to load.
    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }
}