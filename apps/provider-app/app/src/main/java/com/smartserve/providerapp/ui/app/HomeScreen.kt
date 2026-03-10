package com.smartserve.providerapp.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.smartserve.sharedui.SharedBottomSheet
import com.smartserve.sharedui.SharedButton
import com.smartserve.sharedui.SharedButtonVariant
import com.smartserve.sharedui.SharedCard
import com.smartserve.sharedui.SharedConfirmDialog
import com.smartserve.sharedui.SharedAvatar
import com.smartserve.sharedui.SharedBadge
import com.smartserve.sharedui.SharedBadgeVariant
import com.smartserve.sharedui.SharedEmptyState
import com.smartserve.sharedui.SharedLoading
import com.smartserve.sharedui.SharedSkeleton
import com.smartserve.sharedui.SharedSeparator
import com.smartserve.sharedui.SharedSwitchRow
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant
import com.smartserve.sharedui.SharedTextArea
import com.smartserve.sharedui.SharedTextField
import com.smartserve.sharedui.SharedTextFieldVariant
import com.smartserve.sharedui.SharedAccordion

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
) {
    var isSheetOpen by remember { mutableStateOf(false) }
    var isDialogOpen by remember { mutableStateOf(false) }

    var inputValue by remember { mutableStateOf("") }
    var textAreaValue by remember { mutableStateOf("") }
    var switchChecked by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        SharedText(
            text = "Provider Home — Shared UI Gallery",
            variant = SharedTextVariant.Title,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SharedText(
            text = "This screen renders every shared base component for quick visual testing.",
            variant = SharedTextVariant.Body,
        )
        Spacer(modifier = Modifier.height(16.dp))
        SharedSeparator()
        Spacer(modifier = Modifier.height(16.dp))

        SharedText(text = "SharedText variants", variant = SharedTextVariant.Subtitle)
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
        SharedSeparator()
        Spacer(modifier = Modifier.height(16.dp))

        SharedText(text = "SharedButton variants", variant = SharedTextVariant.Subtitle)
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
        SharedSeparator()
        Spacer(modifier = Modifier.height(16.dp))

        SharedText(text = "SharedBottomSheet", variant = SharedTextVariant.Subtitle)
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

        Spacer(modifier = Modifier.height(20.dp))
        SharedSeparator()
        Spacer(modifier = Modifier.height(16.dp))

        SharedText(text = "Inputs", variant = SharedTextVariant.Subtitle)
        Spacer(modifier = Modifier.height(12.dp))

        SharedTextField(
            value = inputValue,
            onValueChange = { inputValue = it },
            label = "SharedTextField",
            placeholder = "Type something…",
            supportingText = "Outlined variant with helper text",
            variant = SharedTextFieldVariant.Outlined,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))

        SharedTextArea(
            value = textAreaValue,
            onValueChange = { textAreaValue = it },
            label = "SharedTextArea",
            placeholder = "Multi-line input…",
            supportingText = "Use this for longer descriptions",
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))
        SharedSeparator()
        Spacer(modifier = Modifier.height(16.dp))

        SharedText(text = "Badges", variant = SharedTextVariant.Subtitle)
        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SharedBadge(text = "Neutral", variant = SharedBadgeVariant.Neutral)
            SharedBadge(text = "Info", variant = SharedBadgeVariant.Info)
            SharedBadge(text = "Success", variant = SharedBadgeVariant.Success)
            SharedBadge(text = "Warning", variant = SharedBadgeVariant.Warning)
            SharedBadge(text = "Destructive", variant = SharedBadgeVariant.Destructive)
            SharedBadge(
                text = "Outlined badge",
                variant = SharedBadgeVariant.Neutral,
                outlined = true,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        SharedSeparator()
        Spacer(modifier = Modifier.height(16.dp))

        SharedText(text = "Cards", variant = SharedTextVariant.Subtitle)
        Spacer(modifier = Modifier.height(12.dp))

        SharedCard(modifier = Modifier.fillMaxWidth()) {
            SharedText(text = "SharedCard", variant = SharedTextVariant.BodyStrong)
            Spacer(modifier = Modifier.height(6.dp))
            SharedText(
                text = "Use this as the base for ProviderCard, ServiceCard, BookingCard, etc.",
                variant = SharedTextVariant.Body,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        SharedCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { },
        ) {
            SharedText(text = "Clickable SharedCard", variant = SharedTextVariant.BodyStrong)
            Spacer(modifier = Modifier.height(6.dp))
            SharedText(text = "Tap feedback enabled via onClick.", variant = SharedTextVariant.Caption)
        }

        Spacer(modifier = Modifier.height(20.dp))
        SharedSeparator()
        Spacer(modifier = Modifier.height(16.dp))

        SharedText(text = "Avatar", variant = SharedTextVariant.Subtitle)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SharedAvatar(name = "Alex Chen")
            SharedAvatar(name = "Sam")
            SharedAvatar(name = null)
        }

        Spacer(modifier = Modifier.height(20.dp))
        SharedSeparator()
        Spacer(modifier = Modifier.height(16.dp))

        SharedText(text = "Switch", variant = SharedTextVariant.Subtitle)
        Spacer(modifier = Modifier.height(12.dp))
        SharedSwitchRow(
            checked = switchChecked,
            onCheckedChange = { switchChecked = it },
            title = "Enable Smart Suggestions",
            description = "Example toggle row component.",
        )

        Spacer(modifier = Modifier.height(20.dp))
        SharedSeparator()
        Spacer(modifier = Modifier.height(16.dp))

        SharedText(text = "Accordion", variant = SharedTextVariant.Subtitle)
        Spacer(modifier = Modifier.height(12.dp))
        SharedAccordion(title = "Filters (Accordion)") {
            SharedText(text = "Accordion content area", variant = SharedTextVariant.Body)
            Spacer(modifier = Modifier.height(10.dp))
            SharedSkeleton(height = 14.dp)
            Spacer(modifier = Modifier.height(8.dp))
            SharedSkeleton(height = 14.dp, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(20.dp))
        SharedSeparator()
        Spacer(modifier = Modifier.height(16.dp))

        SharedText(text = "Loading + Skeleton", variant = SharedTextVariant.Subtitle)
        Spacer(modifier = Modifier.height(12.dp))
        SharedCard(modifier = Modifier.fillMaxWidth()) {
            SharedText(text = "Skeleton lines", variant = SharedTextVariant.BodyStrong)
            Spacer(modifier = Modifier.height(10.dp))
            SharedSkeleton(height = 14.dp, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            SharedSkeleton(height = 14.dp, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            SharedSkeleton(height = 14.dp, modifier = Modifier.fillMaxWidth())
        }
        Spacer(modifier = Modifier.height(10.dp))
        SharedCard(modifier = Modifier.fillMaxWidth()) {
            SharedText(text = "Loading", variant = SharedTextVariant.BodyStrong)
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .height(120.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
            ) {
                SharedLoading(text = "Loading inside a card…")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        SharedSeparator()
        Spacer(modifier = Modifier.height(16.dp))

        SharedText(text = "Empty state", variant = SharedTextVariant.Subtitle)
        Spacer(modifier = Modifier.height(12.dp))
        SharedCard(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.height(180.dp).fillMaxWidth()) {
                SharedEmptyState(
                    title = "No results",
                    description = "Try changing filters or searching again.",
                    action = {
                        SharedButton(
                            text = "Try again",
                            onClick = { },
                            variant = SharedButtonVariant.Outline,
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        SharedSeparator()
        Spacer(modifier = Modifier.height(16.dp))

        SharedText(text = "Dialog", variant = SharedTextVariant.Subtitle)
        Spacer(modifier = Modifier.height(12.dp))
        SharedButton(
            text = "Open confirm dialog",
            onClick = { isDialogOpen = true },
            modifier = Modifier.fillMaxWidth(),
            variant = SharedButtonVariant.Secondary,
        )
        SharedConfirmDialog(
            title = "Cancel booking?",
            message = "This is a shared confirm dialog example.",
            isOpen = isDialogOpen,
            destructive = true,
            confirmText = "Cancel booking",
            cancelText = "Keep",
            onDismiss = { isDialogOpen = false },
            onConfirm = { isDialogOpen = false },
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}
