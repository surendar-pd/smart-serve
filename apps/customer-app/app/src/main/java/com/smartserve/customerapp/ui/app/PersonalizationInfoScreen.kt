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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartserve.sharedui.SharedBadge
import com.smartserve.sharedui.SharedBadgeVariant
import com.smartserve.sharedui.SharedCard
import com.smartserve.sharedui.SharedLoading
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant
import java.util.Calendar

@Composable
fun PersonalizationInfoScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        CustomerStackHeader(
            title = "How My Feed Works",
            subtitle = "Context signals that shape your home screen",
            onBack = onBack,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

        if (state.isLoading || state.activeSignals == null) {
            SharedLoading(modifier = Modifier.fillMaxSize())
            return@Column
        }

        val signals = state.activeSignals!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {

            // ── Live context banner ───────────────────────────────────────────
            SharedCard(
                contentPadding = PaddingValues(16.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SharedText(
                            text = "Your Current Context",
                            variant = SharedTextVariant.BodyStrong,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SharedBadge(
                            text = timeContextLabel(signals.timeContext),
                            variant = SharedBadgeVariant.Info,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        )
                        SharedBadge(
                            text = seasonLabel(signals.seasonContext),
                            variant = SharedBadgeVariant.Success,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        )
                        if (signals.isWeekend) {
                            SharedBadge(
                                text = "Weekend",
                                variant = SharedBadgeVariant.Warning,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                    SharedText(
                        text = "Device time: ${formatHour(signals.hourOfDay)}  ·  ${monthName(signals.month)}",
                        variant = SharedTextVariant.Caption,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            // ── Personal signals ──────────────────────────────────────────────
            SharedCard(
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SharedText(text = "Your Behaviour", variant = SharedTextVariant.BodyStrong)
                    }
                    SignalRow(
                        icon = Icons.Filled.TouchApp,
                        label = "Category taps recorded",
                        value = "${signals.personalTapCount} total",
                    )
                    SignalRow(
                        icon = Icons.Filled.History,
                        label = "Providers booked before",
                        value = "${signals.bookedProviderCount} found",
                    )
                    SharedText(
                        text = "Categories you tap more often score higher. Providers you've used before appear first in Smart Picks.",
                        variant = SharedTextVariant.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Scoring formula ───────────────────────────────────────────────
            SectionCard(
                icon = Icons.Filled.Star,
                title = "Ranking Formula",
            ) {
                SharedText(
                    text = "Each category receives a total score that determines its position:",
                    variant = SharedTextVariant.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FormulaRow(label = "Tap score", formula = "taps × 10 pts")
                FormulaRow(label = "Time-of-day bonus", formula = "0 – 30 pts")
                FormulaRow(label = "Season bonus", formula = "0 – 30 pts")
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                FormulaRow(label = "Total", formula = "sum → rank order", bold = true)
                Spacer(modifier = Modifier.height(4.dp))
                SharedText(
                    text = "The top-ranked category gets the \"Top Pick\" highlight. Frequently tapped categories (≥ 3 taps) keep their \"Your Fave\" badge regardless of time or season.",
                    variant = SharedTextVariant.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── Time-of-day rules ─────────────────────────────────────────────
            SectionCard(
                icon = Icons.Filled.AccessTime,
                title = "Time-of-Day Rules",
            ) {
                SharedText(
                    text = "Detected from your device clock. Resets every time you open the app.",
                    variant = SharedTextVariant.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(10.dp))
                TimeRuleRow(
                    period = "Morning  05:00 – 11:59",
                    rules = listOf("Tutoring +30", "Cleaning +20", "Cooking +15"),
                    isActive = state.timeContext == TimeContext.MORNING,
                )
                TimeRuleRow(
                    period = "Afternoon  12:00 – 16:59",
                    rules = listOf("Handyman +25", "Lawn Care +25", "Moving +20", "Tutoring +15"),
                    isActive = state.timeContext == TimeContext.AFTERNOON,
                )
                TimeRuleRow(
                    period = "Evening  17:00 – 21:59",
                    rules = listOf("Cooking +30", "Cleaning +20", "Pet Care +15"),
                    isActive = state.timeContext == TimeContext.EVENING,
                )
                TimeRuleRow(
                    period = "Night  22:00 – 04:59",
                    rules = listOf("Cleaning +15"),
                    isActive = state.timeContext == TimeContext.NIGHT,
                )
                TimeRuleRow(
                    period = "Weekend  (Sat & Sun)",
                    rules = listOf("Cleaning +30", "Lawn Care +28", "Moving +25", "Handyman +20", "Cooking +20", "Pet Care +15"),
                    isActive = state.timeContext == TimeContext.WEEKEND,
                    last = true,
                )
            }

            // ── Season rules ──────────────────────────────────────────────────
            SectionCard(
                icon = Icons.Filled.CalendarMonth,
                title = "Seasonal Rules",
            ) {
                SharedText(
                    text = "Detected from your device calendar month. Reflects realistic Ottawa service demand patterns.",
                    variant = SharedTextVariant.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(10.dp))
                TimeRuleRow(
                    period = "Winter  Dec – Feb",
                    rules = listOf("Handyman +25", "Cleaning +20", "Tutoring +15", "Cooking +10"),
                    isActive = state.seasonContext == SeasonContext.WINTER,
                )
                TimeRuleRow(
                    period = "Spring  Mar – May",
                    rules = listOf("Cleaning +30", "Lawn Care +28", "Handyman +15", "Moving +15"),
                    isActive = state.seasonContext == SeasonContext.SPRING,
                )
                TimeRuleRow(
                    period = "Summer  Jun – Aug",
                    rules = listOf("Lawn Care +30", "Moving +25", "Cooking +15", "Pet Care +15"),
                    isActive = state.seasonContext == SeasonContext.SUMMER,
                )
                TimeRuleRow(
                    period = "Fall  Sep – Nov",
                    rules = listOf("Lawn Care +25", "Handyman +25", "Cleaning +20", "Moving +15"),
                    isActive = state.seasonContext == SeasonContext.FALL,
                    last = true,
                )
            }

            // ── Smart Picks ───────────────────────────────────────────────────
            SectionCard(
                icon = Icons.Filled.Star,
                title = "Smart Picks",
            ) {
                SharedText(
                    text = "Providers shown in the Smart Picks row are ranked by:",
                    variant = SharedTextVariant.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FormulaRow(label = "1st priority", formula = "Previously booked ✓")
                FormulaRow(label = "2nd priority", formula = "Highest avg rating")
                FormulaRow(label = "3rd priority", formula = "Most total reviews")
                Spacer(modifier = Modifier.height(4.dp))
                SharedText(
                    text = "Up to 5 providers are shown. Providers you've booked before always appear at the top with a \"Booked before\" badge.",
                    variant = SharedTextVariant.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ── Private composables ───────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit,
) {
    SharedCard(
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 10.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                SharedText(text = title, variant = SharedTextVariant.BodyStrong)
            }
            content()
        }
    }
}

@Composable
private fun SignalRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            SharedText(text = label, variant = SharedTextVariant.Caption)
        }
        SharedText(
            text = value,
            variant = SharedTextVariant.Caption,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun FormulaRow(label: String, formula: String, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SharedText(
            text = label,
            variant = if (bold) SharedTextVariant.BodyStrong else SharedTextVariant.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SharedText(
            text = formula,
            variant = if (bold) SharedTextVariant.BodyStrong else SharedTextVariant.Caption,
            color = if (bold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TimeRuleRow(
    period: String,
    rules: List<String>,
    isActive: Boolean,
    last: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                SharedText(
                    text = period,
                    variant = SharedTextVariant.Caption,
                    color = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isActive) {
                    Spacer(modifier = Modifier.width(6.dp))
                    SharedBadge(
                        text = "NOW",
                        variant = SharedBadgeVariant.Info,
                        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 1.dp),
                        textStyle = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                rules.forEach { rule ->
                    SharedText(
                        text = rule,
                        variant = SharedTextVariant.Caption,
                        color = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        if (!last) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

// ── Label helpers ─────────────────────────────────────────────────────────────

private fun timeContextLabel(ctx: TimeContext): String = when (ctx) {
    TimeContext.MORNING   -> "Morning"
    TimeContext.AFTERNOON -> "Afternoon"
    TimeContext.EVENING   -> "Evening"
    TimeContext.NIGHT     -> "Night"
    TimeContext.WEEKEND   -> "Weekend"
}

private fun seasonLabel(ctx: SeasonContext): String = when (ctx) {
    SeasonContext.WINTER -> "Winter"
    SeasonContext.SPRING -> "Spring"
    SeasonContext.SUMMER -> "Summer"
    SeasonContext.FALL   -> "Fall"
}

private fun formatHour(hour: Int): String {
    val suffix = if (hour < 12) "AM" else "PM"
    val h = when {
        hour == 0  -> 12
        hour > 12  -> hour - 12
        else       -> hour
    }
    return "$h:00 $suffix"
}

private fun monthName(month: Int): String = when (month) {
    Calendar.JANUARY   -> "January"
    Calendar.FEBRUARY  -> "February"
    Calendar.MARCH     -> "March"
    Calendar.APRIL     -> "April"
    Calendar.MAY       -> "May"
    Calendar.JUNE      -> "June"
    Calendar.JULY      -> "July"
    Calendar.AUGUST    -> "August"
    Calendar.SEPTEMBER -> "September"
    Calendar.OCTOBER   -> "October"
    Calendar.NOVEMBER  -> "November"
    else               -> "December"
}
