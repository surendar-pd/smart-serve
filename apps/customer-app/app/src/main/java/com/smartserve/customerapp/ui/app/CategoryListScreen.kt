package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedEmptyState
import com.smartserve.sharedui.SharedErrorState
import com.smartserve.sharedui.SharedListItem
import com.smartserve.sharedui.SharedLoading

@Composable
fun CategoryListScreen(
    modifier: Modifier = Modifier,
    categoryId: String = "",
    categoryLabel: String = "Services",
    onBack: () -> Unit,
    onProviderClick: (providerUid: String, providerName: String) -> Unit = { _, _ -> },
    viewModel: CategoryListViewModel = hiltViewModel(),
) {
    LaunchedEffect(categoryId) {
        if (categoryId.isNotBlank()) viewModel.load(categoryId)
    }

    val state by viewModel.state.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        CustomerStackHeader(
            title = categoryLabel,
            subtitle = "Providers for this category",
            onBack = onBack,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

        // Sort chips
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = sortOrder == ProviderSortOrder.Rating,
                onClick  = { viewModel.setSortOrder(ProviderSortOrder.Rating) },
                label    = { Text("Top Rated") },
            )
            FilterChip(
                selected = sortOrder == ProviderSortOrder.PriceLow,
                onClick  = { viewModel.setSortOrder(ProviderSortOrder.PriceLow) },
                label    = { Text("Price: Low") },
            )
            FilterChip(
                selected = sortOrder == ProviderSortOrder.PriceHigh,
                onClick  = { viewModel.setSortOrder(ProviderSortOrder.PriceHigh) },
                label    = { Text("Price: High") },
            )
        }

        when (val s = state) {
            is CategoryListUiState.Loading -> SharedLoading(modifier = Modifier.fillMaxSize())
            is CategoryListUiState.Error   -> SharedErrorState(
                title       = "Couldn't load providers",
                description = s.message,
                modifier    = Modifier.fillMaxSize(),
            )
            is CategoryListUiState.Success -> {
                if (s.providers.isEmpty()) {
                    SharedEmptyState(
                        title       = "No providers yet",
                        description = "No one has listed services in this category yet.",
                        icon        = Icons.Filled.Group,
                        modifier    = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn {
                        itemsIndexed(s.providers) { index, provider ->
                            val subtitle = buildString {
                                if (provider.avgRating > 0) {
                                    append("★ ${"%.1f".format(provider.avgRating)}")
                                    if (provider.totalReviews > 0) append(" (${provider.totalReviews})")
                                } else {
                                    append("New provider")
                                }
                                if (provider.categoryServiceRate > 0) {
                                    append(" · $${provider.categoryServiceRate.toInt()}/hr")
                                }
                                if (provider.categoryAvailabilityDays.isNotEmpty()) {
                                    val days = provider.categoryAvailabilityDays
                                    val daysLabel = if (days.size <= 3) days.joinToString(", ")
                                                    else "${days.take(3).joinToString(", ")}…"
                                    append(" · $daysLabel")
                                }
                                if (provider.categoryAvailabilityStart.isNotBlank()
                                    && provider.categoryAvailabilityEnd.isNotBlank()) {
                                    append(" ${provider.categoryAvailabilityStart}–${provider.categoryAvailabilityEnd}")
                                }
                            }
                            SharedListItem(
                                title         = provider.displayName,
                                supportingText = subtitle,
                                leadingAvatar = { SharedAvatar(name = provider.displayName, size = 40.dp) },
                                trailing = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                showDivider = index > 0,
                                onClick = { onProviderClick(provider.uid, provider.displayName) },
                            )
                        }
                    }
                }
            }
        }
    }
}
