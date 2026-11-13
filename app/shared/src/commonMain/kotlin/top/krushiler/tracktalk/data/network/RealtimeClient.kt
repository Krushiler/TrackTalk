package top.krushiler.tracktalk.data.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.krushiler.tracktalk.ws.RealtimeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RealtimeClient(
    private val sockets: WebSocketRoutes,
    private val accessToken: AccessTokenStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val eventsFlow = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RealtimeEvent> = eventsFlow.asSharedFlow()
    private var job: Job? = null

    fun ensureStarted() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                if (accessToken.token.isNullOrBlank()) {
                    delay(400.milliseconds)
                    continue
                }
                try {
                    sockets.events().collect { eventsFlow.emit(it) }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Throwable) {
                    delay(1.seconds)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
