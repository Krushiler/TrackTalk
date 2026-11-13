package top.krushiler.tracktalk.feature.realtime

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import top.krushiler.tracktalk.ws.ChatMessageReceivedData
import top.krushiler.tracktalk.ws.FriendLocationUpdateData
import top.krushiler.tracktalk.ws.RealtimeEvent

internal class RealtimeHandler(
    private val events: RealtimeEventBus,
    private val realtime: RealtimeService,
) {
    private val logger = LoggerFactory.getLogger(RealtimeHandler::class.java)

    suspend fun start() {
        events.stream.collect { event ->
            try {
                deliver(event)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                logger.warn("Failed to deliver realtime event {}", event::class.simpleName, error)
            }
        }
    }

    private suspend fun deliver(event: RealtimeBusEvent) {
        when (event) {
            is RealtimeBusEvent.FriendLocationUpdated -> realtime.send(
                userIds = event.recipients,
                event = RealtimeEvent.FriendLocationUpdate(
                    FriendLocationUpdateData(
                        userId = event.userId,
                        latitude = event.latitude,
                        longitude = event.longitude,
                    ),
                ),
            )
            is RealtimeBusEvent.FriendLocationStopped -> realtime.sendRaw(
                userIds = listOf(event.recipientId),
                json = buildJsonObject {
                    put("type", "friend_location_stopped")
                    put("user_id", JsonPrimitive(event.userId))
                }.toString(),
            )
            is RealtimeBusEvent.ChatMessageReceived -> realtime.send(
                userIds = listOf(event.recipientId),
                event = RealtimeEvent.ChatMessage(
                    ChatMessageReceivedData(
                        messageId = event.messageId,
                        userId = event.userId,
                        createdAt = event.createdAt,
                    ),
                ),
            )
        }
    }
}
