package com.smartserve.providerapp.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedEmptyState
import com.smartserve.sharedui.SharedListItem

@Composable
fun MyServicesScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onAddService: () -> Unit,
    onOpenService: (String) -> Unit,
    viewModel: MyServicesViewModel = hiltViewModel(),
) {
    val services by viewModel.services.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        ProviderStackHeader(
            title = "My services",
            subtitle = "Tap a row to edit or delete. Add listings with the button below.",
            onBack = onBack,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

        if (services.isEmpty()) {
            SharedEmptyState(
                title = "No services yet",
                description = "Add a listing so customers can book you.",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(services, key = { it.id }) { row ->
                    SharedListItem(
                        title = row.title,
                        supportingText = buildString {
                            append("$${String.format("%.2f", row.hourlyRate)}/hr")
                            if (!row.isActive) append(" · Inactive")
                        },
                        onClick = { onOpenService(row.id) },
                        showDivider = true,
                    )
                }
            }
        }

        SharedButton(
            text = "Add service",
            onClick = onAddService,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            leadingIcon = Icons.Filled.Add,
        )
    }
}
