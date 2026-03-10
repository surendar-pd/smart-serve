package com.smartserve.sharedui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedBottomSheet(
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    sheetContent: @Composable () -> Unit,
    content: @Composable (open: () -> Unit) -> Unit,
) {
    val sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
    )

    content {
        onOpenChange(true)
    }

    LaunchedEffect(isOpen) {
        if (isOpen) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    if (isOpen) {
        ModalBottomSheet(
            onDismissRequest = { onOpenChange(false) },
            sheetState = sheetState,
        ) {
            sheetContent()
        }
    }
}

