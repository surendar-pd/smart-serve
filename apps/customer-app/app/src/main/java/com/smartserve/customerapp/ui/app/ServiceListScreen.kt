package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedCard
import com.smartserve.sharedui.SharedEmptyState
import com.smartserve.sharedui.SharedErrorState
import com.smartserve.sharedui.SharedLoading
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextField
import com.smartserve.sharedui.SharedTextVariant

private enum class ServiceSortMode {
    Relevance,
    HighestRated,
    PriceLowToHigh,
    PriceHighToLow,
}

private fun serviceSortLabel(mode: ServiceSortMode): String = when (mode) {
    ServiceSortMode.Relevance -> "Relevance"
    ServiceSortMode.HighestRated -> "Highest Rated"
    ServiceSortMode.PriceLowToHigh -> "Cost: Low to High"
    ServiceSortMode.PriceHighToLow -> "Cost: High to Low"
}

private fun relevanceScore(service: CustomerServiceListing, query: String): Int {
    val q = query.trim().lowercase()
    if (q.isBlank()) return 0
    val title = service.title.lowercase()
    val description = service.description.lowercase()
    return when {
        title == q -> 300
        title.startsWith(q) -> 200
        title.contains(q) -> 120
        description.contains(q) -> 60
        else -> 0
    }
}

@Composable
fun ServiceListScreen(
    providerUid: String,
    providerName: String,
    categoryId: String = "",
    categoryLabel: String = "",
    onBack: () -> Unit,
    onSelectService: (CustomerServiceListing) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ServiceListViewModel = hiltViewModel(),
) {
    LaunchedEffect(providerUid, categoryId) {
        viewModel.load(providerUid, providerName, categoryId)
    }

    val state by viewModel.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf(ServiceSortMode.Relevance) }
    var sortMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val headerTitle = categoryLabel.ifBlank { "Services" }
    val context = LocalContext.current

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
                    text = headerTitle,
                    variant = SharedTextVariant.Subtitle,
                )
            }

            IconButton(
                onClick = {
                    val services = (state as? ServiceListUiState.Success)?.services ?: emptyList()
                    shareProvider(context, providerName, services)
                },
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share provider",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SharedText(
            text = providerName,
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
                placeholder = "Search services",
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
                        text = serviceSortLabel(sortMode),
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
                            sortMode = ServiceSortMode.Relevance
                            sortMenuExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Highest Rated") },
                        onClick = {
                            sortMode = ServiceSortMode.HighestRated
                            sortMenuExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Cost: Low to High") },
                        onClick = {
                            sortMode = ServiceSortMode.PriceLowToHigh
                            sortMenuExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Cost: High to Low") },
                        onClick = {
                            sortMode = ServiceSortMode.PriceHighToLow
                            sortMenuExpanded = false
                        },
                    )
                }
            }
        }

        when (val s = state) {
            is ServiceListUiState.Loading -> SharedLoading(modifier = Modifier.fillMaxSize())
            is ServiceListUiState.Error -> SharedErrorState(
                title = "Unable to Load Services",
                description = s.message,
                modifier = Modifier.fillMaxSize(),
            )
            is ServiceListUiState.Success -> {
                val filtered = s.services.filter { service ->
                    val q = query.trim().lowercase()
                    q.isBlank() ||
                        service.title.lowercase().contains(q) ||
                        service.description.lowercase().contains(q)
                }

                val shown = when (sortMode) {
                    ServiceSortMode.Relevance -> filtered.sortedWith(
                        compareByDescending<CustomerServiceListing> { relevanceScore(it, query) }
                            .thenByDescending { it.providerAvgRating }
                            .thenBy { it.hourlyRate },
                    )
                    ServiceSortMode.HighestRated -> filtered.sortedWith(
                        compareByDescending<CustomerServiceListing> { it.providerAvgRating }
                            .thenByDescending { it.providerTotalReviews },
                    )
                    ServiceSortMode.PriceLowToHigh -> filtered.sortedBy { it.hourlyRate }
                    ServiceSortMode.PriceHighToLow -> filtered.sortedByDescending { it.hourlyRate }
                }

                if (shown.isEmpty()) {
                    SharedEmptyState(
                        title = if (query.isBlank()) "No Services Available" else "No Matching Services",
                        description = if (query.isBlank()) {
                            "This provider has not listed any active services yet."
                        } else {
                            "Please try a different search term."
                        },
                        icon = Icons.Filled.Build,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(shown, key = { it.serviceId }) { service ->
                            val priceLabel = "$${service.hourlyRate.toInt()}"
                            val ratingLabel = if (service.providerAvgRating > 0 && service.providerTotalReviews > 0)
                                "%.1f".format(service.providerAvgRating) else null
                            val isFavorite = service.serviceId in s.favoriteServiceIds

                            SharedCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(12.dp),
                                elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp),
                                onClick = { onSelectService(service) },
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    if (service.photoUrls.isNotEmpty()) {
                                        AsyncImage(
                                            model = service.photoUrls.first(),
                                            contentDescription = "${service.title} image",
                                            modifier = Modifier
                                                .size(width = 84.dp, height = 84.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 84.dp, height = 84.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            SharedAvatar(name = service.title, size = 44.dp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Top,
                                        ) {
                                            SharedText(
                                                text = service.title,
                                                variant = SharedTextVariant.BodyStrong,
                                                modifier = Modifier.weight(1f),
                                            )

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Column(horizontalAlignment = Alignment.End) {
                                                SharedText(
                                                    text = priceLabel,
                                                    variant = SharedTextVariant.BodyStrong,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                                SharedText(
                                                    text = "/hour",
                                                    variant = SharedTextVariant.Caption,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }

                                        if (service.description.isNotBlank()) {
                                            Text(
                                                text = service.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            if (ratingLabel != null) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Star,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    SharedText(
                                                        text = "$ratingLabel (${service.providerTotalReviews})",
                                                        variant = SharedTextVariant.Caption,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                            } else {
                                                SharedText(
                                                    text = "New",
                                                    variant = SharedTextVariant.Caption,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }

                                            Spacer(modifier = Modifier.weight(1f))

                                            IconButton(
                                                onClick = {
                                                    viewModel.toggleFavorite(service)
                                                },
                                                modifier = Modifier.size(28.dp),
                                            ) {
                                                Icon(
                                                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                                    contentDescription = "Favorite",
                                                    tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        }

                                        val daysHint = service.availabilityDays.joinToString(", ")
                                        if (daysHint.isNotBlank()) {
                                            SharedText(
                                                text = "$daysHint · ${service.availabilityStart}–${service.availabilityEnd}",
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
}

private fun shareProvider(
    context: android.content.Context,
    providerName: String,
    services: List<CustomerServiceListing>,
) {
    val name = providerName.ifBlank { "a provider" }

    val serviceLine = services
        .map { it.title.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .take(3)
        .joinToString(", ")

    val ratingLine = services.firstOrNull()?.let { s ->
        if (s.providerAvgRating > 0 && s.providerTotalReviews > 0)
            "⭐ ${"%.1f".format(s.providerAvgRating)} (${s.providerTotalReviews} reviews)"
        else null
    }

    val priceLine = services.minOfOrNull { it.hourlyRate }
        ?.takeIf { it > 0 }
        ?.let { "💰 From $${it.toInt()}/hr" }

    val text = buildString {
        appendLine("Check out $name on SmartServe! 🔧")
        appendLine()
        if (serviceLine.isNotBlank()) appendLine("Services: $serviceLine")
        if (ratingLine != null) appendLine(ratingLine)
        if (priceLine != null) appendLine(priceLine)
        appendLine("📍 Serving the Ottawa area")
        appendLine()
        append("Book them on the SmartServe app.")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "$name on SmartServe")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}
