package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedButtonVariant
import com.smartserve.sharedui.SharedCard
import com.smartserve.sharedui.SharedIconButton
import com.smartserve.sharedui.SharedScreenHeader
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextField
import com.smartserve.sharedui.SharedTextVariant
import kotlinx.coroutines.tasks.await

private data class Category(val label: String, val icon: ImageVector)

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

private data class SmartPick(val name: String, val subtitle: String, val isRebook: Boolean)

private val smartPicks = listOf(
    SmartPick("Sarah M.", "Home Cleaning · Last booked Jan", isRebook = true),
    SmartPick("James T.", "Math Tutoring · Last booked Feb", isRebook = true),
    SmartPick("Sam R.", "Moving & Packing", isRebook = false),
    SmartPick("Priya N.", "Deep Cleaning Specialist", isRebook = false),
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToCategory: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToProvider: (String) -> Unit = {},
) {
    val greetingName = greetingDisplayName()

    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }

    LaunchedEffect(Unit) {
        runCatching {
            FirebaseFirestore.getInstance()
                .collection("categories")
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("label")?.trim()?.takeIf { s -> s.isNotBlank() } }
                .distinct()
                .sortedBy { it.lowercase() }
                .map { label -> Category(label = label, icon = categoryIconForLabel(label)) }
        }.onSuccess { categories = it }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SharedScreenHeader(
                title = "Hello, $greetingName",
                subtitle = "What are you looking for today?",
                modifier = Modifier.weight(1f),
            )
            SharedIconButton(
                onClick = onNavigateToProfile,
                icon = Icons.Filled.Person,
                contentDescription = "Profile",
            )
        }

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
                    onClick = { onNavigateToCategory(category.label) },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SharedText(text = "Smart Picks", variant = SharedTextVariant.Title)

        Spacer(modifier = Modifier.height(8.dp))

        smartPicks.forEachIndexed { index, pick ->
            if (index > 0) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            SmartPickRow(pick = pick, onNavigateToProvider = onNavigateToProvider)
        }
    }
}

@Composable
private fun SmartPickRow(
    pick: SmartPick,
    onNavigateToProvider: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SharedAvatar(name = pick.name, size = 44.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            SharedText(text = pick.name, variant = SharedTextVariant.BodyStrong)
            SharedText(text = pick.subtitle, variant = SharedTextVariant.Body)
        }
        Spacer(modifier = Modifier.width(8.dp))
        SharedButton(
            text = if (pick.isRebook) "Rebook" else "Book",
            onClick = { onNavigateToProvider(pick.name) },
            variant = if (pick.isRebook) SharedButtonVariant.Outline else SharedButtonVariant.Default,
            modifier = Modifier.height(36.dp),
        )
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
        contentPadding = PaddingValues(12.dp),
        modifier = Modifier.width(90.dp),
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
