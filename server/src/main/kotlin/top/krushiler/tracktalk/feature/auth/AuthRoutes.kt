package top.krushiler.tracktalk.feature.auth

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import top.krushiler.tracktalk.dto.LoginRequest
import top.krushiler.tracktalk.dto.RefreshRequest

internal fun Route.authRoutes(auth: AuthService) {
    post("/login") {
        call.respond(auth.login(call.receive<LoginRequest>()))
    }
    post("/refresh") {
        call.respond(auth.refresh(call.receive<RefreshRequest>()))
    }
}
