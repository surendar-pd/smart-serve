package com.smartserve.customerapp.ui.app

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedCard
import com.smartserve.sharedui.SharedEmptyState
import com.smartserve.sharedui.SharedErrorState
import com.smartserve.sharedui.SharedLoading
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextField
import com.smartserve.sharedui.SharedTextVariant

private fun providerSortLabel(order: ProviderSortOrder): String = when (order) {
    ProviderSortOrder.Relevance -> "Relevance"
    ProviderSortOrder.HighestRated -> "Highest Rated"
    ProviderSortOrder.CostLowToHigh -> "Cost: Low to High"
    ProviderSortOrder.CostHighToLow -> "Cost: High to Low"
}

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
    var query by rememberSaveable { mutableStateOf("") }
    var sortMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                SharedText(
                    text = categoryLabel,
                    variant = SharedTextVariant.Subtitle,
                )
            }

            Spacer(modifier = Modifier.size(48.dp))
        }

        SharedText(
            text = "Service professionals in this category",
            variant = SharedTextVariant.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SharedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search providers",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { sortMenuExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Sort options",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    SharedText(
                        text = providerSortLabel(sortOrder),
                        variant = SharedTextVariant.Caption,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.UnfoldMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Relevance") },
                        onClick = {
                            viewModel.setSortOrder(ProviderSortOrder.Relevance)
                            sortMenuExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Highest Rated") },
                        onClick = {
                            viewModel.setSortOrder(ProviderSortOrder.HighestRated)
                            sortMenuExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Cost: Low to High") },
                        onClick = {
                            viewModel.setSortOrder(ProviderSortOrder.CostLowToHigh)
                            sortMenuExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Cost: High to Low") },
                        onClick = {
                            viewModel.setSortOrder(ProviderSortOrder.CostHighToLow)
                            sortMenuExpanded = false
                        },
                    )
                }
            }
        }

        when (val s = state) {
            is CategoryListUiState.Loading -> SharedLoading(modifier = Modifier.fillMaxSize())
            is CategoryListUiState.Error   -> SharedErrorState(
                title       = "Unable to Load Providers",
                description = s.message,
                modifier    = Modifier.fillMaxSize(),
            )
            is CategoryListUiState.Success -> {
                val shown = s.providers.filter { provider ->
                    val q = query.trim().lowercase()
                    q.isBlank() ||
                        provider.displayName.lowercase().contains(q) ||
                        provider.serviceTitles.any { it.lowercase().contains(q) } ||
                        provider.serviceDescription.lowercase().contains(q)
                }

                if (shown.isEmpty()) {
                    SharedEmptyState(
                        title       = if (query.isBlank()) "No Providers Available" else "No Matching Providers",
                        description = if (query.isBlank()) {
                            "No providers are currently available in this category."
                        } else {
                            "Please try a different search term."
                        },
                        icon        = Icons.Filled.Group,
                        modifier    = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(shown, key = { it.uid }) { provider ->
                            val rating = if (provider.avgRating > 0) {
                                "%.1f".format(provider.avgRating)
                            } else {
                                "5.0"
                            }
                            val daysLabel = if (provider.categoryAvailabilityDays.isNotEmpty()) {
                                val days = provider.categoryAvailabilityDays
                                if (days.size <= 3) days.joinToString(", ")
                                else "${days.take(3).joinToString(", ")}…"
                            } else {
                                "Flexible"
                            }
                            val visibleServiceTitles = provider.serviceTitles
                                .asSequence()
                                .filter { it.isNotBlank() }
                                .distinct()
                                .take(3)
                                .toList()

                            SharedCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                                elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp),
                                onClick = { onProviderClick(provider.uid, provider.displayName) },
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 84.dp, height = 84.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        SharedAvatar(name = provider.displayName, size = 44.dp)
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Top,
                                        ) {
                                            SharedText(
                                                text = provider.displayName,
                                                variant = SharedTextVariant.BodyStrong,
                                                modifier = Modifier.weight(1f),
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        SharedText(
                                            text = "Services provided",
                                            variant = SharedTextVariant.Caption,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        if (visibleServiceTitles.isNotEmpty()) {
                                            visibleServiceTitles.forEach { title ->
                                                SharedText(
                                                    text = "• $title",
                                                    variant = SharedTextVariant.Caption,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        } else {
                                            SharedText(
                                                text = "• Services will appear here",
                                                variant = SharedTextVariant.Caption,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Filled.Star,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                SharedText(
                                                    text = if (provider.totalReviews > 0) "$rating (${provider.totalReviews})" else rating,
                                                    variant = SharedTextVariant.Caption,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }

                                            Spacer(modifier = Modifier.weight(1f))

                                            IconButton(
                                                onClick = { onProviderClick(provider.uid, provider.displayName) },
                                                modifier = Modifier.size(28.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    contentDescription = "View services",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        }

                                        SharedText(
                                            text = "$daysLabel · ${provider.categoryAvailabilityStart.ifBlank { "--" }}-${provider.categoryAvailabilityEnd.ifBlank { "--" }}",
                                            variant = SharedTextVariant.Caption,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
}
