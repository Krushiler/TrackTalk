package top.krushiler.tracktalk.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import top.krushiler.tracktalk.dto.ChatMessageCreate
import top.krushiler.tracktalk.dto.FcmTokenUpdate
import top.krushiler.tracktalk.dto.FriendRequestCreate
import top.krushiler.tracktalk.dto.LoginRequest
import top.krushiler.tracktalk.dto.LoginResponse
import top.krushiler.tracktalk.dto.UsernameUpdate
import top.krushiler.tracktalk.model.ChatMessage
import top.krushiler.tracktalk.model.Friend
import top.krushiler.tracktalk.model.FriendLocation
import top.krushiler.tracktalk.model.Friendship
import top.krushiler.tracktalk.model.Location
import top.krushiler.tracktalk.model.LocationLogEntry
import top.krushiler.tracktalk.model.LocationSharing
import top.krushiler.tracktalk.model.User

@Serializable
data class RootResponse(val message: String)

@Serializable
data class HealthResponse(val status: String)

class HttpRoutes(
    private val http: HttpClient,
) {
    suspend fun root(): RootResponse = http.get("/").body()

    suspend fun health(): HealthResponse = http.get("/health").body()

    suspend fun login(request: LoginRequest): LoginResponse =
        http.post("/login") { json(request) }.body()

    suspend fun me(): User = http.get("/users/me").body()

    suspend fun updateUsername(request: UsernameUpdate): User =
        http.patch("/users/me") { json(request) }.body()

    suspend fun updateFcmToken(request: FcmTokenUpdate) {
        http.put("/users/me/fcm-token") { json(request) }
    }

    suspend fun updateAvatar(bytes: ByteArray, contentType: String): User =
        http.put("/users/me/avatar") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "file",
                            value = bytes,
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, contentType)
                                append(HttpHeaders.ContentDisposition, "filename=\"avatar\"")
                            },
                        )
                    },
                ),
            )
        }.body()

    suspend fun deleteAvatar(): User = http.delete("/users/me/avatar").body()

    suspend fun createFriendRequest(request: FriendRequestCreate): Friendship =
        http.post("/users/friend-request") { json(request) }.body()

    suspend fun friends(): List<Friend> = http.get("/users/friends").body()

    suspend fun incomingFriends(): List<Friendship> = http.get("/users/friends/incoming").body()

    suspend fun outgoingFriends(): List<Friendship> = http.get("/users/friends/outgoing").body()

    suspend fun friendInvitations(): List<Friendship> = http.get("/users/friend-invitations").body()

    suspend fun friendLocations(): List<FriendLocation> = http.get("/users/friends/locations").body()

    suspend fun friendLocation(userId: Long): FriendLocation =
        http.get("/users/friends/locations/$userId").body()

    suspend fun friendLocationLog(
        userId: Long,
        from: Instant? = null,
        to: Instant? = null,
    ): List<LocationLogEntry> =
        http.get("/users/friends/locations/$userId/log") {
            from?.let { parameter("from", it.toString()) }
            to?.let { parameter("to", it.toString()) }
        }.body()

    suspend fun postLocation(location: Location) {
        http.post("/users/location") { json(location) }
    }

    suspend fun locationLog(
        from: Instant? = null,
        to: Instant? = null,
    ): List<LocationLogEntry> =
        http.get("/users/location/log") {
            from?.let { parameter("from", it.toString()) }
            to?.let { parameter("to", it.toString()) }
        }.body()

    suspend fun message(messageId: Long): ChatMessage =
        http.get("/users/messages/$messageId").body()

    suspend fun acceptFriend(userId: Long): Friendship =
        http.post("/users/$userId/accept-friend").body()

    suspend fun rejectFriend(userId: Long) {
        http.post("/users/$userId/reject-friend")
    }

    suspend fun cancelFriendRequest(userId: Long) {
        http.post("/users/$userId/cancel-friend-request")
    }

    suspend fun messages(
        userId: Long,
        beforeId: Long? = null,
        limit: Int? = null,
    ): List<ChatMessage> =
        http.get("/users/$userId/messages") {
            beforeId?.let { parameter("before_id", it) }
            limit?.let { parameter("limit", it) }
        }.body()

    suspend fun sendMessage(userId: Long, request: ChatMessageCreate): ChatMessage =
        http.post("/users/$userId/messages") { json(request) }.body()

    suspend fun shareLocation(userId: Long): LocationSharing =
        http.post("/users/$userId/share-location").body()

    suspend fun unshareLocation(userId: Long) {
        http.post("/users/$userId/unshare-location")
    }
}

private fun io.ktor.client.request.HttpRequestBuilder.json(body: Any) {
    contentType(ContentType.Application.Json)
    setBody(body)
}
