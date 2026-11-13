package top.krushiler.tracktalk.feature.chat

import kotlin.time.Clock
import kotlin.time.Instant
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import top.krushiler.tracktalk.database.ChatMessages
import top.krushiler.tracktalk.database.GmsDatabase
import top.krushiler.tracktalk.database.asDbId
import top.krushiler.tracktalk.database.toKotlinInstant
import top.krushiler.tracktalk.database.toOffsetDateTime
import top.krushiler.tracktalk.feature.user.UserRecord
import top.krushiler.tracktalk.feature.user.findUser

internal class ChatRepository(private val database: GmsDatabase) {
    suspend fun find(currentUserId: Long, messageId: Long): LoadedMessage = database.transaction {
        if (findUser(currentUserId) == null) return@transaction LoadedMessage.MissingUser
        val row = findChatMessage(messageId)
        if (row == null || currentUserId !in setOf(row.senderId, row.recipientId)) {
            return@transaction LoadedMessage.NotFound
        }
        val sender = findUser(row.senderId) ?: return@transaction LoadedMessage.NotFound
        LoadedMessage.Found(row, sender)
    }

    suspend fun list(currentUserId: Long, otherUserId: Long, beforeId: Long?, limit: Int): MessagePage =
        database.transaction {
            if (findUser(currentUserId) == null) return@transaction MessagePage.MissingUser
            val rows = listChatMessages(
                firstUserId = currentUserId,
                secondUserId = otherUserId,
                beforeId = beforeId,
                limit = limit.coerceIn(1, 100),
            ).map { row -> MessageRow(row, findUser(row.senderId)) }
            MessagePage.Listed(rows)
        }

    suspend fun send(currentUserId: Long, otherUserId: Long, body: String): SentMessage = database.transaction {
        val me = findUser(currentUserId) ?: return@transaction SentMessage.MissingUser
        if (findUser(otherUserId) == null) return@transaction SentMessage.RecipientMissing
        val row = insertChatMessage(
            senderId = me.id,
            recipientId = otherUserId,
            body = body,
            createdAt = Clock.System.now(),
        )
        SentMessage.Sent(row, me)
    }
}

private fun findChatMessage(id: Long): ChatRecord? =
    ChatMessages.selectAll()
        .where { ChatMessages.id eq id.asDbId() }
        .singleOrNull()
        ?.toChat()

private fun listChatMessages(
    firstUserId: Long,
    secondUserId: Long,
    beforeId: Long?,
    limit: Int,
): List<ChatRecord> {
    val first = firstUserId.asDbId()
    val second = secondUserId.asDbId()
    val conversation = (
        (ChatMessages.senderId eq first) and (ChatMessages.recipientId eq second)
        ) or (
        (ChatMessages.senderId eq second) and (ChatMessages.recipientId eq first)
        )
    return ChatMessages.selectAll()
        .where {
            if (beforeId == null) {
                conversation
            } else {
                conversation and (ChatMessages.id less beforeId.asDbId())
            }
        }
        .orderBy(ChatMessages.id to SortOrder.DESC)
        .limit(limit)
        .map { it.toChat() }
}

private fun insertChatMessage(
    senderId: Long,
    recipientId: Long,
    body: String,
    createdAt: Instant,
): ChatRecord {
    val id = ChatMessages.insert {
        it[ChatMessages.senderId] = senderId.asDbId()
        it[ChatMessages.recipientId] = recipientId.asDbId()
        it[ChatMessages.body] = body
        it[ChatMessages.createdAt] = createdAt.toOffsetDateTime()
    } get ChatMessages.id
    return ChatRecord(
        id = id.toLong(),
        senderId = senderId,
        recipientId = recipientId,
        body = body,
        createdAt = createdAt,
    )
}

private fun ResultRow.toChat(): ChatRecord = ChatRecord(
    id = this[ChatMessages.id].toLong(),
    senderId = this[ChatMessages.senderId].toLong(),
    recipientId = this[ChatMessages.recipientId].toLong(),
    body = this[ChatMessages.body],
    createdAt = this[ChatMessages.createdAt].toKotlinInstant(),
)

internal data class MessageRow(val message: ChatRecord, val sender: UserRecord?)

internal sealed interface LoadedMessage {
    data class Found(val message: ChatRecord, val sender: UserRecord) : LoadedMessage
    data object MissingUser : LoadedMessage
    data object NotFound : LoadedMessage
}

internal sealed interface MessagePage {
    data class Listed(val rows: List<MessageRow>) : MessagePage
    data object MissingUser : MessagePage
}

internal sealed interface SentMessage {
    data class Sent(val message: ChatRecord, val sender: UserRecord) : SentMessage
    data object MissingUser : SentMessage
    data object RecipientMissing : SentMessage
}
