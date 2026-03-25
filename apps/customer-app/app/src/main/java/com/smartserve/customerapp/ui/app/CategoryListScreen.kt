package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedListItem
import com.smartserve.sharedui.SharedTopAppBar

@Composable
fun CategoryListScreen(
    modifier: Modifier = Modifier,
    categoryName: String = "Services",
    onBack: () -> Unit,
    onProviderClick: (providerName: String) -> Unit = {},
) {
    val providers = allProviders.filter { categoryName in it.categories }

    Column(modifier = modifier.fillMaxSize()) {
        SharedTopAppBar(title = categoryName, onBack = onBack)

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
