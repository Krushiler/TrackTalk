package top.krushiler.tracktalk.data.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import top.krushiler.tracktalk.dto.ChatMessageCreate
import top.krushiler.tracktalk.dto.LoginRequest
import top.krushiler.tracktalk.model.ChatMessage
import top.krushiler.tracktalk.model.Location

class HttpRoutesTest {
    @Test
    fun loginSendsCredentialsWithoutABearerToken() = runBlocking {
        val requests = mutableListOf<HttpRequestData>()
        val routes = routes(requests) {
            respond(
                content = """{"access_token":"tok","refresh_token":"refresh","token_type":"bearer","user_id":1,"login":"ada","username":"ada","avatar_url":null,"is_new_user":true}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }

        val response = routes.login(LoginRequest(login = "ada", password = "secret"))

        assertEquals("tok", response.accessToken)
        assertEquals("refresh", response.refreshToken)
        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/login", request.url.encodedPath)
        assertEquals("1", request.headers[APP_BUILD_HEADER])
        assertNull(request.headers[HttpHeaders.Authorization])
        assertEquals("""{"login":"ada","password":"secret"}""", request.body.asText())
    }

    @Test
    fun authenticatedRoutesSendTheStoredTokenAndQueryParameters() = runBlocking {
        val token = AccessTokenStore()
        token.update(access = "tok", refresh = null)
        val requests = mutableListOf<HttpRequestData>()
        val routes = routes(requests, token) { request ->
            when (request.url.encodedPath) {
                "/users/me" -> respond(
                    content = """{"id":1,"login":"ada","username":"ada","avatar_url":null}""",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders,
                )
                "/users/4/messages" -> respond(
                    content = "[]",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders,
                )
                "/users/location" -> respond(
                    content = "",
                    status = HttpStatusCode.NoContent,
                    headers = jsonHeaders,
                )
                else -> error(request.url.encodedPath)
            }
        }

        assertEquals("ada", routes.me().login)
        assertEquals(emptyList<ChatMessage>(), routes.messages(userId = 4, beforeId = 9, limit = 20))
        routes.postLocation(Location(latitude = 1.5, longitude = 2.5))

        assertEquals(HttpMethod.Get, requests[0].method)
        assertEquals("/users/me", requests[0].url.encodedPath)
        assertEquals("Bearer tok", requests[0].headers[HttpHeaders.Authorization])

        assertEquals("/users/4/messages", requests[1].url.encodedPath)
        assertEquals("9", requests[1].url.parameters["before_id"])
        assertEquals("20", requests[1].url.parameters["limit"])

        assertEquals(HttpMethod.Post, requests[2].method)
        assertEquals("/users/location", requests[2].url.encodedPath)
        assertEquals("""{"latitude":1.5,"longitude":2.5}""", requests[2].body.asText())
    }

    @Test
    fun locationLogUsesInstantQueryParameters() = runBlocking {
        val requests = mutableListOf<HttpRequestData>()
        val routes = routes(requests) {
            respond(content = "[]", status = HttpStatusCode.OK, headers = jsonHeaders)
        }
        val from = Instant.parse("2026-01-02T03:04:05Z")

        routes.friendLocationLog(userId = 8, from = from, to = null)

        val request = requests.single()
        assertEquals("/users/friends/locations/8/log", request.url.encodedPath)
        assertEquals(from.toString(), request.url.parameters["from"])
        assertNull(request.url.parameters["to"])
    }

    @Test
    fun messageSendUsesTheChatRoute() = runBlocking {
        val requests = mutableListOf<HttpRequestData>()
        val routes = routes(requests) {
            respond(
                content = """{"id":3,"sender_id":1,"recipient_id":4,"body":"hello","created_at":"2026-01-02T03:04:05Z"}""",
                status = HttpStatusCode.Created,
                headers = jsonHeaders,
            )
        }

        val message = routes.sendMessage(userId = 4, request = ChatMessageCreate(body = "hello"))

        assertEquals(3, message.id)
        assertEquals(HttpMethod.Post, requests.single().method)
        assertEquals("/users/4/messages", requests.single().url.encodedPath)
        assertEquals("""{"body":"hello"}""", requests.single().body.asText())
    }

    @Test
    fun errorResponsesExposeTheServerDetail() = runBlocking {
        val routes = routes(mutableListOf()) {
            respond(
                content = """{"detail":"App update required","min_build":4}""",
                status = HttpStatusCode.UpgradeRequired,
                headers = jsonHeaders,
            )
        }

        val error = assertFailsWith<ApiException> { routes.me() }
        assertEquals(HttpStatusCode.UpgradeRequired, error.status)
        assertEquals("App update required", error.detail)
        assertEquals(4, error.minBuild)
    }

    @Test
    fun expiredAccessTokenIsRefreshedAndTheOriginalCallIsRetried() = runBlocking {
        val store = AccessTokenStore()
        store.update(access = "expired", refresh = "refresh")
        val requests = mutableListOf<HttpRequestData>()
        val routes = routes(requests, store) { request ->
            when (request.url.encodedPath) {
                "/users/location" -> if (request.headers[HttpHeaders.Authorization] == "Bearer expired") {
                    respond(
                        content = """{"detail":"Invalid or expired token"}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = jsonHeaders,
                    )
                } else {
                    respond(content = "", status = HttpStatusCode.NoContent, headers = jsonHeaders)
                }
                "/refresh" -> respond(
                    content = """{"access_token":"fresh","refresh_token":"refresh-2","token_type":"bearer"}""",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders,
                )
                else -> error(request.url.encodedPath)
            }
        }

        routes.postLocation(Location(latitude = 1.5, longitude = 2.5))

        assertEquals(listOf("/users/location", "/refresh", "/users/location"), requests.map { it.url.encodedPath })
        assertEquals("""{"latitude":1.5,"longitude":2.5}""", requests[0].body.asText())
        assertEquals("""{"refresh_token":"refresh"}""", requests[1].body.asText())
        assertNull(requests[1].headers[HttpHeaders.Authorization])
        assertEquals("Bearer fresh", requests[2].headers[HttpHeaders.Authorization])
        assertEquals("""{"latitude":1.5,"longitude":2.5}""", requests[2].body.asText())
        assertEquals("fresh", store.token)
        assertEquals("refresh-2", store.refreshToken)
    }

    @Test
    fun requestFailsWhenRefreshFailsAndIsNotRetried() = runBlocking {
        val store = AccessTokenStore()
        store.update(access = "expired", refresh = "refresh")
        val requests = mutableListOf<HttpRequestData>()
        val routes = routes(requests, store) { request ->
            when (request.url.encodedPath) {
                "/users/me" -> respond(
                    content = """{"detail":"Invalid or expired token"}""",
                    status = HttpStatusCode.Unauthorized,
                    headers = jsonHeaders,
                )
                "/refresh" -> respond(
                    content = """{"detail":"Refresh rejected"}""",
                    status = HttpStatusCode.Unauthorized,
                    headers = jsonHeaders,
                )
                else -> error(request.url.encodedPath)
            }
        }

        val error = assertFailsWith<ApiException> { routes.me() }

        assertEquals(HttpStatusCode.Unauthorized, error.status)
        assertEquals("Refresh rejected", error.detail)
        assertEquals(listOf("/users/me", "/refresh"), requests.map { it.url.encodedPath })
        assertEquals("expired", store.token)
        assertEquals("refresh", store.refreshToken)
    }

    @Test
    fun unauthorizedCallWithoutARefreshTokenIsNotRetried() = runBlocking {
        val store = AccessTokenStore()
        store.update(access = "expired", refresh = null)
        val requests = mutableListOf<HttpRequestData>()
        val routes = routes(requests, store) {
            respond(
                content = """{"detail":"Invalid or expired token"}""",
                status = HttpStatusCode.Unauthorized,
                headers = jsonHeaders,
            )
        }

        val error = assertFailsWith<ApiException> { routes.me() }

        assertEquals("Invalid or expired token", error.detail)
        assertEquals(listOf("/users/me"), requests.map { it.url.encodedPath })
    }
}

private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

private fun routes(
    requests: MutableList<HttpRequestData>,
    accessToken: AccessTokenStore = AccessTokenStore(),
    handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData,
): HttpRoutes {
    val engine = MockEngine { request ->
        requests += request
        handler(request)
    }
    return HttpRoutes(createHttpClient(engine, baseUrl = "http://localhost", appBuild = 1, accessToken))
}

private fun OutgoingContent.asText(): String = when (this) {
    is TextContent -> text
    is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
    else -> error("Unexpected body ${this::class.simpleName}")
}
