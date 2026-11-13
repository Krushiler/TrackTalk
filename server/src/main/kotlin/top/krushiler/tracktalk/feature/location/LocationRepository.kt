package top.krushiler.tracktalk.feature.location

import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import top.krushiler.tracktalk.database.GmsDatabase
import top.krushiler.tracktalk.database.LocationShares
import top.krushiler.tracktalk.database.UserLocationLogs
import top.krushiler.tracktalk.database.UserLocations
import top.krushiler.tracktalk.database.asDbId
import top.krushiler.tracktalk.database.toKotlinInstant
import top.krushiler.tracktalk.database.toOffsetDateTime
import top.krushiler.tracktalk.feature.friendship.findFriendship
import top.krushiler.tracktalk.feature.friendship.listFriendships
import top.krushiler.tracktalk.feature.user.UserRecord
import top.krushiler.tracktalk.feature.user.findUser
import top.krushiler.tracktalk.model.FriendshipStatus

internal class LocationRepository(private val database: GmsDatabase) {
    suspend fun save(userId: Long, latitude: Double, longitude: Double, updatedAt: Instant): PostedLocation =
        database.transaction {
            if (findUser(userId) == null) return@transaction PostedLocation.MissingUser
            saveLocation(
                LocationRecord(
                    userId = userId,
                    latitude = latitude,
                    longitude = longitude,
                    updatedAt = updatedAt,
                ),
            )
            insertLocationLog(userId, latitude, longitude, updatedAt)
            deleteLocationLogsBefore(userId, updatedAt - 7.days)
            PostedLocation.Saved(locationViewerIds(userId))
        }

    suspend fun friendLocations(currentUserId: Long): FriendLocations = database.transaction {
        val me = findUser(currentUserId) ?: return@transaction FriendLocations.MissingUser
        val friendIds = listFriendships(me.id, FriendshipStatus.Accepted).map { it.other(me.id) }.toSet()
        val sharedByMe = locationViewerIds(me.id).toSet()
        val rows = locationSharerIds(me.id).mapNotNull { sharerId ->
            if (sharerId !in friendIds) return@mapNotNull null
            val loc = findLocation(sharerId) ?: return@mapNotNull null
            val person = findUser(sharerId) ?: return@mapNotNull null
            FriendLocationRow(loc, person, locationShared = person.id in sharedByMe)
        }
        FriendLocations.Listed(rows)
    }

    suspend fun friendLocation(currentUserId: Long, friendId: Long): OneFriendLocation = database.transaction {
        when (val access = sharedAccess(currentUserId, friendId)) {
            SharedAccess.MissingUser -> return@transaction OneFriendLocation.MissingUser
            SharedAccess.Self -> return@transaction OneFriendLocation.Self
            SharedAccess.NotFriends -> return@transaction OneFriendLocation.NotFriends
            SharedAccess.NotShared -> return@transaction OneFriendLocation.NotShared
            SharedAccess.Allowed -> Unit
        }
        val loc = findLocation(friendId) ?: return@transaction OneFriendLocation.NoLocation
        val person = findUser(friendId) ?: return@transaction OneFriendLocation.UserMissing
        OneFriendLocation.Found(
            FriendLocationRow(
                location = loc,
                user = person,
                locationShared = isLocationShared(currentUserId, friendId),
            ),
        )
    }

    suspend fun ownLog(currentUserId: Long, from: Instant?, to: Instant?): LocationLogs = database.transaction {
        if (findUser(currentUserId) == null) return@transaction LocationLogs.MissingUser
        when (val logs = rangedLogs(currentUserId, from, to)) {
            null -> LocationLogs.InvalidRange
            else -> LocationLogs.Listed(logs)
        }
    }

    suspend fun friendLog(currentUserId: Long, friendId: Long, from: Instant?, to: Instant?): LocationLogs =
        database.transaction {
            when (val access = sharedAccess(currentUserId, friendId)) {
                SharedAccess.MissingUser -> return@transaction LocationLogs.MissingUser
                SharedAccess.Self -> return@transaction LocationLogs.Self
                SharedAccess.NotFriends -> return@transaction LocationLogs.NotFriends
                SharedAccess.NotShared -> return@transaction LocationLogs.NotShared
                SharedAccess.Allowed -> Unit
            }
            when (val logs = rangedLogs(friendId, from, to)) {
                null -> LocationLogs.InvalidRange
                else -> LocationLogs.Listed(logs)
            }
        }

    suspend fun share(currentUserId: Long, friendId: Long): LocationShare = database.transaction {
        val me = findUser(currentUserId) ?: return@transaction LocationShare.MissingUser
        val other = findUser(friendId) ?: return@transaction LocationShare.FriendMissing
        val already = isLocationShared(me.id, friendId)
        if (!already) insertLocationShare(me.id, friendId)
        LocationShare.Shared(
            ShareWrite(
                me = me,
                other = other,
                created = !already,
                location = findLocation(me.id),
            ),
        )
    }

    suspend fun unshare(currentUserId: Long, friendId: Long): LocationUnshare = database.transaction {
        if (findUser(currentUserId) == null) return@transaction LocationUnshare.MissingUser
        LocationUnshare.Done(deleteLocationShare(currentUserId, friendId))
    }
}

internal fun findLocation(userId: Long): LocationRecord? =
    UserLocations.selectAll()
        .where { UserLocations.userId eq userId.asDbId() }
        .singleOrNull()
        ?.toLocation()

internal fun saveLocation(record: LocationRecord) {
    val id = record.userId.asDbId()
    val updated = UserLocations.update({ UserLocations.userId eq id }) {
        it[latitude] = record.latitude
        it[longitude] = record.longitude
        it[updatedAt] = record.updatedAt.toOffsetDateTime()
    }
    if (updated == 0) {
        UserLocations.insert {
            it[userId] = id
            it[latitude] = record.latitude
            it[longitude] = record.longitude
            it[updatedAt] = record.updatedAt.toOffsetDateTime()
        }
    }
}

internal fun insertLocationLog(
    userId: Long,
    latitude: Double,
    longitude: Double,
    recordedAt: Instant,
): LocationLogRecord {
    val id = UserLocationLogs.insert {
        it[UserLocationLogs.userId] = userId.asDbId()
        it[UserLocationLogs.latitude] = latitude
        it[UserLocationLogs.longitude] = longitude
        it[UserLocationLogs.recordedAt] = recordedAt.toOffsetDateTime()
    } get UserLocationLogs.id
    return LocationLogRecord(
        id = id.toLong(),
        userId = userId,
        latitude = latitude,
        longitude = longitude,
        recordedAt = recordedAt,
    )
}

internal fun deleteLocationLogsBefore(userId: Long, before: Instant) {
    UserLocationLogs.deleteWhere {
        (UserLocationLogs.userId eq userId.asDbId()) and
            (UserLocationLogs.recordedAt less before.toOffsetDateTime())
    }
}

internal fun listLocationLogs(userId: Long, from: Instant, to: Instant?): List<LocationLogRecord> {
    val id = userId.asDbId()
    return UserLocationLogs.selectAll()
        .where {
            val owned = UserLocationLogs.userId eq id
            val since = UserLocationLogs.recordedAt greaterEq from.toOffsetDateTime()
            if (to == null) {
                owned and since
            } else {
                owned and since and (UserLocationLogs.recordedAt lessEq to.toOffsetDateTime())
            }
        }
        .orderBy(UserLocationLogs.recordedAt to SortOrder.ASC)
        .map { it.toLog() }
}

internal fun locationViewerIds(sharerId: Long): List<Long> =
    LocationShares.selectAll()
        .where { LocationShares.sharerId eq sharerId.asDbId() }
        .map { it[LocationShares.viewerId].toLong() }

internal fun locationSharerIds(viewerId: Long): List<Long> =
    LocationShares.selectAll()
        .where { LocationShares.viewerId eq viewerId.asDbId() }
        .map { it[LocationShares.sharerId].toLong() }

internal fun isLocationShared(sharerId: Long, viewerId: Long): Boolean =
    LocationShares.selectAll()
        .where {
            (LocationShares.sharerId eq sharerId.asDbId()) and
                (LocationShares.viewerId eq viewerId.asDbId())
        }
        .empty()
        .not()

internal fun insertLocationShare(sharerId: Long, viewerId: Long) {
    if (isLocationShared(sharerId, viewerId)) return
    LocationShares.insert {
        it[LocationShares.sharerId] = sharerId.asDbId()
        it[LocationShares.viewerId] = viewerId.asDbId()
        it[createdAt] = OffsetDateTime.now(ZoneOffset.UTC)
    }
}

internal fun deleteLocationShare(sharerId: Long, viewerId: Long): Boolean =
    LocationShares.deleteWhere {
        (LocationShares.sharerId eq sharerId.asDbId()) and
            (LocationShares.viewerId eq viewerId.asDbId())
    } > 0

private fun sharedAccess(viewerId: Long, sharerId: Long): SharedAccess {
    if (findUser(viewerId) == null) return SharedAccess.MissingUser
    if (sharerId == viewerId) return SharedAccess.Self
    val (low, high) = if (viewerId < sharerId) viewerId to sharerId else sharerId to viewerId
    val row = findFriendship(low, high)
    if (row == null || row.status != FriendshipStatus.Accepted) return SharedAccess.NotFriends
    if (!isLocationShared(sharerId, viewerId)) return SharedAccess.NotShared
    return SharedAccess.Allowed
}

private fun rangedLogs(userId: Long, from: Instant?, to: Instant?): List<LocationLogRecord>? {
    val floor = Clock.System.now() - 7.days
    var since = from ?: floor
    if (since < floor) since = floor
    if (to != null && to < since) return null
    return listLocationLogs(userId, since, to)
}

private enum class SharedAccess {
    Allowed,
    MissingUser,
    Self,
    NotFriends,
    NotShared,
}

private fun ResultRow.toLocation(): LocationRecord = LocationRecord(
    userId = this[UserLocations.userId].toLong(),
    latitude = this[UserLocations.latitude],
    longitude = this[UserLocations.longitude],
    updatedAt = this[UserLocations.updatedAt].toKotlinInstant(),
)

private fun ResultRow.toLog(): LocationLogRecord = LocationLogRecord(
    id = this[UserLocationLogs.id].toLong(),
    userId = this[UserLocationLogs.userId].toLong(),
    latitude = this[UserLocationLogs.latitude],
    longitude = this[UserLocationLogs.longitude],
    recordedAt = this[UserLocationLogs.recordedAt].toKotlinInstant(),
)

internal data class FriendLocationRow(
    val location: LocationRecord,
    val user: UserRecord,
    val locationShared: Boolean,
)

internal data class ShareWrite(
    val me: UserRecord,
    val other: UserRecord,
    val created: Boolean,
    val location: LocationRecord?,
)

internal sealed interface PostedLocation {
    data class Saved(val viewers: List<Long>) : PostedLocation
    data object MissingUser : PostedLocation
}

internal sealed interface FriendLocations {
    data class Listed(val rows: List<FriendLocationRow>) : FriendLocations
    data object MissingUser : FriendLocations
}

internal sealed interface OneFriendLocation {
    data class Found(val row: FriendLocationRow) : OneFriendLocation
    data object MissingUser : OneFriendLocation
    data object Self : OneFriendLocation
    data object NotFriends : OneFriendLocation
    data object NotShared : OneFriendLocation
    data object NoLocation : OneFriendLocation
    data object UserMissing : OneFriendLocation
}

internal sealed interface LocationLogs {
    data class Listed(val records: List<LocationLogRecord>) : LocationLogs
    data object MissingUser : LocationLogs
    data object Self : LocationLogs
    data object NotFriends : LocationLogs
    data object NotShared : LocationLogs
    data object InvalidRange : LocationLogs
}

internal sealed interface LocationShare {
    data class Shared(val write: ShareWrite) : LocationShare
    data object MissingUser : LocationShare
    data object FriendMissing : LocationShare
}

internal sealed interface LocationUnshare {
    data class Done(val removed: Boolean) : LocationUnshare
    data object MissingUser : LocationUnshare
}
