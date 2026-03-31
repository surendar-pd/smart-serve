package com.smartserve.customerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isChatEnabled: Boolean = false,
    val bookingStatus: String? = null,
    val providerName: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class ChatViewModel(
    private val bookingId: String,
    private val chatRepository: ChatRepository,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    val currentUserId: String? get() = auth.currentUser?.uid

    init {
        observeBookingStatus()
        loadProviderName()
        observeMessages()
    }

    private fun loadProviderName() {
        viewModelScope.launch {
            val name = runCatching { chatRepository.getProviderName(bookingId) }
                .getOrNull()
                ?.trim()
                .orEmpty()
            if (name.isNotBlank()) {
                _state.value = _state.value.copy(providerName = name)
            }
        }
    }

    private fun observeBookingStatus() {
        viewModelScope.launch {
            chatRepository.getBookingStatus(bookingId).collect { status ->
                _state.value = _state.value.copy(
                    bookingStatus = status,
                    isChatEnabled = status?.lowercase() in listOf("pending", "active"),
                )
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
                chatRepository.sendMessage(
                    bookingId = bookingId,
                    senderId  = uid,
                    text      = text,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    errorMessage = "Failed to send message: ${e.localizedMessage}",
                    inputText    = text,
                )
            }
        }
    }
}

