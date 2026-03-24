package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
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
import com.smartserve.sharedui.SharedEmptyState
import com.smartserve.sharedui.SharedListItem
import com.smartserve.sharedui.SharedTextField
import com.smartserve.sharedui.SharedTextVariant
import com.smartserve.sharedui.SharedText

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    onProviderClick: (providerName: String) -> Unit = {},
) {
    var searchQuery by remember { mutableStateOf("") }

    val results = if (searchQuery.isBlank()) emptyList()
    else allProviders.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true) ||
            it.categories.any { cat -> cat.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SharedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            placeholder = "Search services or providers...",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )

        when {
            searchQuery.isBlank() -> SharedEmptyState(
                title = "Find a service",
                description = "Search by service type, provider name, or category",
                icon = Icons.Filled.Search,
                modifier = Modifier.fillMaxSize(),
            )
            results.isEmpty() -> SharedEmptyState(
                title = "No results",
                description = "Try a different search term",
                icon = Icons.Filled.Search,
                modifier = Modifier.fillMaxSize(),
            )
            else -> {
                SharedText(
                    text = "${results.size} result${if (results.size == 1) "" else "s"}",
                    variant = SharedTextVariant.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                LazyColumn {
                    itemsIndexed(results) { index, provider ->
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
    }
}
