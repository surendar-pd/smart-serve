package com.smartserve.providerapp.ui.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedButtonVariant
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant
import com.smartserve.sharedui.SharedTopAppBar
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File

@Composable
fun ProviderNavigationScreen(
    customerLat: Double,
    customerLng: Double,
    customerAddress: String,
    onBack: () -> Unit,
    bottomPadding: Dp = 0.dp,
    topPadding: Dp = 0.dp,
) {
    val context = LocalContext.current

    // ── Location permission ───────────────────────────────────────────────────
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // ── OSMDroid config — must happen before MapView is created ───────────────
    DisposableEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue   = context.packageName
            osmdroidBasePath = context.cacheDir
            osmdroidTileCache = File(context.cacheDir, "osmdroid")
        }
        onDispose { }
    }

    // ── MapView — created once, reused across recompositions ──────────────────
    val centerPoint = remember {
        if (customerLat == 0.0 && customerLng == 0.0)
            GeoPoint(45.4215, -75.6972)   // Ottawa fallback
        else
            GeoPoint(customerLat, customerLng)
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setUseDataConnection(true)
            controller.setZoom(14.0)
            controller.setCenter(centerPoint)
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

    // ── ViewModel ─────────────────────────────────────────────────────────────
    val holder: ProviderNavigationAssistedFactoryHolder = hiltViewModel()
    val viewModel: ProviderNavigationViewModel = viewModel(
        key     = "Nav_${customerLat}_${customerLng}",
        factory = provideNavigationViewModel(
            factory         = holder.factory,
            customerLat     = customerLat,
            customerLng     = customerLng,
            customerAddress = customerAddress,
        ),
    )

    val state by viewModel.uiState.collectAsState()
    BackHandler {
        onBack()
    }
    // ── UI ────────────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize() 
            .padding(
                top    = topPadding,  
                bottom = bottomPadding, 
            ),  // topPadding removed — AppLayout handles it
    ) {
        // Top app bar removed to avoid duplicate back buttons

        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory  = { mapView },
                modifier = Modifier.fillMaxSize(),
                update   = { map ->
                    map.overlays.clear()

                    // Provider marker
                    state.providerLocation?.let { loc ->
                        map.overlays.add(Marker(map).apply {
                            position = loc
                            title    = "You"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        })
                        map.controller.animateTo(loc)
                    }

                    // Customer marker
                    state.customerLocation?.let { loc ->
                        map.overlays.add(Marker(map).apply {
                            position = loc
                            title    = customerAddress.ifBlank { "Customer" }
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        })
                    }

                    // Route polyline
                    if (state.routePoints.isNotEmpty()) {
                        map.overlays.add(Polyline(map).apply {
                            setPoints(state.routePoints)
                            outlinePaint.color       = android.graphics.Color.parseColor("#4285F4")
                            outlinePaint.strokeWidth = 8f
                        })
                        if (state.routePoints.size > 1) {
                            val box = org.osmdroid.util.BoundingBox.fromGeoPoints(state.routePoints)
                            map.zoomToBoundingBox(box.increaseByScale(1.2f), true)
                        }
                    }

                    map.invalidate()
                },
            )

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            // Show permission warning overlay if no permission
            if (!hasLocationPermission) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    shape         = RoundedCornerShape(8.dp),
                    tonalElevation = 4.dp,
                ) {
                    SharedText(
                        text     = "⚠️ Location permission required for navigation",
                        variant  = SharedTextVariant.Caption,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }

        // Bottom info card
        Surface(
            shape          = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            tonalElevation = 4.dp,
            modifier       = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SharedText(text = "ETA",                              variant = SharedTextVariant.Caption)
                        SharedText(text = state.etaText.ifBlank { "--" },     variant = SharedTextVariant.Title)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SharedText(text = "Distance",                          variant = SharedTextVariant.Caption)
                        SharedText(text = state.distanceText.ifBlank { "--" }, variant = SharedTextVariant.Title)
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (state.customerAddress.isNotBlank()) {
                    SharedText(text = "📍 ${state.customerAddress}", variant = SharedTextVariant.Body)
                    Spacer(Modifier.height(8.dp))
                }

                if (state.errorMessage != null) {
                    SharedText(text = state.errorMessage!!, variant = SharedTextVariant.Caption)
                    Spacer(Modifier.height(8.dp))
                }

                SharedButton(
                    text     = "Refresh Route",
                    onClick  = { viewModel.refreshRoute() },
                    modifier = Modifier.fillMaxWidth(),
                    loading  = state.isLoading,
                    enabled  = !state.isLoading,
                    variant  = SharedButtonVariant.Outline,
                )

                Spacer(Modifier.height(8.dp))

                SharedButton(
                    text = "Open in Maps",
                    onClick = {
                        val customer = state.customerLocation
                        val uri = if (customer != null) {
                            Uri.parse("google.navigation:q=${customer.latitude},${customer.longitude}&mode=d")
                        } else {
                            Uri.parse("google.navigation:q=${Uri.encode(state.customerAddress.ifBlank { customerAddress })}&mode=d")
                        }
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    variant = SharedButtonVariant.Outline,
                )

                Spacer(Modifier.height(8.dp))

                SharedButton(
                    text     = "Back",
                    onClick  = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    variant  = SharedButtonVariant.Secondary,
                )
            }
        }
    }
}