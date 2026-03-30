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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
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
    authViewModel: AuthViewModel = hiltViewModel(),
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val serviceLabels = state.serviceOptions.map { it.title.ifBlank { "Service" } }
    val mapPoint = LatLng(state.areaLat, state.areaLng)
    val cameraPositionState = rememberCameraPositionState()
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
                onClick = onOpenServices,
            )
            SharedListItem(
                title = "Serviceable Areas",
                leadingIcon = Icons.Filled.LocationOn,
                supportingText = buildString {
                    val selected = state.serviceOptions.firstOrNull { it.id == state.selectedServiceId }
                    append(selected?.title ?: "No service selected")
                    append(" • Radius ${state.areaRadiusKm} km")
                },
                onClick = viewModel::openAreaSheet,
            )
            SharedListItem(
                title = "Availability Hours",
                leadingIcon = Icons.Filled.AccessTime,
                supportingText = buildString {
                    val selected = state.serviceOptions.firstOrNull { it.id == state.selectedServiceId }
                    append(selected?.title ?: "No service selected")
                    append(" • ${state.availabilityDays.joinToString(", ")} • ${state.availabilityStart}-${state.availabilityEnd}")
                },
                onClick = viewModel::openAvailabilitySheet,
            )
            SharedListItem(
                title = "Notification Settings",
                leadingIcon = Icons.Filled.Notifications,
                onClick = { /* TODO */ },
            )
            SharedListItem(
                title = "Privacy & Data",
                leadingIcon = Icons.Filled.PrivacyTip,
                onClick = { /* TODO */ },
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
        isOpen = state.areaSheetOpen,
        onOpenChange = { if (!it) viewModel.closeAreaSheet() },
        sheetContent = {
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
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = false),
                    uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
                    onMapClick = { latLng -> viewModel.setMapPoint(latLng.latitude, latLng.longitude) },
                ) {
                    androidx.compose.runtime.LaunchedEffect(mapPoint) {
                        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(mapPoint, 12f))
                    }
                    com.google.maps.android.compose.Marker(
                        state = com.google.maps.android.compose.MarkerState(position = mapPoint),
                        title = "Service center",
                    )
                }
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
                        enabled = !state.isSaving,
                    )
                }
            }
        },
    ) { }

    SharedBottomSheet(
        isOpen = state.availabilitySheetOpen,
        onOpenChange = { if (!it) viewModel.closeAvailabilitySheet() },
        sheetContent = {
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
                        enabled = !state.isSaving,
                    )
                }
            }
        },
    ) { }

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