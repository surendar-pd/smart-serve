package com.smartserve.providerapp.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.smartserve.sharedui.SharedLoading
import com.smartserve.sharedui.SharedIconButton
import com.smartserve.sharedui.SharedTextField
import com.smartserve.sharedui.SharedTextFieldVariant
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalContext

@Composable
fun ChatScreen(
    bookingId: String,
    onBack: () -> Unit,
    bottomPadding: Dp = 0.dp,   // ← receives nav bar height from AppScreen
    topPadding: Dp = 0.dp,
) {
    val context = LocalContext.current
    val viewModel = remember(bookingId) {
        ChatViewModel(
            bookingId      = bookingId,
            chatRepository = ChatRepository(FirebaseFirestore.getInstance()),
            auth           = FirebaseAuth.getInstance(),
            context         = context,
        )
    }

    val state by viewModel.state.collectAsState()
    val currentUserId = viewModel.currentUserId
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val effectiveBottomPadding = if (imeBottomPx > 0) 0.dp else bottomPadding

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            }
            .padding(top    = topPadding,
            bottom = effectiveBottomPadding),  // avoid double-padding when IME is visible
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        ProviderStackHeader(
            title = "Chat",
            subtitle = state.customerName.ifBlank { "Customer" },
            onBack = onBack,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

        // ── Status banner ─────────────────────────────────────────────────────
        if (!state.isChatEnabled && state.bookingStatus != null) {
            Surface(
                color    = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = when (state.bookingStatus?.lowercase()) {
                        "completed" -> "This booking is completed. Chat is read-only."
                        "declined"  -> "This booking was declined. Chat is read-only."
                        "new"       -> "Chat will be available once booking is confirmed."
                        else        -> "Chat is not available for this booking status."
                    },
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        // ── Message area ──────────────────────────────────────────────────────
        Box(
            modifier         = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.isLoading -> SharedLoading()

                state.messages.isEmpty() -> Text(
                    text  = "No messages yet. Start the conversation!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> LazyColumn(
                    state               = listState,
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = state.messages, key = { it.id }) { message ->
                        MessageBubble(
                            message             = message,
                            isSentByCurrentUser = message.senderId == currentUserId,
                        )
                    }
                }
            }
        }

        // ── Input bar ─────────────────────────────────────────────────────────
        Surface(
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        ) {
            Row(
                modifier          = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SharedTextField(
                    value = state.inputText,
                    onValueChange = viewModel::onInputChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = if (state.isChatEnabled) "Type a message" else "Chat is disabled",
                    enabled = state.isChatEnabled,
                    singleLine = false,
                    minLines = 1,
                    maxLines = 4,
                    variant = SharedTextFieldVariant.Filled,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            viewModel.sendMessage()
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                    ),
                )

                Spacer(Modifier.width(8.dp))

                SharedIconButton(
                    onClick = {
                        viewModel.sendMessage()
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                    icon = Icons.AutoMirrored.Filled.Send,
                    enabled = state.isChatEnabled && state.inputText.isNotBlank(),
                    contentDescription = "Send",
                    contentColor = if (state.isChatEnabled && state.inputText.isNotBlank())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Message Bubble ────────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(
    message: Message,
    isSentByCurrentUser: Boolean,
) {
    val bubbleColor = if (isSentByCurrentUser)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant

    val textColor = if (isSentByCurrentUser)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    val bubbleShape = RoundedCornerShape(
        topStart    = 16.dp,
        topEnd      = 16.dp,
        bottomStart = if (isSentByCurrentUser) 16.dp else 4.dp,
        bottomEnd   = if (isSentByCurrentUser) 4.dp else 16.dp,
    )

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isSentByCurrentUser) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Column {
                Text(
                    text  = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
                message.timestamp?.let { ts ->
                    val timeText = remember(ts) {
                        SimpleDateFormat("h:mm a", Locale.getDefault()).format(ts.toDate())
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}