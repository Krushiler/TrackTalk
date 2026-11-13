package top.krushiler.tracktalk.http

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets

internal fun Application.configureWebSockets() {
    install(WebSockets)
}
