package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedButtonVariant
import com.smartserve.sharedui.SharedCard
import com.smartserve.sharedui.SharedChip
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant
import com.smartserve.sharedui.SharedTopAppBar

@Composable
fun ServiceListScreen(
    providerName: String,
    onBack: () -> Unit,
    onAddToCart: (CartItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val provider = allProviders.find { it.name == providerName }
    val allServices = servicesFor(provider?.categories ?: emptyList())
    val sortOptions = listOf("Recommended", "Price", "Rating")
    var selectedSort by remember { mutableStateOf("Recommended") }

    val services = when (selectedSort) {
        "Price" -> allServices.sortedBy { it.priceValue }
        "Rating" -> allServices.sortedByDescending { it.name.hashCode() and Int.MAX_VALUE }
        else -> allServices
    }

    Column(modifier = modifier.fillMaxSize()) {
        SharedTopAppBar(title = providerName, onBack = onBack)

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

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(services) { service ->
                SharedCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SharedAvatar(name = service.name, size = 48.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            SharedText(text = service.name, variant = SharedTextVariant.BodyStrong)
                            SharedText(text = service.description, variant = SharedTextVariant.Body)
                            SharedText(
                                text = service.price,
                                variant = SharedTextVariant.Caption,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        SharedButton(
                            text = "Add to Cart",
                            onClick = { onAddToCart(CartItem(providerName, service.name, service.price)) },
                            variant = SharedButtonVariant.Default,
                        )
                    }
                }
            }
        }
    }
}
