@file:UseSerializers(IsoInstantSerializer::class)

package top.krushiler.tracktalk.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import top.krushiler.tracktalk.IsoInstantSerializer

@Serializable
data class ChatMessage(
    val id: Long,
    val senderId: Long,
    val recipientId: Long,
    val body: String,
    val createdAt: Instant,
    val sender: User? = null,
)
