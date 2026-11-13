package top.krushiler.tracktalk

import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.withTimeout
import top.krushiler.tracktalk.config.AppConfig
import top.krushiler.tracktalk.dto.LoginResponse
import top.krushiler.tracktalk.dto.RefreshResponse
import top.krushiler.tracktalk.model.ChatMessage
import top.krushiler.tracktalk.http.ErrorResponse
import top.krushiler.tracktalk.http.UpdateRequiredResponse
import top.krushiler.tracktalk.ws.RealtimeEvent

class ApplicationTest {
    @Test
    fun rootAndHealthDoNotRequireAppBuild() = testApplication {
        application { module(testConfig) }

        val root = client.get("/")
        assertEquals(HttpStatusCode.OK, root.status)
        assertEquals("""{"message":"GMS Server"}""", root.bodyAsText())

        val health = client.get("/health")
        assertEquals(HttpStatusCode.OK, health.status)
        assertEquals("""{"status":"ok"}""", health.bodyAsText())
    }

    @Test
    fun loginRegistersThenRejectsAWrongPassword() = testApplication {
        application { module(testConfig) }

        val missingHeader = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"ada","password":"secret"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, missingHeader.status)
        assertEquals(
            "Missing X-App-Build header",
            GmsJson.decodeFromString<ErrorResponse>(missingHeader.bodyAsText()).detail,
        )

        val outdated = client.post("/login") {
            header("X-App-Build", "0")
            contentType(ContentType.Application.Json)
            setBody("""{"login":"ada","password":"secret"}""")
        }
        assertEquals(HttpStatusCode.UpgradeRequired, outdated.status)
        assertEquals(1, GmsJson.decodeFromString<UpdateRequiredResponse>(outdated.bodyAsText()).minBuild)

        val created = client.login("ada", "secret")
        assertTrue(created.isNewUser)
        assertEquals("ada", created.username)
        assertEquals("bearer", created.tokenType)
        assertTrue(created.refreshToken.isNotBlank())

        val again = client.login("ada", "secret")
        assertFalse(again.isNewUser)
        assertEquals(created.userId, again.userId)

        val rejected = client.post("/login") {
            gms()
            setBody("""{"login":"ada","password":"wrong"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, rejected.status)

        val me = client.get("/users/me") {
            gms(created.accessToken)
        }
        assertEquals(HttpStatusCode.OK, me.status)
        assertTrue(me.bodyAsText().contains("\"login\":\"ada\""))
    }

    @Test
    fun refreshTokenIssuesANewAccessTokenAndCannotAuthorizeRequests() = testApplication {
        application { module(testConfig) }
        val created = client.login("ada", "secret")

        val refreshed = client.post("/refresh") {
            gms()
            setBody("""{"refresh_token":"${created.refreshToken}"}""")
        }
        assertEquals(HttpStatusCode.OK, refreshed.status, refreshed.bodyAsText())
        val tokens = GmsJson.decodeFromString<RefreshResponse>(refreshed.bodyAsText())
        assertEquals("bearer", tokens.tokenType)
        assertTrue(tokens.accessToken.isNotBlank())
        assertTrue(tokens.refreshToken.isNotBlank())

        val me = client.get("/users/me") { gms(tokens.accessToken) }
        assertEquals(HttpStatusCode.OK, me.status)

        val refreshAsAccess = client.get("/users/me") { gms(created.refreshToken) }
        assertEquals(HttpStatusCode.Unauthorized, refreshAsAccess.status)

        val accessAsRefresh = client.post("/refresh") {
            gms()
            setBody("""{"refresh_token":"${created.accessToken}"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, accessAsRefresh.status)

        val rejected = client.post("/refresh") {
            gms()
            setBody("""{"refresh_token":"not-a-token"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, rejected.status)
    }

    @Test
    fun chatMessageIsDeliveredOnTheGmsWebSocket() = testApplication {
        application { module(testConfig) }
        val ada = client.login("ada", "secret")
        val grace = client.login("grace", "secret")

        val requested = client.post("/users/friend-request") {
            gms(ada.accessToken)
            setBody("""{"friend_login":"grace"}""")
        }
        assertEquals(HttpStatusCode.Created, requested.status)

        val accepted = client.post("/users/${ada.userId}/accept-friend") {
            gms(grace.accessToken)
        }
        assertEquals(HttpStatusCode.OK, accepted.status)

        val sockets = createClient {
            install(WebSockets)
        }
        sockets.webSocket(
            urlString = "/ws?token=${grace.accessToken}",
            request = { header("X-App-Build", "1") },
        ) {
            val sent = client.post("/users/${grace.userId}/messages") {
                gms(ada.accessToken)
                setBody("""{"body":"hello"}""")
            }
            assertEquals(HttpStatusCode.Created, sent.status)
            val message = GmsJson.decodeFromString<ChatMessage>(sent.bodyAsText())

            val frame = withTimeout(5_000) { incoming.receive() as Frame.Text }
            val event = GmsJson.decodeFromString(RealtimeEvent.serializer(), frame.readText())
            val chat = assertIs<RealtimeEvent.ChatMessage>(event)
            assertEquals(message.id, chat.data.messageId)
            assertEquals(ada.userId, chat.data.userId)
        }
    }
}

private val testConfig = AppConfig(jwtSecret = "test-secret", bcryptCost = 4)

private fun io.ktor.client.request.HttpRequestBuilder.gms(token: String? = null) {
    header("X-App-Build", "1")
    contentType(ContentType.Application.Json)
    if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
}

private suspend fun io.ktor.client.HttpClient.login(login: String, password: String): LoginResponse {
    val response = post("/login") {
        gms()
        setBody("""{"login":"$login","password":"$password"}""")
    }
    assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
    return GmsJson.decodeFromString(response.bodyAsText())
}
