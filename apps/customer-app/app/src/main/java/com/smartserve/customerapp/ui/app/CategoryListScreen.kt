package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedChip
import com.smartserve.sharedui.SharedListItem
import com.smartserve.sharedui.SharedTopAppBar

@Composable
fun CategoryListScreen(
    modifier: Modifier = Modifier,
    categoryName: String = "Services",
    onBack: () -> Unit,
    onProviderClick: (providerName: String) -> Unit = {},
) {
    val sortOptions = listOf("Recommended", "Trending", "Rating")
    var selectedSort by remember { mutableStateOf("Recommended") }

    val baseProviders = allProviders.filter { categoryName in it.categories }

    val providers = when (selectedSort) {
        "Trending" -> baseProviders.sortedByDescending {
            Regex("(\\d+)\\+?\\s*jobs").find(it.description)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        }
        "Rating" -> baseProviders.sortedByDescending {
            Regex("(\\d+\\.\\d+)\\s*★").find(it.description)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        }
        else -> baseProviders
    }

    Column(modifier = modifier.fillMaxSize()) {
        SharedTopAppBar(title = categoryName, onBack = onBack)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            sortOptions.forEach { option ->
                SharedChip(
                    label = option,
                    selected = selectedSort == option,
                    onSelectedChange = { if (it) selectedSort = option },
                )
            }
        }

        LazyColumn {
            itemsIndexed(providers) { index, provider ->
                SharedListItem(
                    title = provider.name,
                    supportingText = provider.description,
                    leadingAvatar = { SharedAvatar(name = provider.name, size = 40.dp) },
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    showDivider = index > 0,
                    onClick = { onProviderClick(provider.name) },
                )
            }
        }
    }
}
