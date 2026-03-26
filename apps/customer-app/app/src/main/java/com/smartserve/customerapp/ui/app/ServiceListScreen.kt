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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedCard
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant

@Composable
fun ServiceListScreen(
    providerName: String,
    onBack: () -> Unit,
    onSelectService: (providerName: String, serviceName: String, price: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val provider = allProviders.find { it.name == providerName }
    val services = servicesFor(provider?.categories ?: emptyList())

    Column(modifier = modifier.fillMaxSize()) {
        CustomerStackHeader(
            title = providerName,
            subtitle = "Choose a service",
            onBack = onBack,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

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
                                variant = SharedTextVariant.BodyStrong,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        SharedButton(
                            text = "Add to Cart",
                            onClick = { onSelectService(providerName, service.name, service.price) },
                            modifier = Modifier.height(36.dp),
                        )
                    }
                }
            }
        }
    }
}
