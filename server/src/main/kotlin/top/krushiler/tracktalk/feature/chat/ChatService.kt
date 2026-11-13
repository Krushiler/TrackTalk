package top.krushiler.tracktalk.feature.chat

import io.ktor.http.HttpStatusCode
import top.krushiler.tracktalk.feature.friendship.FriendshipService
import top.krushiler.tracktalk.feature.push.PushService
import top.krushiler.tracktalk.feature.realtime.RealtimeBusEvent
import top.krushiler.tracktalk.feature.realtime.RealtimeEventBus
import top.krushiler.tracktalk.feature.user.Profiles
import top.krushiler.tracktalk.http.ApiException
import top.krushiler.tracktalk.model.ChatMessage
import top.krushiler.tracktalk.model.User

internal class ChatService(
    private val messages: ChatRepository,
    private val profiles: Profiles,
    private val friendships: FriendshipService,
    private val events: RealtimeEventBus,
    private val push: PushService,
) {
    suspend fun get(currentUserId: Long, messageId: Long): ChatMessage {
        val (record, sender) = when (val loaded = messages.find(currentUserId, messageId)) {
            LoadedMessage.MissingUser -> throw ApiException(HttpStatusCode.Unauthorized, "User not found")
            LoadedMessage.NotFound -> throw ApiException(HttpStatusCode.NotFound, "Message not found")
            is LoadedMessage.Found -> loaded.message to loaded.sender
        }
        return record.toMessage(profiles.user(sender))
    }

    suspend fun list(currentUserId: Long, otherUserId: Long, beforeId: Long?, limit: Int): List<ChatMessage> {
        if (otherUserId == currentUserId) {
            throw ApiException(HttpStatusCode.BadRequest, "Cannot list messages with yourself")
        }
        friendships.requireAccepted(currentUserId, otherUserId)
        val page = when (val listed = messages.list(currentUserId, otherUserId, beforeId, limit)) {
            MessagePage.MissingUser -> throw ApiException(HttpStatusCode.Unauthorized, "User not found")
            is MessagePage.Listed -> listed.rows
        }
        return page.map { row -> row.message.toMessage(row.sender?.let { profiles.user(it) }) }
    }

    suspend fun send(currentUserId: Long, otherUserId: Long, body: String): ChatMessage {
        if (otherUserId == currentUserId) {
            throw ApiException(HttpStatusCode.BadRequest, "Cannot message yourself")
        }
        if (body.length > 4000) {
            throw ApiException(HttpStatusCode.BadRequest, "body is too long")
        }
        val text = body.trim()
        if (text.isEmpty()) {
            throw ApiException(HttpStatusCode.BadRequest, "body cannot be empty")
        }
        friendships.requireAccepted(currentUserId, otherUserId)
        val sent = when (val result = messages.send(currentUserId, otherUserId, text)) {
            SentMessage.MissingUser -> throw ApiException(HttpStatusCode.Unauthorized, "User not found")
            SentMessage.RecipientMissing -> throw ApiException(HttpStatusCode.NotFound, "User not found")
            is SentMessage.Sent -> result
        }
        events.publish(
            RealtimeBusEvent.ChatMessageReceived(
                recipientId = otherUserId,
                messageId = sent.message.id,
                userId = sent.message.senderId,
                createdAt = sent.message.createdAt,
            ),
        )
        val preview = if (text.length <= 100) text else text.take(97) + "..."
        push.send(
            userId = otherUserId,
            title = sent.sender.username,
            body = preview,
            data = mapOf(
                "type" to "chat_message",
                "message_id" to sent.message.id.toString(),
                "user_id" to currentUserId.toString(),
            ),
            skipIfOnline = true,
        )
        return sent.message.toMessage(profiles.user(sent.sender))
    }

    private fun ChatRecord.toMessage(sender: User?): ChatMessage = ChatMessage(
        id = id,
        senderId = senderId,
        recipientId = recipientId,
        body = body,
        createdAt = createdAt,
        sender = sender,
    )
}
