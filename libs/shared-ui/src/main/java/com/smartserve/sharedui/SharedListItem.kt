package com.smartserve.sharedui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
private fun SupportingTextContent(text: String) {
    SharedText(text = text, variant = SharedTextVariant.Body)
}

/**
 * Single row list item with optional leading (icon or avatar), title, supporting text, and trailing slot.
 */
@Composable
fun SharedListItem(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    leadingAvatar: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = false,
) {
    if (showDivider) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
    val listItemModifier = if (onClick != null) modifier.then(Modifier.clickable(onClick = onClick)) else modifier
    ListItem(
        headlineContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SharedText(text = title, variant = SharedTextVariant.BodyStrong)
            }
        },
        supportingContent = if (supportingText != null) {
            { SupportingTextContent(supportingText) }
        } else null,
        leadingContent = when {
            leadingAvatar != null -> leadingAvatar
            leadingIcon != null -> {
                {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> null
        },
        trailingContent = trailing,
        modifier = listItemModifier,
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            headlineColor = MaterialTheme.colorScheme.onSurface,
            supportingColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}
