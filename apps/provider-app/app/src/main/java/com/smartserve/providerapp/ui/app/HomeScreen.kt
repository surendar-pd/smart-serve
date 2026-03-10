package com.smartserve.providerapp.ui.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartserve.sharedui.SharedBottomSheet
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedButtonVariant
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
) {
    var isSheetOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        SharedText(
            text = "Home (SharedText demo)",
            variant = SharedTextVariant.Title,
        )
        Spacer(modifier = Modifier.height(12.dp))

        SharedText(
            text = "Subtitle variant",
            variant = SharedTextVariant.Subtitle,
        )
        Spacer(modifier = Modifier.height(8.dp))

        SharedText(
            text = "Body variant: regular supporting text goes here.",
            variant = SharedTextVariant.Body,
        )
        Spacer(modifier = Modifier.height(8.dp))

        SharedText(
            text = "BodyStrong variant: emphasized supporting text.",
            variant = SharedTextVariant.BodyStrong,
        )
        Spacer(modifier = Modifier.height(12.dp))

        SharedText(
            text = "Caption variant: small helper text.",
            variant = SharedTextVariant.Caption,
        )
        Spacer(modifier = Modifier.height(8.dp))

        SharedText(
            text = "Label variant",
            variant = SharedTextVariant.Label,
        )

        Spacer(modifier = Modifier.height(20.dp))
        SharedText(
            text = "SharedButton variants",
            variant = SharedTextVariant.Subtitle,
        )
        Spacer(modifier = Modifier.height(12.dp))

        SharedButton(
            text = "Default",
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
            variant = SharedButtonVariant.Default,
        )
        Spacer(modifier = Modifier.height(10.dp))

        SharedButton(
            text = "Outline",
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
            variant = SharedButtonVariant.Outline,
        )
        Spacer(modifier = Modifier.height(10.dp))

        SharedButton(
            text = "Secondary",
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
            variant = SharedButtonVariant.Secondary,
        )
        Spacer(modifier = Modifier.height(10.dp))

        SharedButton(
            text = "Ghost",
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
            variant = SharedButtonVariant.Ghost,
        )
        Spacer(modifier = Modifier.height(10.dp))

        SharedButton(
            text = "Destructive",
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
            variant = SharedButtonVariant.Destructive,
        )

        Spacer(modifier = Modifier.height(20.dp))
        SharedText(
            text = "SharedBottomSheet",
            variant = SharedTextVariant.Subtitle,
        )
        Spacer(modifier = Modifier.height(12.dp))

        SharedBottomSheet(
            isOpen = isSheetOpen,
            onOpenChange = { isSheetOpen = it },
            sheetContent = {
                Column(modifier = Modifier.padding(24.dp)) {
                    SharedText(
                        text = "Draggable bottom sheet",
                        variant = SharedTextVariant.Title,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SharedText(
                        text = "Drag me down to dismiss or tap outside.",
                        variant = SharedTextVariant.Body,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SharedButton(
                        text = "Close",
                        onClick = { isSheetOpen = false },
                        modifier = Modifier.fillMaxWidth(),
                        variant = SharedButtonVariant.Outline,
                    )
                }
            },
        ) { open ->
            SharedButton(
                text = "Open bottom sheet",
                onClick = open,
                modifier = Modifier.fillMaxWidth(),
                variant = SharedButtonVariant.Default,
            )
        }
    }
}
