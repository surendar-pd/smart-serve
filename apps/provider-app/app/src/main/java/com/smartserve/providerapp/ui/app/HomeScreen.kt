package com.smartserve.providerapp.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedCard
import com.smartserve.sharedui.SharedChip
import com.smartserve.sharedui.SharedEmptyState
import com.smartserve.sharedui.SharedErrorState
import com.smartserve.sharedui.SharedLoading
import com.smartserve.sharedui.SharedSwitchRow
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant

private val filterOptions: List<Pair<String, RequestStatus>> = listOf(
    "New" to RequestStatus.PENDING,
    "Active" to RequestStatus.ACTIVE,
    "Completed" to RequestStatus.COMPLETED,
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToRequestDetail: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val firstName = FirebaseAuth.getInstance().currentUser
        ?.displayName?.substringBefore(" ")?.takeIf { it.isNotBlank() } ?: "there"

    Column(modifier = modifier.fillMaxSize()) {
        ProviderTabHeader(
            title = "Hello, $firstName",
            subtitle = "Manage your requests",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            trailing = {
                SharedAvatar(name = firstName, size = 40.dp)
            },
        )

        Spacer(Modifier.height(16.dp))

        // ── Online / Offline toggle ───────────────────────────────────────────
        SharedSwitchRow(
            checked         = state.isOnline,
            onCheckedChange = { viewModel.toggleOnline() },
            title           = if (state.isOnline) "You are Online" else "You are Offline",
            description     = if (state.isOnline) "Accepting new requests"
                              else "Not visible to customers",
            modifier        = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(8.dp))

        // ── Filter chips ──────────────────────────────────────────────────────
        LazyRow(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filterOptions) { (label, status) ->
                SharedChip(
                    label            = label,
                    selected         = state.activeFilter == status,
                    onSelectedChange = { _ -> viewModel.setFilter(status) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Body ──────────────────────────────────────────────────────────────
        when {
            state.isLoading -> Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { SharedLoading() }

            state.errorMessage != null -> SharedErrorState(
                title = state.errorMessage!!,
            )

            else -> {
                val requests = viewModel.filteredRequests()

                if (requests.isEmpty()) {
                    SharedEmptyState(title = "No requests here")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(requests, key = { it.id }) { request ->
                            RequestCard(
                                request = request,
                                onClick = { onNavigateToRequestDetail(request.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestCard(request: ServiceRequest, onClick: () -> Unit) {
    SharedCard(onClick = onClick) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SharedAvatar(
                name = request.customerFirstName.ifBlank { request.customerInitials },
                size = 44.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                SharedText(text = request.customerFirstName, variant = SharedTextVariant.BodyStrong)
                SharedText(text = request.serviceType, variant = SharedTextVariant.Body)
                SharedText(
                    text    = "${request.date} · ${request.time}",
                    variant = SharedTextVariant.Caption,
                )
                if (request.specialInstructions.isNotBlank()) {
                    SharedText(
                        text = "Note: ${request.specialInstructions}",
                        variant = SharedTextVariant.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                SharedText(
                    text = when (request.status) {
                        RequestStatus.PENDING -> "● Pending"
                        RequestStatus.ACTIVE -> "● Active"
                        RequestStatus.COMPLETED -> "● Completed"
                        RequestStatus.DECLINED -> "● Declined"
                        RequestStatus.NEW -> "● New"
                    },
                    variant = SharedTextVariant.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "View",
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

 