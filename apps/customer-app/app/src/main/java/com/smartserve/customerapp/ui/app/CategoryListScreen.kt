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
import com.smartserve.sharedui.SharedListItem
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextField
import com.smartserve.sharedui.SharedTextVariant
import com.smartserve.sharedui.SharedTopAppBar

private data class ProviderItem(val name: String, val description: String)

private val mockProviders = listOf(
    ProviderItem("Sarah M.", "Home cleaning · 4.9 ★ · 120+ jobs"),
    ProviderItem("David K.", "Eco-friendly, background checked"),
    ProviderItem("Priya N.", "Deep cleaning specialist"),
    ProviderItem("Tom H.", "Move-out & post-reno cleaning"),
    ProviderItem("Aisha R.", "Weekly & bi-weekly packages"),
    ProviderItem("Carlos J.", "Same-day availability"),
)

@Composable
fun CategoryListScreen(
    modifier: Modifier = Modifier,
    categoryName: String = "Services",
    onBack: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = if (searchQuery.isBlank()) mockProviders
    else mockProviders.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = modifier.fillMaxSize()) {
        SharedTopAppBar(
            title = categoryName,
            onBack = onBack,
        )

        SharedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = "Search providers...",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )

        SharedText(
            text = "Available Providers",
            variant = SharedTextVariant.Title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        LazyColumn {
            itemsIndexed(filtered) { index, provider ->
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
                    onClick = {},
                )
            }
        }
    }
}
