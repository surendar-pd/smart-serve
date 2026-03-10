package com.smartserve.sharedui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SharedSeparator(
    modifier: Modifier = Modifier,
    inset: PaddingValues = PaddingValues(0.dp),
) {
    HorizontalDivider(
        modifier = modifier
            .fillMaxWidth()
            .padding(inset),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

