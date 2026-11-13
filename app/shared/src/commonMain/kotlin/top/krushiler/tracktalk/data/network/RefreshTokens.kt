package top.krushiler.tracktalk.data.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.SendingRequest
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import top.krushiler.tracktalk.dto.RefreshRequest
import top.krushiler.tracktalk.dto.RefreshResponse

private val SkipRefresh = AttributeKey<Unit>("SkipRefreshToken")

internal fun HttpClientConfig<*>.installRefreshTokens(accessToken: AccessTokenStore) {
    val refreshMutex = Mutex()
    install(createClientPlugin("RefreshTokens") {
        val http = client
        on(SendingRequest) { request, _ ->
            if (request.attributes.contains(SkipRefresh)) {
                request.headers.remove(HttpHeaders.Authorization)
            }
        }
        on(Send) { request ->
            if (request.attributes.contains(SkipRefresh)) {
                return@on proceed(request)
            }

            val call = try {
                proceed(request)
            } catch (error: CancellationException) {
                throw error
            } catch (error: ResponseException) {
                if (!shouldRefresh(request, error.response.status, accessToken)) throw error
                return@on refreshAndRetry(http, accessToken, refreshMutex, request)
            }
            if (!shouldRefresh(request, call.response.status, accessToken)) {
                return@on call
            }
            try {
                call.body<ByteArray>()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                // Drop the unauthorized response and retry with a new access token.
            }
            refreshAndRetry(http, accessToken, refreshMutex, request)
        }
    })
}

private suspend fun Send.Sender.refreshAndRetry(
    http: HttpClient,
    accessToken: AccessTokenStore,
    refreshMutex: Mutex,
    request: HttpRequestBuilder,
): HttpClientCall {
    val failedAccessToken = request.bearerToken()
    refreshMutex.withLock {
        val current = accessToken.token
        if (failedAccessToken != null && !current.isNullOrBlank() && current != failedAccessToken) {
            return@withLock
        }
        val refreshToken = accessToken.refreshToken?.takeIf { it.isNotBlank() } ?: return@withLock
        val response = http.post("/refresh") {
            attributes.put(SkipRefresh, Unit)
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(refreshToken))
        }.body<RefreshResponse>()
        accessToken.update(response.accessToken, response.refreshToken)
    }
    request.headers.remove(HttpHeaders.Authorization)
    accessToken.token?.let { token ->
        request.headers.append(HttpHeaders.Authorization, "Bearer $token")
    }
    return proceed(request)
}

private fun shouldRefresh(
    request: HttpRequestBuilder,
    status: HttpStatusCode,
    accessToken: AccessTokenStore,
): Boolean {
    if (status != HttpStatusCode.Unauthorized) return false
    if (request.attributes.contains(SkipRefresh)) return false
    if (request.url.build().encodedPath.isCredentialExchange()) return false
    if (request.bearerToken() == null) return false
    return !accessToken.refreshToken.isNullOrBlank()
}

private fun HttpRequestBuilder.bearerToken(): String? {
    val header = headers[HttpHeaders.Authorization] ?: return null
    if (!header.startsWith("Bearer ")) return null
    return header.removePrefix("Bearer ").takeIf { it.isNotBlank() }
}

private fun String.isCredentialExchange(): Boolean {
    val path = trimEnd('/')
    return path.endsWith("/login") || path.endsWith("/refresh")
}
