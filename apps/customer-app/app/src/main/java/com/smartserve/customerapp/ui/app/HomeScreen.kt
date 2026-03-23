package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedButtonVariant
import com.smartserve.sharedui.SharedCard
import com.smartserve.sharedui.SharedListItem
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextField
import com.smartserve.sharedui.SharedTextVariant

private data class Category(val label: String, val icon: ImageVector)

private val categories = listOf(
    Category("Cleaning", Icons.Filled.CleaningServices),
    Category("Tutoring", Icons.Filled.School),
    Category("Moving", Icons.Filled.LocalShipping),
)

private data class SmartPick(val name: String, val subtitle: String, val isRebook: Boolean)

private val smartPicks = listOf(
    SmartPick("Maria G.", "Home Cleaning · Last booked Jan", isRebook = true),
    SmartPick("James T.", "Math Tutoring · Last booked Feb", isRebook = true),
    SmartPick("Quick Move Co.", "Moving & Packing", isRebook = false),
    SmartPick("SparkleClean", "Deep Cleaning Service", isRebook = false),
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToCategory: () -> Unit = {},
) {
    val greetingName = greetingDisplayName()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        SharedText(
            text = "Hello, $greetingName",
            variant = SharedTextVariant.Title,
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(24.dp))

        SharedText(text = "Categories", variant = SharedTextVariant.Title)

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            categories.forEach { category ->
                CategoryCard(
                    label = category.label,
                    icon = category.icon,
                    onClick = onNavigateToCategory,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SharedText(text = "Smart Picks", variant = SharedTextVariant.Title)

        Spacer(modifier = Modifier.height(8.dp))

        smartPicks.forEachIndexed { index, pick ->
            SharedListItem(
                title = pick.name,
                supportingText = pick.subtitle,
                leadingAvatar = { SharedAvatar(name = pick.name, size = 40.dp) },
                trailing = {
                    SharedButton(
                        text = if (pick.isRebook) "Rebook" else "Book",
                        onClick = {},
                        variant = if (pick.isRebook) SharedButtonVariant.Outline else SharedButtonVariant.Default,
                    )
                },
                showDivider = index > 0,
            )
        }
    }
}

@Composable
private fun CategoryCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    SharedCard(
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        modifier = Modifier.width(100.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(32.dp),
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
