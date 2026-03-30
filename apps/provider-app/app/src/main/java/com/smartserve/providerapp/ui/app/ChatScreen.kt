package com.smartserve.providerapp.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.smartserve.sharedui.SharedTopAppBar
import com.smartserve.sharedui.SharedLoading
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ChatScreen(
    bookingId: String,
    onBack: () -> Unit,
    bottomPadding: Dp = 0.dp,   // ← receives nav bar height from AppScreen
    topPadding: Dp = 0.dp,
) {
    val viewModel = remember(bookingId) {
        ChatViewModel(
            bookingId      = bookingId,
            chatRepository = ChatRepository(FirebaseFirestore.getInstance()),
            auth           = FirebaseAuth.getInstance(),
        )
    }

    val state by viewModel.state.collectAsState()
    val currentUserId = viewModel.currentUserId
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top    = topPadding,
            bottom = bottomPadding),  // ← pushes content above nav bar
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        SharedTopAppBar(title = "Chat", onBack = onBack)

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
            tonalElevation = 4.dp,
            modifier       = Modifier
                .fillMaxWidth()
                .imePadding(),
        ) {
            Row(
                modifier          = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value         = state.inputText,
                    onValueChange = viewModel::onInputChanged,
                    modifier      = Modifier.weight(1f),
                    placeholder   = {
                        Text(if (state.isChatEnabled) "Type a message..." else "Chat is disabled")
                    },
                    enabled  = state.isChatEnabled,
                    maxLines = 4,
                    shape    = RoundedCornerShape(24.dp),
                )

                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = viewModel::sendMessage,
                    enabled = state.isChatEnabled && state.inputText.isNotBlank(),
                    colors  = IconButtonDefaults.iconButtonColors(
                        containerColor         = MaterialTheme.colorScheme.primary,
                        contentColor           = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor   = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                    )
                }
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