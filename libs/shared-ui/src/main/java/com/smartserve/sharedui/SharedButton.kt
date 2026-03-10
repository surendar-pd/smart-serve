package com.smartserve.sharedui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class SharedButtonVariant {
    Default,
    Outline,
    Secondary,
    Ghost,
    Destructive,
}

@Composable
fun SharedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: SharedButtonVariant = SharedButtonVariant.Default,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val destructiveColors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
    )

    val iconSize = 18.dp
    val iconSpacer = 8.dp

    @Composable
    fun ButtonContent() {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                )
                Spacer(Modifier.width(iconSpacer))
            }
            Text(text = text)
            if (trailingIcon != null) {
                Spacer(Modifier.width(iconSpacer))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
    }

    when (variant) {
        SharedButtonVariant.Default -> Button(onClick = onClick, modifier = modifier) {
            ButtonContent()
        }

        SharedButtonVariant.Outline -> OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            ButtonContent()
        }

        SharedButtonVariant.Secondary -> Button(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            ButtonContent()
        }

        SharedButtonVariant.Ghost -> TextButton(onClick = onClick, modifier = modifier) {
            ButtonContent()
        }

        SharedButtonVariant.Destructive -> Button(
            onClick = onClick,
            modifier = modifier,
            colors = destructiveColors,
        ) {
            ButtonContent()
        }
    }
}
