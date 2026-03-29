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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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

@Composable
fun ActiveJobScreen(
    bookingId: String,
    modifier: Modifier = Modifier,
    onNavigateToBookings: () -> Unit,
    viewModel: ActiveJobViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

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
                        SharedText(text = "● Status: In Progress", variant = SharedTextVariant.BodyStrong)
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
                                text    = "${req.date} · ${req.time} · ${req.neighborhood}",
                                variant = SharedTextVariant.Caption,
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Route placeholder ─────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector        = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint               = MaterialTheme.colorScheme.primary,
                                modifier           = Modifier.size(28.dp),
                            )
                            Spacer(Modifier.height(4.dp))
                            SharedText(text = "Route to Customer", variant = SharedTextVariant.Caption)
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
                }
            }
        }
    }
}