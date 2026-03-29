package com.smartserve.providerapp.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedButtonVariant
import com.smartserve.sharedui.SharedChip
import com.smartserve.sharedui.SharedConfirmDialog
import com.smartserve.sharedui.SharedDropdown
import com.smartserve.sharedui.SharedInputIcon
import com.smartserve.sharedui.SharedLoading
import com.smartserve.sharedui.SharedProgress
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextArea
import com.smartserve.sharedui.SharedTextField
import com.smartserve.sharedui.SharedTextVariant
import com.smartserve.sharedui.SharedSwitchRow

@Composable
fun ServiceEditorScreen(
    serviceId: String?,
    sessionKey: Long,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onFinished: () -> Unit,
    viewModel: ServiceEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val categoryLabels = remember(state.categories) {
        state.categories.map { it.label }
    }
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    var categoryExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(serviceId, sessionKey) {
        viewModel.load(serviceId)
    }

    LaunchedEffect(state.savedOk) {
        if (state.savedOk) {
            viewModel.consumeSaved()
            onFinished()
        }
    }

    LaunchedEffect(state.deletedOk) {
        if (state.deletedOk) {
            viewModel.consumeDeleted()
            onFinished()
        }
    }

    val titleBar = if (serviceId == null) "New service" else "Edit service"
    val subtitleBar =
        if (serviceId == null) "Add a new listing" else "Update this listing"
    val navOk = !state.saving && !state.deleting

    Column(modifier = modifier.fillMaxSize()) {
        ProviderStackHeader(
            title = titleBar,
            subtitle = subtitleBar,
            onBack = onBack,
            backEnabled = navOk,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

        if (state.loading) {
            SharedLoading(
                modifier = Modifier.weight(1f),
                text = "Loading…",
            )
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            state.errorMessage?.let { msg ->
                SharedText(
                    text = msg,
                    variant = SharedTextVariant.Body,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            SharedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = "Title *",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(12.dp))

            SharedText(
                text = "Category *",
                variant = SharedTextVariant.Label,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 4.dp),
            )

            if (state.categoriesLoading) {
                SharedProgress(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                SharedText(
                    text = "Loading categories…",
                    variant = SharedTextVariant.Caption,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            } else if (categoryLabels.isEmpty()) {
                SharedText(
                    text = "No categories in Firestore yet. Add categories in Firebase or complete provider onboarding first.",
                    variant = SharedTextVariant.Body,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            } else {
                SharedDropdown(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    options = categoryLabels,
                    selectedOption = state.categories.find { it.id == state.categoryId }?.label,
                    onOptionSelected = { label ->
                        state.categories.find { it.label == label }
                            ?.let { viewModel.onCategoryChange(it.id) }
                    },
                    label = "Select category",
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            SharedTextArea(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = "Description",
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(12.dp))

            SharedTextField(
                value = state.hourlyRate,
                onValueChange = viewModel::onHourlyRateChange,
                label = "Hourly rate (CAD) *",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { SharedInputIcon(Icons.Filled.AttachMoney, contentDescription = null) },
            )

            Spacer(Modifier.height(12.dp))

            SharedText(
                text = "Availability *",
                variant = SharedTextVariant.Label,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 4.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val rows = daysOfWeek.chunked(4)
                rows.forEach { rowDays ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        rowDays.forEach { day ->
                            val selected = day in state.availabilityDays
                            SharedChip(
                                label = day,
                                selected = selected,
                                onSelectedChange = { newSelected ->
                                    val updated = when {
                                        newSelected && day !in state.availabilityDays ->
                                            state.availabilityDays + day
                                        !newSelected -> state.availabilityDays - day
                                        else -> state.availabilityDays
                                    }
                                    viewModel.onAvailabilityDaysChange(updated)
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SharedTextField(
                    label = "From",
                    value = state.availabilityStart,
                    onValueChange = viewModel::onAvailabilityStartChange,
                    modifier = Modifier.weight(1f),
                    placeholder = "HH:mm",
                )
                SharedTextField(
                    label = "To",
                    value = state.availabilityEnd,
                    onValueChange = viewModel::onAvailabilityEndChange,
                    modifier = Modifier.weight(1f),
                    placeholder = "HH:mm",
                )
            }

            Spacer(Modifier.height(8.dp))

            SharedText(
                text = "Service area (map and radius) is set in your provider profile.",
                variant = SharedTextVariant.Caption,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            Spacer(Modifier.height(8.dp))

            SharedSwitchRow(
                title = "Listing active",
                checked = state.isActive,
                onCheckedChange = viewModel::onActiveChange,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(24.dp))

            SharedButton(
                text = "Save",
                onClick = { viewModel.save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                loading = state.saving,
                enabled = !state.deleting,
            )

            if (serviceId != null) {
                Spacer(Modifier.height(12.dp))
                SharedButton(
                    text = "Delete listing",
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    variant = SharedButtonVariant.Destructive,
                    enabled = !state.saving && !state.deleting,
                    loading = state.deleting,
                )
            }
        }
    }

    SharedConfirmDialog(
        title = "Delete this service?",
        message = "Customers will no longer see this listing. This cannot be undone.",
        isOpen = showDeleteConfirm,
        onDismiss = { showDeleteConfirm = false },
        confirmText = "Delete",
        destructive = true,
        onConfirm = {
            showDeleteConfirm = false
            viewModel.delete()
        },
    )
}
