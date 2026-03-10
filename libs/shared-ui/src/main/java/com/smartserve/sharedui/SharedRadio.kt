package com.smartserve.sharedui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Radio option for single-select. Use with a group (e.g. Column of SharedRadio) and shared selected value.
 */
@Composable
fun SharedRadio(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    description: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .then(if (label != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        if (label != null) {
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                SharedText(text = label, variant = SharedTextVariant.Body)
                if (description != null) {
                    Spacer(Modifier.height(4.dp))
                    SharedText(text = description, variant = SharedTextVariant.Caption)
                }
            }
        }
    }
}
