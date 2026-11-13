package top.krushiler.tracktalk.data.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import top.krushiler.tracktalk.GmsJson

internal const val APP_BUILD_HEADER = "X-App-Build"
internal const val APP_OUTDATED_HEADER = "X-App-Outdated"

expect fun createHttpEngine(): HttpClientEngine

fun createHttpClient(
    baseUrl: String,
    appBuild: Int,
    accessToken: AccessTokenStore,
    appUpdate: AppUpdateState? = null,
): HttpClient = createHttpClient(createHttpEngine(), baseUrl, appBuild, accessToken, appUpdate)

internal fun createHttpClient(
    engine: HttpClientEngine,
    baseUrl: String,
    appBuild: Int,
    accessToken: AccessTokenStore,
    appUpdate: AppUpdateState? = null,
): HttpClient = HttpClient(engine) {
    configureTrackTalk(baseUrl, appBuild, accessToken, appUpdate)
}

private fun HttpClientConfig<*>.configureTrackTalk(
    baseUrl: String,
    appBuild: Int,
    accessToken: AccessTokenStore,
    appUpdate: AppUpdateState?,
) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(GmsJson)
    }
    install(WebSockets)
    if (appUpdate != null) {
        install(createClientPlugin("AppOutdated") {
            onResponse { response ->
                markOutdated(response.headers, appUpdate)
            }
        })
    }
    HttpResponseValidator {
        handleResponseExceptionWithRequest { cause, _ ->
            val response = (cause as? ResponseException)?.response ?: return@handleResponseExceptionWithRequest
            markOutdated(response.headers, appUpdate)
            val text = runCatching { response.bodyAsText() }.getOrDefault("")
            val parsed = runCatching { GmsJson.decodeFromString<ApiErrorBody>(text) }.getOrNull()
            throw ApiException(
                status = response.status,
                detail = parsed?.detail ?: text.ifBlank { response.status.description },
                minBuild = parsed?.minBuild,
            )
        }
    }
    defaultRequest {
        if (baseUrl.isNotBlank()) {
            url(baseUrl)
        }
        headers.append(APP_BUILD_HEADER, appBuild.toString())
        accessToken.token?.let { token ->
            headers.append(HttpHeaders.Authorization, "Bearer $token")
        }
    }
    installRefreshTokens(accessToken)
}

private fun markOutdated(headers: Headers, appUpdate: AppUpdateState?) {
    if (headers[APP_OUTDATED_HEADER].equals("true", ignoreCase = true)) {
        appUpdate?.markOutdated()
    }
}
