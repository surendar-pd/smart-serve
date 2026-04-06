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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartserve.sharedui.SharedBadge
import com.smartserve.sharedui.SharedBadgeVariant
import com.smartserve.sharedui.SharedCard
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant

private data class FeatureItem(
    val title: String,
    val description: String,
    val badge: String,
    val badgeVariant: SharedBadgeVariant,
)

private data class FeatureSection(
    val heading: String,
    val items: List<FeatureItem>,
)

private val featureSections = listOf(
    FeatureSection(
        heading = "AI & Smart Features",
        items = listOf(
            FeatureItem(
                title = "AI-Powered Recommendations",
                description = "Personalized service suggestions based on your past bookings and preferences using machine learning.",
                badge = "Planned",
                badgeVariant = SharedBadgeVariant.Info,
            ),
            FeatureItem(
                title = "Smart Scheduling Assistant",
                description = "Automatically find the best time slot based on your calendar and provider availability.",
                badge = "Not in scope for project",
                badgeVariant = SharedBadgeVariant.Neutral,
            ),
            FeatureItem(
                title = "Price Prediction",
                description = "Estimate costs ahead of time using historical pricing data and demand patterns.",
                badge = "Not in scope for project",
                badgeVariant = SharedBadgeVariant.Neutral,
            ),
        ),
    ),
    FeatureSection(
        heading = "Payments & Trust",
        items = listOf(
            FeatureItem(
                title = "In-App Payments",
                description = "Securely pay for services directly through the app using cards or digital wallets.",
                badge = "Planned",
                badgeVariant = SharedBadgeVariant.Success,
            ),
            FeatureItem(
                title = "Provider Verification Badges",
                description = "Background-checked and identity-verified provider badges for added trust.",
                badge = "Planned",
                badgeVariant = SharedBadgeVariant.Success,
            ),
            FeatureItem(
                title = "Escrow-Based Payments",
                description = "Funds held securely and released only after service completion and customer confirmation.",
                badge = "Not in scope for project",
                badgeVariant = SharedBadgeVariant.Neutral,
            ),
        ),
    ),
    FeatureSection(
        heading = "Booking Experience",
        items = listOf(
            FeatureItem(
                title = "Real-Time Provider Tracking",
                description = "Track your provider's location on a live map as they make their way to you.",
                badge = "Planned",
                badgeVariant = SharedBadgeVariant.Warning,
            ),
            FeatureItem(
                title = "Recurring Bookings",
                description = "Set up weekly or monthly repeat bookings for regular services like cleaning or tutoring.",
                badge = "Planned",
                badgeVariant = SharedBadgeVariant.Warning,
            ),
            FeatureItem(
                title = "Multi-Provider Bundling",
                description = "Book multiple services across providers in a single checkout flow.",
                badge = "Not in scope for project",
                badgeVariant = SharedBadgeVariant.Neutral,
            ),
        ),
    ),
    FeatureSection(
        heading = "Provider Tools",
        items = listOf(
            FeatureItem(
                title = "Provider Analytics Dashboard",
                description = "Providers can see earnings, booking trends, and customer satisfaction scores over time.",
                badge = "Planned",
                badgeVariant = SharedBadgeVariant.Success,
            ),
            FeatureItem(
                title = "Team & Sub-Provider Support",
                description = "Providers can add team members and delegate bookings to assistants.",
                badge = "Not in scope for project",
                badgeVariant = SharedBadgeVariant.Neutral,
            ),
        ),
    ),
    FeatureSection(
        heading = "Platform Expansion",
        items = listOf(
            FeatureItem(
                title = "iOS App",
                description = "Native iOS version for iPhone and iPad users.",
                badge = "Not in scope for project",
                badgeVariant = SharedBadgeVariant.Neutral,
            ),
            FeatureItem(
                title = "Web Portal",
                description = "Full-featured browser interface for customers who prefer desktop booking.",
                badge = "Not in scope for project",
                badgeVariant = SharedBadgeVariant.Neutral,
            ),
            FeatureItem(
                title = "Expansion Beyond Ottawa",
                description = "Rollout to other Canadian cities starting with Toronto and Vancouver.",
                badge = "Not in scope for project",
                badgeVariant = SharedBadgeVariant.Neutral,
            ),
        ),
    ),
)

@Composable
fun ComingSoonScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        CustomerStackHeader(
            title = "What's Coming",
            subtitle = "Future features & roadmap",
            onBack = onBack,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Intro card
            SharedCard(
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column {
                        SharedText(
                            text = "SmartServe Roadmap",
                            variant = SharedTextVariant.BodyStrong,
                        )
                        SharedText(
                            text = "Here's a transparent look at what we're building next and what's beyond our current project scope.",
                            variant = SharedTextVariant.Caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Feature sections
            featureSections.forEach { section ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SharedText(
                        text = section.heading,
                        variant = SharedTextVariant.Subtitle,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    section.items.forEach { item ->
                        FeatureCard(item = item)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FeatureCard(item: FeatureItem) {
    SharedCard(
        contentPadding = PaddingValues(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                SharedText(
                    text = item.title,
                    variant = SharedTextVariant.BodyStrong,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.padding(start = 8.dp))
                SharedBadge(
                    text = item.badge,
                    variant = item.badgeVariant,
                )
            }
            SharedText(
                text = item.description,
                variant = SharedTextVariant.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
