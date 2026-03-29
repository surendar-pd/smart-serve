package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
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
import com.smartserve.sharedui.SharedCard
import com.smartserve.sharedui.SharedEmptyState
import com.smartserve.sharedui.SharedErrorState
import com.smartserve.sharedui.SharedLoading
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant

@Composable
fun ServiceListScreen(
    providerUid: String,
    providerName: String,
    onBack: () -> Unit,
    onSelectService: (providerName: String, serviceName: String, price: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ServiceListViewModel = hiltViewModel(),
) {
    LaunchedEffect(providerUid) {
        viewModel.load(providerUid, providerName)
    }

    val state by viewModel.state.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        CustomerStackHeader(
            title = providerName,
            subtitle = "Choose a service",
            onBack = onBack,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

        when (val s = state) {
            is ServiceListUiState.Loading -> SharedLoading(modifier = Modifier.fillMaxSize())
            is ServiceListUiState.Error -> SharedErrorState(
                title = "Couldn't load services",
                description = s.message,
                modifier = Modifier.fillMaxSize(),
            )
            is ServiceListUiState.Success -> {
                if (s.services.isEmpty()) {
                    SharedEmptyState(
                        title = "No services listed",
                        description = "This provider hasn't listed any active services yet.",
                        icon = Icons.Filled.Build,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(s.services) { service ->
                            val priceLabel = "$${service.hourlyRate.toInt()}/hr"
                            val availabilityHint = "${service.availabilityStart}–${service.availabilityEnd}"
                            SharedCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(12.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SharedAvatar(name = service.title, size = 48.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        SharedText(text = service.title, variant = SharedTextVariant.BodyStrong)
                                        if (service.description.isNotBlank()) {
                                            SharedText(text = service.description, variant = SharedTextVariant.Body)
                                        }
                                        SharedText(
                                            text = priceLabel,
                                            variant = SharedTextVariant.BodyStrong,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        SharedText(
                                            text = availabilityHint,
                                            variant = SharedTextVariant.Caption,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    SharedButton(
                                        text = "Add to Cart",
                                        onClick = { onSelectService(providerName, service.title, priceLabel) },
                                        modifier = Modifier.height(36.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
