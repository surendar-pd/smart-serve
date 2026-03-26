package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.smartserve.sharedui.SharedIconButton
import com.smartserve.sharedui.SharedScreenHeader

/**
 * Tab/root screens: same header chrome as Home — [SharedScreenHeader] with optional trailing action.
 */
@Composable
fun CustomerTabHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SharedScreenHeader(
            title = title,
            subtitle = subtitle,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/**
 * Pushed/detail screens: back control + [SharedScreenHeader] for consistent title/subtitle typography.
 */
@Composable
fun CustomerStackHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        SharedIconButton(
            onClick = onBack,
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
        )
        SharedScreenHeader(
            title = title,
            subtitle = subtitle,
            modifier = Modifier.weight(1f),
        )
    }
}
