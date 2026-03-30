package com.smartserve.providerapp.ui.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
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
import com.smartserve.sharedui.SharedTimePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class ServiceEditorAvailabilityField { Start, End }

private fun formatTime12(hour24: Int, minute: Int): String {
    val period = if (hour24 < 12) "AM" else "PM"
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return "$hour12:${minute.toString().padStart(2, '0')} $period"
}

private fun parseTimeForPicker(value: String, defaultHour: Int, defaultMinute: Int = 0): Pair<Int, Int> {
    val text = value.trim()
    val patterns = listOf("h:mm a", "hh:mm a", "H:mm", "HH:mm")
    for (pattern in patterns) {
        runCatching {
            val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
            val parsed = sdf.parse(text) ?: return@runCatching null
            val cal = Calendar.getInstance().apply { time = parsed }
            cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
        }.getOrNull()?.let { return it }
    }
    return defaultHour to defaultMinute
}

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
    val context = androidx.compose.ui.platform.LocalContext.current
    val categoryLabels = remember(state.categories) {
        state.categories.map { it.label }
    }
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    var categoryExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var activeAvailabilityField by remember { mutableStateOf<ServiceEditorAvailabilityField?>(null) }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris -> viewModel.onPhotoPicked(uris) },
    )
    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                photoPicker.launch("image/*")
            } else {
                viewModel.setError("Photo permission denied. Enable media permission to upload service images.")
            }
        },
    )

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
                SharedButton(
                    text = if (state.availabilityStart.isBlank()) "From" else state.availabilityStart,
                    onClick = { activeAvailabilityField = ServiceEditorAvailabilityField.Start },
                    modifier = Modifier.weight(1f),
                    variant = SharedButtonVariant.Outline,
                )
                SharedButton(
                    text = if (state.availabilityEnd.isBlank()) "To" else state.availabilityEnd,
                    onClick = { activeAvailabilityField = ServiceEditorAvailabilityField.End },
                    modifier = Modifier.weight(1f),
                    variant = SharedButtonVariant.Outline,
                )
            }

            Spacer(Modifier.height(8.dp))

            SharedText(
                text = "Service area (map and radius) is set in your provider profile.",
                variant = SharedTextVariant.Caption,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            Spacer(Modifier.height(8.dp))

            SharedText(
                text = "Service Images",
                variant = SharedTextVariant.Label,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.existingPhotoUrls) { url ->
                    Box(modifier = Modifier.width(144.dp).height(96.dp)) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Service image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        SharedButton(
                            text = "X",
                            onClick = { viewModel.onRemoveExistingPhoto(url) },
                            modifier = Modifier.padding(6.dp),
                            variant = SharedButtonVariant.Ghost,
                        )
                    }
                }
                items(state.pendingPhotoUris) { uri ->
                    Box(modifier = Modifier.width(144.dp).height(96.dp)) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Pending service image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        SharedButton(
                            text = "X",
                            onClick = { viewModel.onRemovePendingPhoto(uri) },
                            modifier = Modifier.padding(6.dp),
                            variant = SharedButtonVariant.Ghost,
                        )
                    }
                }
            }

            SharedButton(
                text = "Upload Images",
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(context, mediaPermission) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        photoPicker.launch("image/*")
                    } else {
                        permissionLauncher.launch(mediaPermission)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                variant = SharedButtonVariant.Outline,
                enabled = !state.saving,
            )

            if (state.saving && state.uploadProgress in 0f..0.99f) {
                Spacer(Modifier.height(8.dp))
                SharedProgress(
                    progress = state.uploadProgress,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

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
                enabled = !state.deleting && !state.saving,
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

    val (initialHour, initialMinute) = when (activeAvailabilityField) {
        ServiceEditorAvailabilityField.Start -> parseTimeForPicker(state.availabilityStart, defaultHour = 9)
        ServiceEditorAvailabilityField.End -> parseTimeForPicker(state.availabilityEnd, defaultHour = 17)
        null -> 9 to 0
    }
    SharedTimePickerDialog(
        isOpen = activeAvailabilityField != null,
        title = if (activeAvailabilityField == ServiceEditorAvailabilityField.Start) "Start time" else "End time",
        initialHour = initialHour,
        initialMinute = initialMinute,
        onDismiss = { activeAvailabilityField = null },
        onConfirm = { hour, minute ->
            when (activeAvailabilityField) {
                ServiceEditorAvailabilityField.Start -> viewModel.onAvailabilityStartChange(formatTime12(hour, minute))
                ServiceEditorAvailabilityField.End -> viewModel.onAvailabilityEndChange(formatTime12(hour, minute))
                null -> Unit
            }
            activeAvailabilityField = null
        },
    )
}
