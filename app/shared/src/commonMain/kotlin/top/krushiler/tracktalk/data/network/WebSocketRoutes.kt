package top.krushiler.tracktalk.data.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.parameter
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import top.krushiler.tracktalk.GmsJson
import top.krushiler.tracktalk.ws.RealtimeEvent

class WebSocketRoutes(
    private val http: HttpClient,
    private val accessToken: AccessTokenStore,
) {
    fun events(token: String = accessToken.token ?: error("Not authenticated")): Flow<RealtimeEvent> = flow {
        http.webSocket(urlString = "/ws", request = {
            parameter("token", token)
        }) {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                this@flow.emit(GmsJson.decodeFromString(RealtimeEvent.serializer(), frame.readText()))
            }
        }
    }
}
