package com.smartserve.providerapp.ui.app

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.smartserve.providerapp.service.AccessTokenProvider
import com.smartserve.providerapp.service.FcmApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isChatEnabled: Boolean = false,
    val bookingStatus: String? = null,
    val customerName: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class ChatViewModel(
    private val bookingId: String,
    private val chatRepository: ChatRepository,
    private val auth: FirebaseAuth,
    private val context: Context,            // ← ADD THIS
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    val currentUserId: String? get() = auth.currentUser?.uid

    init {
        observeBookingStatus()
        loadCustomerName()
        observeMessages()
    }

    private fun loadCustomerName() {
        viewModelScope.launch {
            val name = runCatching { chatRepository.getCustomerName(bookingId) }
                .getOrNull()
                ?.trim()
                .orEmpty()
            if (name.isNotBlank()) {
                _state.value = _state.value.copy(customerName = name)
            }
        }
    }

    private fun observeBookingStatus() {
        viewModelScope.launch {
            chatRepository.getBookingStatus(bookingId).collect { status ->
                android.util.Log.d("ChatVM", "Booking status from Firestore: '$status'")
                _state.value = _state.value.copy(
                    bookingStatus = status,
                    isChatEnabled = status?.lowercase() in listOf("pending", "active"),
                )
                android.util.Log.d("ChatVM", "isChatEnabled: ${status?.lowercase() in listOf("pending", "active")}")
            }
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            chatRepository.getMessages(bookingId).collect { messages ->
                _state.value = _state.value.copy(
                    messages  = messages,
                    isLoading = false,
                )
            }
        }
    }

    fun onInputChanged(newText: String) {
        _state.value = _state.value.copy(inputText = newText)
    }

    fun sendMessage() {
        val text = _state.value.inputText.trim()
        val uid  = currentUserId

        if (text.isBlank() || uid == null || !_state.value.isChatEnabled) return

        _state.value = _state.value.copy(inputText = "")

        viewModelScope.launch {
            try {
                // 1. Send message to Firestore (existing)
                chatRepository.sendMessage(
                    bookingId = bookingId,
                    senderId  = uid,
                    text      = text,
                )

                // 2. Send FCM notification to recipient
                sendChatNotification(senderUid = uid, messageText = text)

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    errorMessage = "Failed to send message: ${e.localizedMessage}",
                    inputText    = text,
                )
            }
        }
    }

    // ── NEW FUNCTION ──────────────────────────────────────────────────────────
    private suspend fun sendChatNotification(senderUid: String, messageText: String) {
    try {
        val firestore = FirebaseFirestore.getInstance()

        val bookingSnap = firestore
            .collection("bookings")
            .document(bookingId)
            .get()
            .await()

        val booking = bookingSnap.data ?: run {
            Log.e("ChatVM", "Booking not found: $bookingId")
            return
        }

        // ← FIXED: fields are Firestore references
        val providerRef = booking["provider"] as? com.google.firebase.firestore.DocumentReference ?: run {
            Log.e("ChatVM", "provider reference missing in booking")
            return
        }
        val customerRef = booking["customer"] as? com.google.firebase.firestore.DocumentReference ?: run {
            Log.e("ChatVM", "customer reference missing in booking")
            return
        }

        val providerId = providerRef.id
        val customerId = customerRef.id

        Log.d("ChatVM", "providerId=$providerId customerId=$customerId")

        // Provider sends → recipient is customer → token in "users"
        val recipientToken = firestore
            .collection("users")
            .document(customerId)
            .get()
            .await()
            .getString("fcmToken") ?: run {
                Log.w("ChatVM", "No fcmToken for customer $customerId")
                return
            }

        // Sender is provider → name in "provider_profiles"
        val senderName = firestore
            .collection("provider_profiles")
            .document(providerId)
            .get()
            .await()
            .getString("displayName") ?: "Provider"

        val accessToken = AccessTokenProvider.getAccessToken(context) ?: run {
            Log.e("ChatVM", "Failed to get FCM access token")
            return
        }

        FcmApiService.sendNotification(
            recipientToken = recipientToken,
            title          = "Message from $senderName",
            body           = messageText,
            bookingId      = bookingId,
            accessToken    = accessToken,
        )

        Log.d("ChatVM", "Notification sent to customer $customerId")

    } catch (e: Exception) {
        Log.e("ChatVM", "Notification failed: ${e.localizedMessage}", e)
    }
}
}

