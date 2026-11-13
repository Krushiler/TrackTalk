package top.krushiler.tracktalk.http

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import org.koin.ktor.ext.inject
import top.krushiler.tracktalk.config.AppConfig

internal const val APP_BUILD_HEADER = "X-App-Build"

private val skipPaths = setOf("/", "/health")

internal fun Application.configureAppVersion() {
    val config by inject<AppConfig>()
    intercept(ApplicationCallPipeline.Plugins) {
        if (call.request.httpMethod == HttpMethod.Options || call.request.path() in skipPaths) {
            return@intercept
        }
        val header = call.request.headers[APP_BUILD_HEADER]
        if (header == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Missing $APP_BUILD_HEADER header"),
            )
            finish()
            return@intercept
        }
        val build = header.toIntOrNull()
        if (build == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Invalid $APP_BUILD_HEADER header"),
            )
            finish()
            return@intercept
        }
        if (build < config.minAppBuild) {
            call.respond(
                HttpStatusCode.UpgradeRequired,
                UpdateRequiredResponse(
                    detail = "App update required",
                    minBuild = config.minAppBuild,
                ),
            )
            finish()
        }
    }
}
