package com.smartserve.sharedui

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable

@Composable
fun SharedConfirmDialog(
    title: String,
    message: String,
    isOpen: Boolean,
    onDismiss: () -> Unit,
    confirmText: String = "Confirm",
    cancelText: String = "Cancel",
    destructive: Boolean = false,
    onConfirm: () -> Unit,
) {
    if (!isOpen) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { SharedText(text = title, variant = SharedTextVariant.Subtitle) },
        text = { SharedText(text = message, variant = SharedTextVariant.Body) },
        confirmButton = {
            SharedButton(
                text = confirmText,
                onClick = onConfirm,
                variant = if (destructive) SharedButtonVariant.Destructive else SharedButtonVariant.Default,
            )
        },
        dismissButton = {
            SharedButton(
                text = cancelText,
                onClick = onDismiss,
                variant = SharedButtonVariant.Outline,
            )
        },
    )
}

