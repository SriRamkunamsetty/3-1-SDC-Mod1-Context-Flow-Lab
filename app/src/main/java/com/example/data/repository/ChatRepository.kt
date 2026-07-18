package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.RetrofitClient
import com.example.data.database.ChatDao
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ChatSessionEntity
import com.example.data.model.Content
import com.example.data.model.GeminiRequest
import com.example.data.model.GenerationConfig
import com.example.data.model.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ChatRepository(private val chatDao: ChatDao) {

    val allSessions: Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun createSession(session: ChatSessionEntity) = withContext(Dispatchers.IO) {
        chatDao.insertSession(session)
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        chatDao.deleteSessionById(sessionId)
        chatDao.clearMessagesForSession(sessionId)
    }

    suspend fun clearSessionMessages(sessionId: String) = withContext(Dispatchers.IO) {
        chatDao.clearMessagesForSession(sessionId)
    }

    suspend fun updateSessionSettings(
        sessionId: String,
        systemInstruction: String,
        temperature: Float,
        maxOutputTokens: Int
    ) = withContext(Dispatchers.IO) {
        chatDao.updateSessionSettings(sessionId, systemInstruction, temperature, maxOutputTokens)
    }

    suspend fun getSessionById(sessionId: String): ChatSessionEntity? = withContext(Dispatchers.IO) {
        chatDao.getSessionById(sessionId)
    }

    suspend fun insertMessage(message: ChatMessageEntity) = withContext(Dispatchers.IO) {
        chatDao.insertMessage(message)
    }

    /**
     * Sends a message, maintains context history by fetching all past messages
     * for this session, compiles them into a single multi-turn request,
     * and calls the Gemini API. Saves the response to Room.
     */
    suspend fun sendMessageAndGetReply(
        sessionId: String,
        userText: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(Exception("Gemini API key is not configured. Please enter your API key in the Secrets panel in AI Studio."))
            }

            // 1. Save user's message to Room database
            val userMessage = ChatMessageEntity(
                sessionId = sessionId,
                role = "user",
                text = userText
            )
            chatDao.insertMessage(userMessage)

            // 2. Load the session metadata to get parameters like system instruction and temperature
            val session = chatDao.getSessionById(sessionId)
                ?: return@withContext Result.failure(Exception("Session not found."))

            // 3. Load all messages in this session to build the conversation history (context)
            val pastMessages = chatDao.getMessagesForSession(sessionId).first()

            // 4. Map the messages to the Gemini contents structure
            val contents = pastMessages.map { msg ->
                Content(
                    role = msg.role,
                    parts = listOf(Part(text = msg.text))
                )
            }

            // 5. Construct the system instruction block if defined
            val systemInstruction = if (session.systemInstruction.isNotBlank()) {
                Content(
                    role = "system",
                    parts = listOf(Part(text = session.systemInstruction))
                )
            } else null

            // 6. Build the request body
            val request = GeminiRequest(
                contents = contents,
                generationConfig = GenerationConfig(
                    temperature = session.temperature,
                    maxOutputTokens = session.maxOutputTokens
                ),
                systemInstruction = systemInstruction
            )

            // 7. Fire the network call
            val response = RetrofitClient.service.generateContent(apiKey, request)

            // 8. Handle errors or candidates
            val error = response.error
            if (error != null) {
                return@withContext Result.failure(Exception("API Error (${error.code}): ${error.message}"))
            }

            val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext Result.failure(Exception("No response received from the model."))

            // 9. Save model's reply to Room database
            val modelMessage = ChatMessageEntity(
                sessionId = sessionId,
                role = "model",
                text = replyText
            )
            chatDao.insertMessage(modelMessage)

            Result.success(replyText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
