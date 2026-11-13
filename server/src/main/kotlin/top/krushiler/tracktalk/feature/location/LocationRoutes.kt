package top.krushiler.tracktalk.feature.location

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import top.krushiler.tracktalk.http.longParam
import top.krushiler.tracktalk.http.optionalInstant
import top.krushiler.tracktalk.http.userId
import top.krushiler.tracktalk.model.Location

internal fun Route.locationRoutes(locations: LocationService) {
    get("/friends/locations") {
        call.respond(locations.listFriends(call.userId()))
    }
    get("/friends/locations/{userId}") {
        call.respond(locations.friendLocation(call.userId(), call.longParam("userId")))
    }
    get("/friends/locations/{userId}/log") {
        call.respond(
            locations.friendLog(
                currentUserId = call.userId(),
                friendId = call.longParam("userId"),
                from = call.optionalInstant("from"),
                to = call.optionalInstant("to"),
            ),
        )
    }
    post("/location") {
        val body = call.receive<Location>()
        locations.post(call.userId(), body.latitude, body.longitude)
        call.respond(HttpStatusCode.NoContent)
    }
    get("/location/log") {
        call.respond(
            locations.ownLog(
                currentUserId = call.userId(),
                from = call.optionalInstant("from"),
                to = call.optionalInstant("to"),
            ),
        )
    }
}

internal fun Route.locationByUserRoutes(locations: LocationService) {
    post("/{userId}/share-location") {
        call.respond(locations.share(call.userId(), call.longParam("userId")))
    }
    post("/{userId}/unshare-location") {
        locations.unshare(call.userId(), call.longParam("userId"))
        call.respond(HttpStatusCode.NoContent)
    }
}
