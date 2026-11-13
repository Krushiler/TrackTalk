package top.krushiler.tracktalk.feature.realtime

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import top.krushiler.tracktalk.GmsJson
import top.krushiler.tracktalk.ws.RealtimeEvent

internal fun interface ClientSocket {
    suspend fun send(text: String)
}

internal class RealtimeService {
    private val connections = ConcurrentHashMap<Long, MutableSet<ClientSocket>>()

    fun connect(userId: Long, socket: ClientSocket) {
        connections.compute(userId) { _, existing ->
            val set = existing ?: Collections.synchronizedSet(mutableSetOf())
            set.add(socket)
            set
        }
    }

    fun disconnect(userId: Long, socket: ClientSocket) {
        connections.computeIfPresent(userId) { _, set ->
            set.remove(socket)
            set.takeUnless { it.isEmpty() }
        }
    }

    fun isOnline(userId: Long): Boolean {
        val set = connections[userId] ?: return false
        synchronized(set) { return set.isNotEmpty() }
    }

    suspend fun send(userIds: Collection<Long>, event: RealtimeEvent) {
        sendRaw(
            userIds = userIds,
            json = GmsJson.encodeToString(RealtimeEvent.serializer(), event),
        )
    }

    suspend fun sendRaw(userIds: Collection<Long>, json: String) {
        val sockets = userIds.distinct().flatMap(::snapshot)
        for (socket in sockets) {
            try {
                socket.send(json)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                // The receiver may already be gone. Disconnect removes the socket.
            }
        }
    }

    private fun snapshot(userId: Long): List<ClientSocket> {
        val set = connections[userId] ?: return emptyList()
        synchronized(set) { return set.toList() }
    }
}
