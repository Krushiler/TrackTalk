package top.krushiler.tracktalk.http

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import top.krushiler.tracktalk.GmsJson

internal fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(GmsJson)
    }
}
