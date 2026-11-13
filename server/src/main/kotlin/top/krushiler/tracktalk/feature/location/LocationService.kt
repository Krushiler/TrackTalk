package top.krushiler.tracktalk.feature.location

import io.ktor.http.HttpStatusCode
import kotlin.time.Clock
import kotlin.time.Instant
import top.krushiler.tracktalk.feature.friendship.FriendshipService
import top.krushiler.tracktalk.feature.push.PushService
import top.krushiler.tracktalk.feature.realtime.RealtimeBusEvent
import top.krushiler.tracktalk.feature.realtime.RealtimeEventBus
import top.krushiler.tracktalk.feature.user.Profiles
import top.krushiler.tracktalk.http.ApiException
import top.krushiler.tracktalk.model.FriendLocation
import top.krushiler.tracktalk.model.LocationLogEntry
import top.krushiler.tracktalk.model.LocationSharing

internal class LocationService(
    private val locations: LocationRepository,
    private val profiles: Profiles,
    private val friendships: FriendshipService,
    private val events: RealtimeEventBus,
    private val push: PushService,
) {
    suspend fun post(currentUserId: Long, latitude: Double, longitude: Double) {
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            throw ApiException(HttpStatusCode.BadRequest, "Invalid coordinates")
        }
        val viewers = when (val posted = locations.save(currentUserId, latitude, longitude, Clock.System.now())) {
            PostedLocation.MissingUser -> throw ApiException(HttpStatusCode.Unauthorized, "User not found")
            is PostedLocation.Saved -> posted.viewers
        }
        if (viewers.isEmpty()) return
        events.publish(
            RealtimeBusEvent.FriendLocationUpdated(
                recipients = viewers,
                userId = currentUserId,
                latitude = latitude,
                longitude = longitude,
            ),
        )
    }

    suspend fun listFriends(currentUserId: Long): List<FriendLocation> {
        val rows = when (val listed = locations.friendLocations(currentUserId)) {
            FriendLocations.MissingUser -> throw ApiException(HttpStatusCode.Unauthorized, "User not found")
            is FriendLocations.Listed -> listed.rows
        }
        return rows.map { row ->
            FriendLocation(
                user = profiles.friend(row.user, row.locationShared),
                latitude = row.location.latitude,
                longitude = row.location.longitude,
                updatedAt = row.location.updatedAt,
            )
        }
    }

    suspend fun friendLocation(currentUserId: Long, friendId: Long): FriendLocation {
        val row = when (val found = locations.friendLocation(currentUserId, friendId)) {
            OneFriendLocation.MissingUser -> throw ApiException(HttpStatusCode.Unauthorized, "User not found")
            OneFriendLocation.Self ->
                throw ApiException(HttpStatusCode.BadRequest, "Cannot fetch your own location this way")
            OneFriendLocation.NotFriends ->
                throw ApiException(HttpStatusCode.NotFound, "Not friends with this user")
            OneFriendLocation.NotShared ->
                throw ApiException(HttpStatusCode.NotFound, "Location is not shared with you")
            OneFriendLocation.NoLocation -> throw ApiException(HttpStatusCode.NotFound, "Location not found")
            OneFriendLocation.UserMissing -> throw ApiException(HttpStatusCode.NotFound, "User not found")
            is OneFriendLocation.Found -> found.row
        }
        return FriendLocation(
            user = profiles.friend(row.user, locationShared = row.locationShared),
            latitude = row.location.latitude,
            longitude = row.location.longitude,
            updatedAt = row.location.updatedAt,
        )
    }

    suspend fun ownLog(currentUserId: Long, from: Instant?, to: Instant?): List<LocationLogEntry> =
        logs(locations.ownLog(currentUserId, from, to))

    suspend fun friendLog(
        currentUserId: Long,
        friendId: Long,
        from: Instant?,
        to: Instant?,
    ): List<LocationLogEntry> = logs(locations.friendLog(currentUserId, friendId, from, to))

    suspend fun share(currentUserId: Long, friendId: Long): LocationSharing {
        if (friendId == currentUserId) {
            throw ApiException(HttpStatusCode.BadRequest, "Cannot share location with yourself")
        }
        friendships.requireAccepted(currentUserId, friendId)
        val created = when (val shared = locations.share(currentUserId, friendId)) {
            LocationShare.MissingUser -> throw ApiException(HttpStatusCode.Unauthorized, "User not found")
            LocationShare.FriendMissing -> throw ApiException(HttpStatusCode.NotFound, "User not found")
            is LocationShare.Shared -> shared.write
        }
        if (created.created) {
            push.send(
                userId = friendId,
                title = "Location sharing",
                body = "${created.me.username} started sharing their location with you",
                data = mapOf(
                    "type" to "location_share",
                    "user_id" to created.me.id.toString(),
                ),
            )
        }
        val loc = created.location
        if (loc != null) {
            events.publish(
                RealtimeBusEvent.FriendLocationUpdated(
                    recipients = listOf(friendId),
                    userId = currentUserId,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                ),
            )
        }
        return LocationSharing(
            friend = profiles.friend(created.other, locationShared = true),
            locationShared = true,
        )
    }

    suspend fun unshare(currentUserId: Long, friendId: Long) {
        if (friendId == currentUserId) {
            throw ApiException(HttpStatusCode.BadRequest, "Cannot unshare location with yourself")
        }
        friendships.requireAccepted(currentUserId, friendId)
        val removed = when (val result = locations.unshare(currentUserId, friendId)) {
            LocationUnshare.MissingUser -> throw ApiException(HttpStatusCode.Unauthorized, "User not found")
            is LocationUnshare.Done -> result.removed
        }
        if (!removed) return
        events.publish(
            RealtimeBusEvent.FriendLocationStopped(
                recipientId = friendId,
                userId = currentUserId,
            ),
        )
    }

    private suspend fun logs(result: LocationLogs): List<LocationLogEntry> {
        val records = when (result) {
            LocationLogs.MissingUser -> throw ApiException(HttpStatusCode.Unauthorized, "User not found")
            LocationLogs.Self ->
                throw ApiException(HttpStatusCode.BadRequest, "Cannot fetch your own location this way")
            LocationLogs.NotFriends -> throw ApiException(HttpStatusCode.NotFound, "Not friends with this user")
            LocationLogs.NotShared ->
                throw ApiException(HttpStatusCode.NotFound, "Location is not shared with you")
            LocationLogs.InvalidRange ->
                throw ApiException(HttpStatusCode.BadRequest, "'from' must be before or equal to 'to'")
            is LocationLogs.Listed -> result.records
        }
        return records.map { it.toEntry() }
    }

    private fun LocationLogRecord.toEntry(): LocationLogEntry = LocationLogEntry(
        id = id,
        latitude = latitude,
        longitude = longitude,
        recordedAt = recordedAt,
    )
}
