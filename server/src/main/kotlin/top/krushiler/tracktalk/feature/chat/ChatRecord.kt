package top.krushiler.tracktalk.feature.chat

import kotlin.time.Instant

internal data class ChatRecord(
    val id: Long,
    val senderId: Long,
    val recipientId: Long,
    val body: String,
    val createdAt: Instant,
)
