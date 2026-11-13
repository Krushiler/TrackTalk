package top.krushiler.tracktalk.http

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable

@Serializable
internal data class MessageResponse(val message: String)

@Serializable
internal data class HealthResponse(val status: String)

internal fun Route.healthRoutes() {
    get("/") {
        call.respond(MessageResponse("GMS Server"))
    }
    get("/health") {
        call.respond(HealthResponse("ok"))
    }
}
