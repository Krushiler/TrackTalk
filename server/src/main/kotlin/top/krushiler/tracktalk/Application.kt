package top.krushiler.tracktalk

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import top.krushiler.tracktalk.config.AppConfig
import top.krushiler.tracktalk.di.serverModules
import top.krushiler.tracktalk.feature.realtime.RealtimeHandler
import top.krushiler.tracktalk.http.configureAppVersion
import top.krushiler.tracktalk.http.configureAuthentication
import top.krushiler.tracktalk.http.configureCors
import top.krushiler.tracktalk.http.configureRouting
import top.krushiler.tracktalk.http.configureSerialization
import top.krushiler.tracktalk.http.configureStatusPages
import top.krushiler.tracktalk.http.configureWebSockets

fun main() {
    val config = AppConfig.fromEnvironment()
    embeddedServer(Netty, port = config.port, host = config.host) {
        module(config)
    }.start(wait = true)
}

fun Application.module(config: AppConfig = AppConfig.fromEnvironment()) {
    install(Koin) {
        slf4jLogger()
        modules(serverModules(config))
    }
    val realtimeHandler by inject<RealtimeHandler>()
    launch { realtimeHandler.start() }
    configureSerialization()
    configureStatusPages()
    configureCors()
    configureAppVersion()
    configureAuthentication()
    configureWebSockets()
    configureRouting()
}
