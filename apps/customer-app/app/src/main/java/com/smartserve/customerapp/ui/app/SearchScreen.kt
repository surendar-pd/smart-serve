package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedEmptyState
import com.smartserve.sharedui.SharedListItem
import com.smartserve.sharedui.SharedLoading
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextField
import com.smartserve.sharedui.SharedTextVariant

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    onResultClick: (CustomerServiceListing) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.loadAll() }

    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        CustomerTabHeader(
            title = "Search",
            subtitle = "Find providers and services",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        SharedTextField(
            value = query,
            onValueChange = { viewModel.setQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = "Search services or providers...",
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
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isLoading -> SharedLoading()
                query.isBlank() -> SharedEmptyState(
                    title = "Find a service",
                    description = "Search by service type or provider name",
                    icon = Icons.Filled.Search,
                    modifier = Modifier.fillMaxSize(),
                )
                results.isEmpty() -> SharedEmptyState(
                    title = "No results",
                    description = "Try a different search term",
                    icon = Icons.Filled.Search,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart,
                ) {
                    Column {
                        SharedText(
                            text = "${results.size} result${if (results.size == 1) "" else "s"}",
                            variant = SharedTextVariant.Caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        LazyColumn {
                            itemsIndexed(results) { index, service ->
                                SharedListItem(
                                    title = service.title,
                                    supportingText = "by ${service.providerName}",
                                    leadingAvatar = { SharedAvatar(name = service.providerName, size = 40.dp) },
                                    trailing = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                    showDivider = index > 0,
                                    onClick = { onResultClick(service) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
