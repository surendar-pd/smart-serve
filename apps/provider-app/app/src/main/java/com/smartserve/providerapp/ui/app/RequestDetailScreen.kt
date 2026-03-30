package com.smartserve.providerapp.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel          // ← ADD this import
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedButtonVariant
import com.smartserve.sharedui.SharedLoading
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextArea
import com.smartserve.sharedui.SharedTextVariant

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
                is RequestDetailEvent.NavigateToActive -> onNavigateToActiveJob(event.bookingId)
                RequestDetailEvent.NavigateBack        -> onBack()
            }
        }
    }

    // ── rest of your UI is UNCHANGED from here down ───────────────────────────
    Column(modifier = modifier.fillMaxSize()) {
        ProviderStackHeader(
            title = "Request details",
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

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
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

                        Spacer(Modifier.height(16.dp))
                        SharedText(text = req.serviceType, variant = SharedTextVariant.BodyStrong)
                        Spacer(Modifier.height(12.dp))

                        DetailRow(label = "Customer", value = req.customerFirstName)
                        DetailRow(label = "Date",     value = req.date)
                        DetailRow(label = "Time",     value = req.time)
                        DetailRow(label = "Area",     value = req.neighborhood)

                        if (req.specialInstructions.isNotBlank()) {
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

                        SharedButton(
                            text     = "Accept",
                            onClick  = { viewModel.accept() },
                            modifier = Modifier.fillMaxWidth(),
                            loading  = state.isActing,
                            enabled  = !state.isActing,
                        )

                        Spacer(Modifier.height(8.dp))

                        SharedButton(
                            text     = "Decline",
                            onClick  = { viewModel.decline() },
                            modifier = Modifier.fillMaxWidth(),
                            variant  = SharedButtonVariant.Destructive,
                            enabled  = !state.isActing,
                        )

                        // ── Show chat button for PENDING and ACTIVE requests ─────────────────
                        if (state.request?.status in listOf(RequestStatus.PENDING, RequestStatus.ACTIVE)) {
                            Spacer(Modifier.height(8.dp))
                            SharedButton(
                                text     = "Open Chat",
                                onClick  = { onNavigateToChat(state.request!!.id) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        SharedText(
                            text     = "Customer will be notified",
                            variant  = SharedTextVariant.Caption,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )

                        if (state.errorMessage != null) {
                            Spacer(Modifier.height(8.dp))
                            SharedText(text = state.errorMessage!!, variant = SharedTextVariant.Caption)
                        }
                    }

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
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        SharedText(text = "$label: ", variant = SharedTextVariant.BodyStrong)
        SharedText(text = value,      variant = SharedTextVariant.Body)
    }
}