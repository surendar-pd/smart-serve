package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedButtonVariant
import com.smartserve.sharedui.SharedLoading
import com.smartserve.sharedui.SharedSwitchRow
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextField
import com.smartserve.sharedui.SharedTextVariant
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Surface
import com.smartserve.sharedauth.AddressValidState
import com.smartserve.sharedauth.AddressValidationStatusRow
import com.smartserve.sharedauth.GeoResult
import com.smartserve.sharedui.SharedListItem
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onOpenPrivacyData: () -> Unit = {},
    onOpenComingSoon: () -> Unit = {},
    onOpenPersonalizationInfo: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.savedOk) {
        if (state.savedOk) {
            delay(2000)
            viewModel.clearSaved()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        CustomerStackHeader(
            title = "Profile",
            subtitle = "Account and preferences",
            onBack = onBack,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SharedLoading()
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SharedAvatar(name = state.name.ifBlank { "User" }, size = 80.dp)

            Spacer(modifier = Modifier.height(8.dp))

            SharedText(text = state.name.ifBlank { "User" }, variant = SharedTextVariant.Title)
            SharedText(text = state.email, variant = SharedTextVariant.Body)

            Spacer(modifier = Modifier.height(28.dp))

            SharedText(
                text = "Personal Info",
                variant = SharedTextVariant.Title,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            SharedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = "Name",
            )

            Spacer(modifier = Modifier.height(8.dp))

            SharedTextField(
                value = state.email,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = "Email",
                readOnly = true,
                enabled = false,
            )

            Spacer(modifier = Modifier.height(8.dp))

            SharedTextField(
                value = state.phone,
                onValueChange = viewModel::onPhoneChange,
                modifier = Modifier.fillMaxWidth(),
                label = "Phone",
                placeholder = "e.g. +1 613 555 0100",
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SharedTextField(
                    value = state.homeAddress,
                    onValueChange = viewModel::onAddressChange,
                    modifier = Modifier.weight(1f),
                    label = "Home Address",
                    placeholder = "e.g. 123 Main St, Ottawa",
                )
                SharedButton(
                    text = "Verify",
                    onClick = viewModel::validateAddress,
                    enabled = state.homeAddress.isNotBlank() &&
                              state.addressValidState != AddressValidState.Validating,
                    loading = state.addressValidState == AddressValidState.Validating,
                    variant = SharedButtonVariant.Outline,
                )
            }

            // Autocomplete suggestions (appear while typing, disappear when one is picked)
            if (state.addressSuggestions.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    tonalElevation = 4.dp,
                    shadowElevation = 2.dp,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        state.addressSuggestions.forEachIndexed { index, suggestion ->
                            if (index > 0) {
                                androidx.compose.material3.HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onSuggestionSelected(suggestion) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    SharedText(
                                        text = suggestion.shortLabel,
                                        variant = SharedTextVariant.Body,
                                    )
                                    if (!suggestion.isInOttawa) {
                                        SharedText(
                                            text = "Outside Ottawa area",
                                            variant = SharedTextVariant.Caption,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            AddressValidationStatusRow(
                state = state.addressValidState,
                geoResult = state.addressGeoResult,
            )

            // Show OSM map preview when address is confirmed as Ottawa
            if (state.addressGeoResult != null && state.addressGeoResult!!.isInOttawa) {
                Spacer(modifier = Modifier.height(8.dp))
                OsmAddressMapPreview(
                    lat = state.addressGeoResult!!.lat,
                    lon = state.addressGeoResult!!.lon,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(modifier = Modifier.height(16.dp))

            SharedText(
                text = "Preferences",
                variant = SharedTextVariant.Title,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))

            SharedSwitchRow(
                checked = state.locationAwareness,
                onCheckedChange = viewModel::onLocationToggle,
                title = "Location Awareness",
                description = "Use your location for better service matches",
            )

            SharedSwitchRow(
                checked = state.pushNotifications,
                onCheckedChange = viewModel::onNotifToggle,
                title = "Push Notifications",
                description = "Get notified about bookings and updates",
            )

            Spacer(modifier = Modifier.height(10.dp))

            SharedListItem(
                title = "Privacy & Data",
                leadingIcon = Icons.Filled.PrivacyTip,
                supportingText = "Read the C-SmartService privacy and data template.",
                onClick = onOpenPrivacyData,
            )

            SharedListItem(
                title = "How My Feed Works",
                leadingIcon = Icons.Filled.Insights,
                supportingText = "See the context signals ranking your home screen right now.",
                onClick = onOpenPersonalizationInfo,
            )

            SharedListItem(
                title = "What's Coming",
                leadingIcon = Icons.Filled.AutoAwesome,
                supportingText = "See planned features and future roadmap.",
                onClick = onOpenComingSoon,
            )

            Spacer(modifier = Modifier.height(20.dp))

            SharedText(
                text = "Your favorites",
                variant = SharedTextVariant.Title,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (state.favoriteServices.isEmpty()) {
                SharedText(
                    text = "No favorites yet. Tap hearts on services you love.",
                    variant = SharedTextVariant.Body,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                state.favoriteServices.take(5).forEach { service ->
                    SharedListItem(
                        title = service.title,
                        supportingText = "${service.providerName} • $${service.hourlyRate.toInt()}/hr",
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            SharedButton(
                text = if (state.savedOk) "Saved!" else "Save Changes",
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                loading = state.isSaving,
            )

            Spacer(modifier = Modifier.height(8.dp))

            SharedButton(
                text = "Log Out",
                onClick = {
                    viewModel.signOut()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth(),
                variant = SharedButtonVariant.Ghost,
            )

            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                SharedText(
                    text = state.errorMessage!!,
                    variant = SharedTextVariant.Caption,
                )
            }
        }
    }
}

@Composable
private fun OsmAddressMapPreview(
    lat: Double,
    lon: Double,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Configuration.getInstance().userAgentValue = "SmartServe/1.0"
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(false)
            isClickable = false
            controller.setZoom(16.0)
        }
    }
    val marker = remember { Marker(mapView) }
    LaunchedEffect(lat, lon) {
        val gp = OsmGeoPoint(lat, lon)
        marker.position = gp
        if (!mapView.overlays.contains(marker)) mapView.overlays.add(marker)
        mapView.controller.setCenter(gp)
        mapView.invalidate()
    }
    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }
    AndroidView(
        factory = { mapView },
        modifier = modifier,
    )
}
