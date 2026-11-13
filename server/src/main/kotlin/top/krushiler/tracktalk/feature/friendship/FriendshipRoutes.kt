package top.krushiler.tracktalk.feature.friendship

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import top.krushiler.tracktalk.dto.FriendRequestCreate
import top.krushiler.tracktalk.http.longParam
import top.krushiler.tracktalk.http.userId

internal fun Route.friendshipRoutes(friendships: FriendshipService) {
    post("/friend-request") {
        val body = call.receive<FriendRequestCreate>()
        call.respond(
            HttpStatusCode.Created,
            friendships.request(call.userId(), body.friendLogin),
        )
    }
    get("/friends") {
        call.respond(friendships.listFriends(call.userId()))
    }
    get("/friends/incoming") {
        call.respond(friendships.incoming(call.userId()))
    }
    get("/friends/outgoing") {
        call.respond(friendships.outgoing(call.userId()))
    }
    get("/friend-invitations") {
        call.respond(friendships.incoming(call.userId()))
    }
}

internal fun Route.friendshipByUserRoutes(friendships: FriendshipService) {
    post("/{userId}/accept-friend") {
        call.respond(friendships.accept(call.userId(), call.longParam("userId")))
    }
    post("/{userId}/reject-friend") {
        friendships.reject(call.userId(), call.longParam("userId"))
        call.respond(HttpStatusCode.NoContent)
    }
    post("/{userId}/cancel-friend-request") {
        friendships.cancel(call.userId(), call.longParam("userId"))
        call.respond(HttpStatusCode.NoContent)
    }
}
