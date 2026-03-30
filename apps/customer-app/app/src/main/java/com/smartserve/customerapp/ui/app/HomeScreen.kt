package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedCard
import com.smartserve.sharedui.SharedEmptyState
import com.smartserve.sharedui.SharedIconButton
import com.smartserve.sharedui.SharedLoading
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextField
import com.smartserve.sharedui.SharedTextVariant

private fun categoryIconForLabel(label: String): ImageVector =
    when (label.trim().lowercase()) {
        "cleaning" -> Icons.Filled.CleaningServices
        "tutoring" -> Icons.Filled.School
        "moving" -> Icons.Filled.LocalShipping
        "lawn care" -> Icons.Filled.LocalFlorist
        "handyman" -> Icons.Filled.Build
        "pet care" -> Icons.Filled.Pets
        "cooking" -> Icons.Filled.Restaurant
        else -> Icons.Filled.Build
    }

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToCategory: (categoryId: String, categoryLabel: String) -> Unit = { _, _ -> },
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToProvider: (providerUid: String, providerName: String) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val greetingName = greetingDisplayName()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
    ) {
        CustomerTabHeader(
            title = "Hello, $greetingName",
            subtitle = "What are you looking for today?",
            trailing = {
                SharedIconButton(
                    onClick = onNavigateToProfile,
                    icon = Icons.Filled.Person,
                    contentDescription = "Profile",
                )
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            SharedTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Search services...",
                readOnly = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onNavigateToSearch,
                    ),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SharedText(text = "Categories", variant = SharedTextVariant.Title)

        Spacer(modifier = Modifier.height(12.dp))

        if (state.isLoading && state.categories.isEmpty()) {
            SharedLoading(modifier = Modifier.fillMaxWidth().height(80.dp))
        } else {
            // Uniform grid: divide available width evenly across columns (max 4 per row)
            androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val columns = 4
                val spacing = 8.dp
                val cardWidth: Dp = (maxWidth - spacing * (columns - 1)) / columns
                val rows = state.categories.chunked(columns)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { rowCategories ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing),
                        ) {
                            rowCategories.forEach { category ->
                                CategoryCard(
                                    label = category.label,
                                    icon = categoryIconForLabel(category.label),
                                    onClick = { onNavigateToCategory(category.id, category.label) },
                                    width = cardWidth,
                                )
                            }
                            // Fill remaining slots in the last row with invisible spacers
                            repeat(columns - rowCategories.size) {
                                Spacer(modifier = Modifier.width(cardWidth))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SharedText(text = "Smart Picks", variant = SharedTextVariant.Title)

        Spacer(modifier = Modifier.height(8.dp))

        if (state.isLoading && state.topProviders.isEmpty()) {
            SharedLoading(modifier = Modifier.fillMaxWidth().height(120.dp))
        } else if (state.topProviders.isEmpty()) {
            SharedEmptyState(
                title = "No providers yet",
                description = "Check back soon for recommendations.",
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            state.topProviders.forEach { provider ->
                SmartPickCard(
                    provider = provider,
                    onNavigateToProvider = onNavigateToProvider,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SmartPickCard(
    provider: CustomerProviderSummary,
    onNavigateToProvider: (providerUid: String, providerName: String) -> Unit,
) {
    SharedCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
        onClick = { onNavigateToProvider(provider.uid, provider.displayName) },
    ) {
        Row(verticalAlignment = Alignment.Top) {
            SharedAvatar(name = provider.displayName, size = 48.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                SharedText(text = provider.displayName, variant = SharedTextVariant.BodyStrong)
                Spacer(modifier = Modifier.height(2.dp))
                if (provider.avgRating > 0) {
                    SharedText(
                        text = "★ ${"%.1f".format(provider.avgRating)}" +
                            if (provider.totalReviews > 0) " · ${provider.totalReviews} reviews" else "",
                        variant = SharedTextVariant.Caption,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    SharedText(
                        text = "New provider",
                        variant = SharedTextVariant.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (provider.serviceDescription.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SharedText(
                        text = provider.serviceDescription.take(80),
                        variant = SharedTextVariant.Body,
                    )
                }
                if (provider.hourlyRate > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SharedText(
                        text = "From $${provider.hourlyRate.toInt()}/hr",
                        variant = SharedTextVariant.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    width: Dp = 80.dp,
) {
    SharedCard(
        onClick = onClick,
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp),
        modifier = Modifier.width(width),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            SharedText(text = label, variant = SharedTextVariant.Caption)
        }
    }
}

private fun greetingDisplayName(): String {
    val user = FirebaseAuth.getInstance().currentUser ?: return "there"
    return user.displayName?.takeIf { it.isNotBlank() }
        ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
        ?: "there"
}
