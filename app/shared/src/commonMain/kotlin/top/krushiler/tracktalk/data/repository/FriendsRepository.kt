package top.krushiler.tracktalk.data.repository

import kotlin.time.Instant
import top.krushiler.tracktalk.data.network.HttpRoutes
import top.krushiler.tracktalk.dto.FriendRequestCreate
import top.krushiler.tracktalk.model.Friend
import top.krushiler.tracktalk.model.FriendLocation
import top.krushiler.tracktalk.model.LocationLogEntry

class FriendsRepository(
    private val http: HttpRoutes,
) {
    suspend fun friendLocations(): List<FriendLocation> = http.friendLocations()

    suspend fun friendLocation(userId: Long): FriendLocation = http.friendLocation(userId)

    suspend fun friends(): List<Friend> = http.friends()

    suspend fun incoming(): List<Friend> = http.incomingFriends().map { it.friend }

    suspend fun outgoing(): List<Friend> = http.outgoingFriends().map { it.friend }

    suspend fun sendRequest(friendLogin: String) {
        http.createFriendRequest(FriendRequestCreate(friendLogin))
    }

    suspend fun accept(userId: Long) {
        http.acceptFriend(userId)
    }

    suspend fun reject(userId: Long) {
        http.rejectFriend(userId)
    }

    suspend fun cancel(userId: Long) {
        http.cancelFriendRequest(userId)
    }

    suspend fun shareLocation(userId: Long) {
        http.shareLocation(userId)
    }

    suspend fun unshareLocation(userId: Long) {
        http.unshareLocation(userId)
    }

    suspend fun locationLog(userId: Long, from: Instant?, to: Instant?): List<LocationLogEntry> =
        http.friendLocationLog(userId, from, to)
}
