package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.smartserve.sharedui.SharedBadge
import com.smartserve.sharedui.SharedBadgeVariant
import com.smartserve.sharedui.SharedCard
import com.smartserve.sharedui.SharedIconButton
import com.smartserve.sharedui.SharedLoading
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextField
import com.smartserve.sharedui.SharedTextVariant

private fun categoryIconForLabel(label: String): ImageVector =
    when (label.trim().lowercase()) {
        "cleaning"  -> Icons.Filled.CleaningServices
        "tutoring"  -> Icons.Filled.School
        "moving"    -> Icons.Filled.LocalShipping
        "lawn care" -> Icons.Filled.LocalFlorist
        "handyman"  -> Icons.Filled.Build
        "pet care"  -> Icons.Filled.Pets
        "cooking"   -> Icons.Filled.Restaurant
        else        -> Icons.Filled.Build
    }

private fun badgeLabel(badge: CategoryBadge): String = when (badge) {
    CategoryBadge.TOP_PICK        -> "Top Pick"
    CategoryBadge.FREQUENTLY_USED -> "Your Fave"
    CategoryBadge.MORNING_PICK    -> "Morning Pick"
    CategoryBadge.AFTERNOON_PICK  -> "Afternoon Pick"
    CategoryBadge.EVENING_PICK    -> "Evening Pick"
    CategoryBadge.WEEKEND_PICK    -> "Weekend Pick"
    CategoryBadge.POPULAR         -> "Popular"
}

private fun badgeVariant(badge: CategoryBadge): SharedBadgeVariant = when (badge) {
    CategoryBadge.TOP_PICK        -> SharedBadgeVariant.Warning
    CategoryBadge.FREQUENTLY_USED -> SharedBadgeVariant.Success
    CategoryBadge.MORNING_PICK    -> SharedBadgeVariant.Info
    CategoryBadge.AFTERNOON_PICK  -> SharedBadgeVariant.Info
    CategoryBadge.EVENING_PICK    -> SharedBadgeVariant.Info
    CategoryBadge.WEEKEND_PICK    -> SharedBadgeVariant.Success
    CategoryBadge.POPULAR         -> SharedBadgeVariant.Neutral
}

private fun greetingText(timeContext: TimeContext): Pair<String, String> {
    val name = greetingDisplayName()
    return when (timeContext) {
        TimeContext.MORNING   -> "Good morning, $name" to "Ready to get things done today?"
        TimeContext.AFTERNOON -> "Good afternoon, $name" to "What can we help with today?"
        TimeContext.EVENING   -> "Good evening, $name" to "Let's take care of something tonight."
        TimeContext.NIGHT     -> "Hi, $name" to "Looking for help? We've got you covered."
        TimeContext.WEEKEND   -> "Happy weekend, $name" to "A great day to get things sorted!"
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    val (greetingTitle, greetingSubtitle) = greetingText(state.timeContext)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
    ) {
        CustomerTabHeader(
            title = greetingTitle,
            subtitle = greetingSubtitle,
            trailing = {
                SharedIconButton(
                    onClick = onNavigateToProfile,
                    icon = Icons.Filled.Person,
                    contentDescription = "Profile",
                )
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tappable search bar that jumps to the Search tab
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

        if (state.isLoading) {
            SharedLoading(modifier = Modifier.fillMaxWidth().height(80.dp))
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3,
            ) {
                state.scoredCategories.forEach { scored ->
                    ScoredCategoryCard(
                        scored = scored,
                        onClick = {
                            viewModel.onCategoryTapped(scored.category.id)
                            onNavigateToCategory(scored.category.id, scored.category.label)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── Smart Picks ───────────────────────────────────────────────────
            if (state.smartPicks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(28.dp))

                SharedText(text = "Smart Picks", variant = SharedTextVariant.Title)

                Spacer(modifier = Modifier.height(4.dp))

                SharedText(
                    text = "Providers chosen for you",
                    variant = SharedTextVariant.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(end = 8.dp),
                ) {
                    items(state.smartPicks, key = { it.uid }) { pick ->
                        SmartPickCard(
                            pick = pick,
                            onClick = { onNavigateToProvider(pick.uid, pick.name) },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Scored category card ───────────────────────────────────────────────────────

@Composable
private fun ScoredCategoryCard(
    scored: ScoredCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val cardColors = when {
        scored.badge == CategoryBadge.TOP_PICK ->
            CardDefaults.cardColors(containerColor = colorScheme.primaryContainer)
        scored.badge != null && scored.badge != CategoryBadge.POPULAR ->
            CardDefaults.cardColors(containerColor = colorScheme.secondaryContainer)
        else ->
            CardDefaults.cardColors()
    }
    val iconTint = when {
        scored.badge == CategoryBadge.TOP_PICK    -> colorScheme.onPrimaryContainer
        scored.badge != null                       -> colorScheme.onSecondaryContainer
        else                                       -> colorScheme.primary
    }

    Box(modifier = modifier.height(110.dp)) {
        SharedCard(
            onClick = onClick,
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 8.dp),
            modifier = Modifier.fillMaxSize(),
            colors = cardColors,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = categoryIconForLabel(scored.category.label),
                    contentDescription = scored.category.label,
                    modifier = Modifier.size(32.dp),
                    tint = iconTint,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = scored.category.label,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Badge overlay at top-end
        if (scored.badge != null) {
            SharedBadge(
                text = badgeLabel(scored.badge),
                variant = badgeVariant(scored.badge),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                textStyle = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .wrapContentSize(),
            )
        }
    }
}

// ── Smart pick card ────────────────────────────────────────────────────────────

@Composable
private fun SmartPickCard(
    pick: SmartPickProvider,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.width(160.dp)) {
        SharedCard(
            onClick = onClick,
            contentPadding = PaddingValues(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = pick.name.ifBlank { "Provider" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (pick.primaryServiceTitle.isNotBlank()) {
                    Text(
                        text = pick.primaryServiceTitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (pick.avgRating > 0 && pick.totalReviews > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        SharedText(
                            text = "${"%.1f".format(pick.avgRating)} (${pick.totalReviews})",
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
            }
        }

        // "Booked before" badge — bottom-end so it never overlaps the provider name
        if (pick.bookedBefore) {
            SharedBadge(
                text = "Booked before",
                variant = SharedBadgeVariant.Success,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                textStyle = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .wrapContentSize(),
            )
        }
    }
}

private fun greetingDisplayName(): String {
    val user = FirebaseAuth.getInstance().currentUser ?: return "there"
    return user.displayName?.takeIf { it.isNotBlank() }
        ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
        ?: "there"
}
