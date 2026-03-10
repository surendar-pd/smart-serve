package com.smartserve.sharedui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Exposed dropdown: shows selected option in a field; expands to pick from [options].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedDropdown(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Select",
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedOption ?: "",
            onValueChange = { },
            readOnly = true,
            label = { SharedText(text = label, variant = SharedTextVariant.Label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            for (option in options) {
                DropdownMenuItem(
                    text = { SharedText(text = option, variant = SharedTextVariant.Body) },
                    onClick = {
                        onOptionSelected(option)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}
