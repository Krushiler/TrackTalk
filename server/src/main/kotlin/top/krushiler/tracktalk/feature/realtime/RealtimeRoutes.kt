package top.krushiler.tracktalk.feature.realtime

import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import top.krushiler.tracktalk.feature.auth.TokenService
import top.krushiler.tracktalk.feature.user.UserService
import top.krushiler.tracktalk.http.ApiException

internal fun Route.realtimeRoutes(
    realtime: RealtimeService,
    tokens: TokenService,
    users: UserService,
) {
    webSocket("/ws") {
        val userId = try {
            val token = call.request.queryParameters["token"]
            if (token.isNullOrBlank()) {
                throw ApiException(HttpStatusCode.Unauthorized, "Missing token")
            }
            val id = tokens.userId(token)
            users.require(id)
            id
        } catch (_: ApiException) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
            return@webSocket
        }
        val socket = ClientSocket { text -> send(Frame.Text(text)) }
        realtime.connect(userId, socket)
        try {
            for (frame in incoming) {
                frame.readBytes()
            }
        } finally {
            realtime.disconnect(userId, socket)
        }
    }
}
