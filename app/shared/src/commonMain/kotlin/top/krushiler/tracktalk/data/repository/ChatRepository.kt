package top.krushiler.tracktalk.data.repository

import top.krushiler.tracktalk.data.network.HttpRoutes
import top.krushiler.tracktalk.dto.ChatMessageCreate
import top.krushiler.tracktalk.model.ChatMessage

class ChatRepository(
    private val http: HttpRoutes,
) {
    suspend fun messages(userId: Long, beforeId: Long? = null, limit: Int = 50): List<ChatMessage> =
        http.messages(userId, beforeId, limit)

    suspend fun send(userId: Long, body: String): ChatMessage =
        http.sendMessage(userId, ChatMessageCreate(body))

    suspend fun message(messageId: Long): ChatMessage = http.message(messageId)
}
