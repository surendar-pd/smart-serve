package com.smartserve.sharedui

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
) {
    val destructiveColors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
    )

    when (variant) {
        SharedButtonVariant.Default -> Button(onClick = onClick, modifier = modifier) {
            Text(text = text)
        }

        SharedButtonVariant.Outline -> OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Text(text = text)
        }

        SharedButtonVariant.Secondary -> Button(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Text(text = text)
        }

        SharedButtonVariant.Ghost -> TextButton(onClick = onClick, modifier = modifier) {
            Text(text = text)
        }

        SharedButtonVariant.Destructive -> Button(
            onClick = onClick,
            modifier = modifier,
            colors = destructiveColors,
        ) {
            Text(text = text)
        }
    }
}
