package com.smartserve.customerapp.ui.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.smartserve.sharedui.SharedEmptyState

@Composable
fun SearchScreen(modifier: Modifier = Modifier) {
    SharedEmptyState(
        title = "Search",
        description = "Search for services coming soon",
        icon = Icons.Filled.Search,
        modifier = modifier,
    )
}
