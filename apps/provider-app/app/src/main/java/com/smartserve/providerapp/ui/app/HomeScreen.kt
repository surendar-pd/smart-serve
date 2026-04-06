package com.smartserve.providerapp.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedEmptyState
import com.smartserve.sharedui.SharedErrorState
import com.smartserve.sharedui.SharedLoading
import com.smartserve.sharedui.SharedSwitchRow
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToRequestDetail: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val firstName = FirebaseAuth.getInstance().currentUser
        ?.displayName?.substringBefore(" ")?.takeIf { it.isNotBlank() } ?: "there"

    val statCards = listOf(
        StatCardUi(
            title = "Bookings Received",
            value = state.totalBookings.toString(),
            icon = Icons.Filled.ReceiptLong,
            accent = Color(0xFF1D4ED8),
        ),
        StatCardUi(
            title = "Services Listed",
            value = state.totalServices.toString(),
            icon = Icons.Filled.BuildCircle,
            accent = Color(0xFF047857),
        ),
        StatCardUi(
            title = "Total Reviews",
            value = state.totalReviews.toString(),
            icon = Icons.Filled.Star,
            accent = Color(0xFFB45309),
        ),
        StatCardUi(
            title = "Total Earning",
            value = "$${String.format(java.util.Locale.US, "%,.2f", state.totalEarningsCad)}",
            icon = Icons.Filled.Payments,
            accent = Color(0xFFBE123C),
        ),
    )

    Column(modifier = modifier.fillMaxSize()) {
        ProviderTabHeader(
            title = "Hello, $firstName",
            subtitle = "Dashboard overview",
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

        Spacer(Modifier.height(16.dp))

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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatsCard(card = statCards[0], modifier = Modifier.weight(1f))
                        StatsCard(card = statCards[1], modifier = Modifier.weight(1f))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatsCard(card = statCards[2], modifier = Modifier.weight(1f))
                        StatsCard(card = statCards[3], modifier = Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(4.dp))

                    SharedText(
                        text = "Monthly Revenue (CAD)",
                        variant = SharedTextVariant.Subtitle,
                        modifier = Modifier.padding(top = 8.dp),
                    )

                    RevenueChart(
                        points = state.monthlyRevenue,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (state.totalBookings == 0) {
                        SharedEmptyState(title = "No bookings yet")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsCard(
    card: StatCardUi,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = card.icon,
                contentDescription = card.title,
                tint = card.accent,
                modifier = Modifier.size(22.dp),
            )

            SharedText(
                text = card.value,
                variant = SharedTextVariant.Title,
                color = MaterialTheme.colorScheme.onSurface,
            )

            SharedText(
                text = card.title,
                variant = SharedTextVariant.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RevenueChart(
    points: List<MonthlyRevenuePoint>,
    modifier: Modifier = Modifier,
) {
    val safePoints = points.takeIf { it.isNotEmpty() } ?: return
    val maxValue = safePoints.maxOf { it.amount }.coerceAtLeast(1.0)
    var selectedIndex by remember { mutableIntStateOf(safePoints.indexOfFirst { it.amount == maxValue }.coerceAtLeast(0)) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val selectedPoint = safePoints.getOrNull(selectedIndex)
            selectedPoint?.let { point ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    SharedText(
                        text = "${point.month}: $${String.format(java.util.Locale.US, "%,.2f", point.amount)}",
                        variant = SharedTextVariant.Caption,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                safePoints.forEachIndexed { index, point ->
                    val ratio = if (maxValue <= 0.0) 0.08f else (point.amount / maxValue).toFloat().coerceIn(0.08f, 1f)
                    val isSelected = selectedIndex == index
                    val animatedRatio by animateFloatAsState(targetValue = ratio, label = "revenueBarRatio")

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedIndex = index },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp * animatedRatio)
                                .padding(horizontal = 2.dp)
                                .background(
                                    if (isSelected) Color(0xFF1D4ED8) else Color(0xFF93C5FD),
                                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                                ),
                        )
                        SharedText(
                            text = point.month,
                            variant = SharedTextVariant.Caption,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            SharedText(
                text = "Tap a bar to view month revenue",
                variant = SharedTextVariant.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class StatCardUi(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val accent: Color,
)

 