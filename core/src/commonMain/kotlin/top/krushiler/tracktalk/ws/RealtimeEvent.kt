@file:UseSerializers(IsoInstantSerializer::class)

package top.krushiler.tracktalk.ws

import kotlin.time.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import top.krushiler.tracktalk.IsoInstantSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable(with = RealtimeEventSerializer::class)
sealed interface RealtimeEvent {
    data class FriendLocationUpdate(
        val data: FriendLocationUpdateData,
    ) : RealtimeEvent

    data class ChatMessage(
        val data: ChatMessageReceivedData,
    ) : RealtimeEvent

    data class Unknown(
        val type: String,
        val data: JsonElement,
    ) : RealtimeEvent
}

@Serializable
data class FriendLocationUpdateData(
    val userId: Long,
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class ChatMessageReceivedData(
    val messageId: Long,
    val userId: Long,
    val createdAt: Instant,
)

object RealtimeEventSerializer : KSerializer<RealtimeEvent> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun serialize(encoder: Encoder, value: RealtimeEvent) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("RealtimeEvent can only be serialized as JSON")
        val element = when (value) {
            is RealtimeEvent.FriendLocationUpdate -> envelope(
                type = "friend_location_update",
                data = jsonEncoder.json.encodeToJsonElement(FriendLocationUpdateData.serializer(), value.data),
            )
            is RealtimeEvent.ChatMessage -> envelope(
                type = "chat_message",
                data = jsonEncoder.json.encodeToJsonElement(ChatMessageReceivedData.serializer(), value.data),
            )
            is RealtimeEvent.Unknown -> envelope(value.type, value.data)
        }
        jsonEncoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): RealtimeEvent {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("RealtimeEvent can only be deserialized from JSON")
        val element = jsonDecoder.decodeJsonElement()
        val obj = element as? JsonObject
            ?: return RealtimeEvent.Unknown(type = "", data = element)
        val type = obj["type"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val data = obj["data"] ?: JsonNull
        return when (type) {
            "friend_location_update" -> RealtimeEvent.FriendLocationUpdate(
                jsonDecoder.json.decodeFromJsonElement(FriendLocationUpdateData.serializer(), data),
            )
            "chat_message" -> RealtimeEvent.ChatMessage(
                jsonDecoder.json.decodeFromJsonElement(ChatMessageReceivedData.serializer(), data),
            )
            else -> RealtimeEvent.Unknown(type, data)
        }
    }

    private fun envelope(type: String, data: JsonElement): JsonObject =
        buildJsonObject {
            put("type", type)
            put("data", data)
        }
}
