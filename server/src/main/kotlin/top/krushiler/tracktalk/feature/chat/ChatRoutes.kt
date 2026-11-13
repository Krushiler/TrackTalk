package top.krushiler.tracktalk.feature.chat

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import top.krushiler.tracktalk.dto.ChatMessageCreate
import top.krushiler.tracktalk.http.limitQuery
import top.krushiler.tracktalk.http.longParam
import top.krushiler.tracktalk.http.optionalLongQuery
import top.krushiler.tracktalk.http.userId

internal fun Route.chatRoutes(chat: ChatService) {
    get("/messages/{messageId}") {
        call.respond(chat.get(call.userId(), call.longParam("messageId")))
    }
}

internal fun Route.chatByUserRoutes(chat: ChatService) {
    get("/{userId}/messages") {
        call.respond(
            chat.list(
                currentUserId = call.userId(),
                otherUserId = call.longParam("userId"),
                beforeId = call.optionalLongQuery("before_id"),
                limit = call.limitQuery(),
            ),
        )
    }
    post("/{userId}/messages") {
        val body = call.receive<ChatMessageCreate>()
        call.respond(
            HttpStatusCode.Created,
            chat.send(call.userId(), call.longParam("userId"), body.body),
        )
    }
}
