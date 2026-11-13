package top.krushiler.tracktalk.feature.realtime

import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first

internal sealed interface RealtimeBusEvent {
    data class FriendLocationUpdated(
        val recipients: List<Long>,
        val userId: Long,
        val latitude: Double,
        val longitude: Double,
    ) : RealtimeBusEvent

    data class FriendLocationStopped(
        val recipientId: Long,
        val userId: Long,
    ) : RealtimeBusEvent

    data class ChatMessageReceived(
        val recipientId: Long,
        val messageId: Long,
        val userId: Long,
        val createdAt: Instant,
    ) : RealtimeBusEvent
}

internal class RealtimeEventBus {
    private val events = MutableSharedFlow<RealtimeBusEvent>()

    val stream: SharedFlow<RealtimeBusEvent> = events

    suspend fun publish(event: RealtimeBusEvent) {
        events.subscriptionCount.first { it > 0 }
        events.emit(event)
    }
}
