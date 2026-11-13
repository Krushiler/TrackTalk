package top.krushiler.tracktalk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import top.krushiler.tracktalk.dto.ChatMessageCreate
import top.krushiler.tracktalk.dto.FcmTokenUpdate
import top.krushiler.tracktalk.dto.FriendRequestCreate
import top.krushiler.tracktalk.dto.LoginRequest
import top.krushiler.tracktalk.dto.LoginResponse
import top.krushiler.tracktalk.dto.RefreshRequest
import top.krushiler.tracktalk.dto.UsernameUpdate
import top.krushiler.tracktalk.model.ChatMessage
import top.krushiler.tracktalk.model.Friend
import top.krushiler.tracktalk.model.Friendship
import top.krushiler.tracktalk.model.FriendshipStatus
import top.krushiler.tracktalk.model.LocationSharing
import top.krushiler.tracktalk.model.User
import top.krushiler.tracktalk.ws.ChatMessageReceivedData
import top.krushiler.tracktalk.ws.FriendLocationUpdateData
import top.krushiler.tracktalk.ws.RealtimeEvent

class GmsContractTest {

    @Test
    fun loginResponseUsesSnakeCaseAndKeepsBearerDefault() {
        val response = LoginResponse(
            accessToken = "tok",
            refreshToken = "refresh",
            userId = 7,
            login = "ada@example.com",
            username = "ada",
            avatarUrl = null,
            isNewUser = true,
        )

        val encoded = GmsJson.encodeToString(response)
        val json = GmsJson.parseToJsonElement(encoded).jsonObject

        assertEquals("tok", json.getValue("access_token").jsonPrimitive.content)
        assertEquals("refresh", json.getValue("refresh_token").jsonPrimitive.content)
        assertEquals("bearer", json.getValue("token_type").jsonPrimitive.content)
        assertEquals("7", json.getValue("user_id").jsonPrimitive.content)
        assertEquals("true", json.getValue("is_new_user").jsonPrimitive.content)
        assertTrue(json.containsKey("avatar_url"))
        assertFalse(json.containsKey("accessToken"))
        assertFalse(json.containsKey("refreshToken"))
        assertEquals(response, GmsJson.decodeFromString<LoginResponse>(encoded))
    }

    @Test
    fun requestBodiesUseSnakeCase() {
        assertEquals(
            """{"login":"ada","password":"secret"}""",
            GmsJson.encodeToString(LoginRequest(login = "ada", password = "secret")),
        )
        assertEquals(
            """{"refresh_token":"refresh"}""",
            GmsJson.encodeToString(RefreshRequest(refreshToken = "refresh")),
        )
        assertEquals(
            """{"username":"ada"}""",
            GmsJson.encodeToString(UsernameUpdate(username = "ada")),
        )
        assertEquals(
            """{"token":"fcm"}""",
            GmsJson.encodeToString(FcmTokenUpdate(token = "fcm")),
        )
        assertEquals(
            """{"friend_login":"grace"}""",
            GmsJson.encodeToString(FriendRequestCreate(friendLogin = "grace")),
        )
        assertEquals(
            """{"body":"hello"}""",
            GmsJson.encodeToString(ChatMessageCreate(body = "hello")),
        )
    }

    @Test
    fun userOmitsLocationSharedWhileFriendDefaultsIt() {
        val user = User(id = 1, login = "ada", username = "ada", avatarUrl = null)
        val userJson = GmsJson.encodeToString(user)
        assertFalse(userJson.contains("location_shared"))

        val friend = GmsJson.decodeFromString<Friend>(userJson)
        assertEquals(false, friend.locationShared)
        assertEquals(user.id, friend.id)

        val encodedFriend = GmsJson.encodeToString(
            friend.copy(locationShared = true, avatarUrl = "https://cdn/a.png"),
        )
        val json = GmsJson.parseToJsonElement(encodedFriend).jsonObject
        assertEquals("true", json.getValue("location_shared").jsonPrimitive.content)
        assertEquals("https://cdn/a.png", json.getValue("avatar_url").jsonPrimitive.content)
        assertFalse(json.containsKey("locationShared"))
    }

    @Test
    fun chatMessageDecodesOffsetTimestampAndNullSender() {
        val json = """
            {
              "id": 3,
              "sender_id": 1,
              "recipient_id": 2,
              "body": "hi",
              "created_at": "2024-05-01T10:15:30+00:00",
              "sender": null
            }
        """.trimIndent()

        val message = GmsJson.decodeFromString<ChatMessage>(json)

        assertEquals(3, message.id)
        assertEquals(Instant.parse("2024-05-01T10:15:30Z"), message.createdAt)
        assertNull(message.sender)

        val withSender = message.copy(
            sender = User(id = 1, login = "ada", username = "ada"),
        )
        assertEquals(withSender, GmsJson.decodeFromString<ChatMessage>(GmsJson.encodeToString(withSender)))
    }

    @Test
    fun friendshipAndLocationSharingRoundTrip() {
        val sharing = LocationSharing(
            friend = Friend(
                id = 2,
                login = "grace",
                username = "grace",
                avatarUrl = null,
                locationShared = true,
            ),
            locationShared = true,
        )
        val friendship = Friendship(
            status = FriendshipStatus.Pending,
            requestedByUserId = 1,
            friend = sharing.friend,
        )

        val friendshipJson = GmsJson.parseToJsonElement(GmsJson.encodeToString(friendship)).jsonObject
        assertEquals("pending", friendshipJson.getValue("status").jsonPrimitive.content)
        assertEquals("1", friendshipJson.getValue("requested_by_user_id").jsonPrimitive.content)
        assertEquals(friendship, GmsJson.decodeFromString<Friendship>(GmsJson.encodeToString(friendship)))

        val sharingJson = GmsJson.parseToJsonElement(GmsJson.encodeToString(sharing)).jsonObject
        assertEquals("true", sharingJson.getValue("location_shared").jsonPrimitive.content)
        assertEquals(sharing, GmsJson.decodeFromString<LocationSharing>(GmsJson.encodeToString(sharing)))
    }

    @Test
    fun realtimeEventsRoundTripKnownTypesAndKeepUnknownPayloads() {
        val location = RealtimeEvent.FriendLocationUpdate(
            FriendLocationUpdateData(userId = 4, latitude = 1.5, longitude = 2.5),
        )
        val locationJson = GmsJson.parseToJsonElement(
            GmsJson.encodeToString(RealtimeEvent.serializer(), location),
        ).jsonObject
        assertEquals("friend_location_update", locationJson.getValue("type").jsonPrimitive.content)
        assertEquals("4", locationJson.getValue("data").jsonObject.getValue("user_id").jsonPrimitive.content)
        assertEquals(
            location,
            GmsJson.decodeFromString(
                RealtimeEvent.serializer(),
                GmsJson.encodeToString(RealtimeEvent.serializer(), location),
            ),
        )

        val chat = RealtimeEvent.ChatMessage(
            ChatMessageReceivedData(
                messageId = 9,
                userId = 4,
                createdAt = Instant.parse("2024-05-01T10:15:30Z"),
            ),
        )
        val wire = """
            {"type":"chat_message","data":{"message_id":9,"user_id":4,"created_at":"2024-05-01T10:15:30+00:00"}}
        """.trimIndent()
        assertEquals(chat, GmsJson.decodeFromString<RealtimeEvent>(wire))

        val unknown = GmsJson.decodeFromString<RealtimeEvent>(
            """{"type":"location_share","data":{"user_id":4}}""",
        )
        val unknownEvent = assertIs<RealtimeEvent.Unknown>(unknown)
        assertEquals("location_share", unknownEvent.type)
        assertEquals(
            unknownEvent,
            GmsJson.decodeFromString(
                RealtimeEvent.serializer(),
                GmsJson.encodeToString(RealtimeEvent.serializer(), unknownEvent),
            ),
        )
    }
}
