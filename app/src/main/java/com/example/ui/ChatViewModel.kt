package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ChatSessionEntity
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    // Active session ID flow
    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    // All saved sessions
    val sessions: StateFlow<List<ChatSessionEntity>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current active session metadata
    private val _activeSession = MutableStateFlow<ChatSessionEntity?>(null)
    val activeSession: StateFlow<ChatSessionEntity?> = _activeSession.asStateFlow()

    // Messages for the active session, dynamically updating when session ID changes
    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessageEntity>> = _activeSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != null) {
                repository.getMessagesForSession(sessionId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI Input field state
    private val _inputMessageText = MutableStateFlow("")
    val inputMessageText: StateFlow<String> = _inputMessageText.asStateFlow()

    // Loading / Sending state
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    // Error message state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // UI inspector toggle
    private val _isInspectorOpen = MutableStateFlow(false)
    val isInspectorOpen: StateFlow<Boolean> = _isInspectorOpen.asStateFlow()

    // Initialize with a default session if empty
    init {
        viewModelScope.launch {
            repository.allSessions.collect { sessionList ->
                if (sessionList.isEmpty() && _activeSessionId.value == null) {
                    createSession("Default Conversation")
                } else if (_activeSessionId.value == null && sessionList.isNotEmpty()) {
                    selectSession(sessionList.first().id)
                }
            }
        }
    }

    fun updateInputText(text: String) {
        _inputMessageText.value = text
    }

    fun toggleInspector() {
        _isInspectorOpen.value = !_isInspectorOpen.value
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun selectSession(sessionId: String) {
        viewModelScope.launch {
            _activeSessionId.value = sessionId
            _activeSession.value = repository.getSessionById(sessionId)
        }
    }

    fun createSession(title: String = "New Conversation") {
        viewModelScope.launch {
            val sessionId = UUID.randomUUID().toString()
            val newSession = ChatSessionEntity(
                id = sessionId,
                title = title,
                systemInstruction = "You are a helpful assistant.",
                temperature = 0.7f,
                maxOutputTokens = 1000
            )
            repository.createSession(newSession)
            selectSession(sessionId)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_activeSessionId.value == sessionId) {
                _activeSessionId.value = null
                _activeSession.value = null
            }
        }
    }

    fun clearActiveMessages() {
        val sessionId = _activeSessionId.value ?: return
        viewModelScope.launch {
            repository.clearSessionMessages(sessionId)
        }
    }

    fun updateSessionSettings(systemInstruction: String, temperature: Float, maxOutputTokens: Int) {
        val sessionId = _activeSessionId.value ?: return
        viewModelScope.launch {
            repository.updateSessionSettings(sessionId, systemInstruction, temperature, maxOutputTokens)
            _activeSession.value = repository.getSessionById(sessionId)
        }
    }

    fun sendMessage() {
        val sessionId = _activeSessionId.value ?: return
        val text = _inputMessageText.value.trim()
        if (text.isEmpty()) return

        _inputMessageText.value = ""
        _isSending.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = repository.sendMessageAndGetReply(sessionId, text)
            _isSending.value = false
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "An unknown error occurred"
            }
        }
    }

    /**
     * Instantly loads a rich chatbot preset (Trivia, Coding, RPG) that illustrates
     * multi-turn context flow in action.
     */
    fun loadPreset(title: String, systemInstruction: String, temperature: Float, firstMessage: String) {
        viewModelScope.launch {
            val sessionId = UUID.randomUUID().toString()
            val newSession = ChatSessionEntity(
                id = sessionId,
                title = title,
                systemInstruction = systemInstruction,
                temperature = temperature,
                maxOutputTokens = 1500
            )
            repository.createSession(newSession)
            
            // Pre-seed the chat session with a model message
            val modelMessage = ChatMessageEntity(
                sessionId = sessionId,
                role = "model",
                text = firstMessage
            )
            repository.insertMessage(modelMessage)
            
            selectSession(sessionId)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = AppDatabase.getDatabase(context)
            val repository = ChatRepository(database.chatDao())
            return ChatViewModel(repository) as T
        }
    }
}
