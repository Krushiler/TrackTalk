package top.krushiler.tracktalk.feature.friendship

import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import top.krushiler.tracktalk.database.Friendships
import top.krushiler.tracktalk.database.GmsDatabase
import top.krushiler.tracktalk.database.asDbId
import top.krushiler.tracktalk.feature.location.locationViewerIds
import top.krushiler.tracktalk.feature.user.UserRecord
import top.krushiler.tracktalk.feature.user.findUser
import top.krushiler.tracktalk.feature.user.findUserByLogin
import top.krushiler.tracktalk.model.FriendshipStatus

internal class FriendshipRepository(private val database: GmsDatabase) {
    suspend fun request(currentUserId: Long, friendLogin: String): FriendRequest = database.transaction {
        val me = findUser(currentUserId) ?: return@transaction FriendRequest.MissingUser
        val target = findUserByLogin(friendLogin) ?: return@transaction FriendRequest.TargetMissing
        val (low, high) = ordered(me.id, target.id) ?: return@transaction FriendRequest.Self
        val existing = findFriendship(low, high)
        if (existing != null) {
            return@transaction when {
                existing.status == FriendshipStatus.Accepted -> FriendRequest.AlreadyFriends
                existing.requestedByUserId == me.id -> FriendRequest.AlreadySent
                else -> FriendRequest.IncomingExists
            }
        }
        val row = insertFriendship(
            userId = low,
            friendId = high,
            requestedByUserId = me.id,
            status = FriendshipStatus.Pending,
        )
        FriendRequest.Created(row, me, target)
    }

    suspend fun friends(currentUserId: Long): FriendList = database.transaction {
        val me = findUser(currentUserId) ?: return@transaction FriendList.MissingUser
        val sharedWith = locationViewerIds(me.id).toSet()
        val rows = listFriendships(me.id, FriendshipStatus.Accepted).mapNotNull { row ->
            val other = findUser(row.other(me.id)) ?: return@mapNotNull null
            FriendEntry(other, locationShared = other.id in sharedWith)
        }
        FriendList.Listed(rows)
    }

    suspend fun invitations(currentUserId: Long, sentByMe: Boolean): InvitationList = database.transaction {
        val me = findUser(currentUserId) ?: return@transaction InvitationList.MissingUser
        val rows = listFriendships(me.id, FriendshipStatus.Pending).mapNotNull { row ->
            val mine = row.requestedByUserId == me.id
            if (mine != sentByMe) return@mapNotNull null
            val otherId = if (sentByMe) row.other(me.id) else row.requestedByUserId
            val other = findUser(otherId) ?: return@mapNotNull null
            InvitationEntry(row, other)
        }
        InvitationList.Listed(rows)
    }

    suspend fun accept(currentUserId: Long, requesterId: Long): FriendAccept = database.transaction {
        val me = findUser(currentUserId) ?: return@transaction FriendAccept.MissingUser
        if (requesterId == me.id) return@transaction FriendAccept.InvalidUser
        val row = pendingFrom(me.id, requesterId) ?: return@transaction FriendAccept.NotFound
        val other = findUser(requesterId) ?: return@transaction FriendAccept.NotFound
        val saved = saveFriendship(row.copy(status = FriendshipStatus.Accepted))
        FriendAccept.Accepted(saved, me, other)
    }

    suspend fun reject(currentUserId: Long, requesterId: Long): FriendRemoval = database.transaction {
        val me = findUser(currentUserId) ?: return@transaction FriendRemoval.MissingUser
        if (requesterId == me.id) return@transaction FriendRemoval.InvalidUser
        val row = pendingFrom(me.id, requesterId) ?: return@transaction FriendRemoval.NotFound
        deleteFriendship(row.id)
        FriendRemoval.Deleted
    }

    suspend fun cancel(currentUserId: Long, targetId: Long): FriendRemoval = database.transaction {
        val me = findUser(currentUserId) ?: return@transaction FriendRemoval.MissingUser
        if (targetId == me.id) return@transaction FriendRemoval.InvalidUser
        val (low, high) = ordered(me.id, targetId) ?: return@transaction FriendRemoval.InvalidUser
        val row = findFriendship(low, high)
        if (row == null || row.status != FriendshipStatus.Pending || row.requestedByUserId != me.id) {
            return@transaction FriendRemoval.NotFound
        }
        deleteFriendship(row.id)
        FriendRemoval.Deleted
    }

    suspend fun accepted(currentUserId: Long, otherId: Long): AcceptedFriendship = database.transaction {
        val me = findUser(currentUserId) ?: return@transaction AcceptedFriendship.MissingUser
        val (low, high) = ordered(me.id, otherId) ?: return@transaction AcceptedFriendship.Self
        val row = findFriendship(low, high)
        if (row == null || row.status != FriendshipStatus.Accepted) {
            AcceptedFriendship.NotFriends
        } else {
            AcceptedFriendship.Friends
        }
    }
}

internal fun findFriendship(lowId: Long, highId: Long): FriendshipRecord? =
    Friendships.selectAll()
        .where {
            (Friendships.userId eq lowId.asDbId()) and (Friendships.friendId eq highId.asDbId())
        }
        .singleOrNull()
        ?.toFriendship()

internal fun listFriendships(userId: Long, status: FriendshipStatus): List<FriendshipRecord> {
    val id = userId.asDbId()
    return Friendships.selectAll()
        .where {
            ((Friendships.userId eq id) or (Friendships.friendId eq id)) and
                (Friendships.status eq status)
        }
        .orderBy(Friendships.id to SortOrder.ASC)
        .map { it.toFriendship() }
}

internal fun insertFriendship(
    userId: Long,
    friendId: Long,
    requestedByUserId: Long,
    status: FriendshipStatus,
): FriendshipRecord {
    val id = Friendships.insert {
        it[Friendships.userId] = userId.asDbId()
        it[Friendships.friendId] = friendId.asDbId()
        it[Friendships.requestedByUserId] = requestedByUserId.asDbId()
        it[Friendships.status] = status
        it[createdAt] = OffsetDateTime.now(ZoneOffset.UTC)
    } get Friendships.id
    return FriendshipRecord(
        id = id.toLong(),
        userId = userId,
        friendId = friendId,
        requestedByUserId = requestedByUserId,
        status = status,
    )
}

internal fun saveFriendship(record: FriendshipRecord): FriendshipRecord {
    val updated = Friendships.update({ Friendships.id eq record.id.asDbId() }) {
        it[status] = record.status
    }
    if (updated == 0) error("Friendship ${record.id} is not stored")
    return record
}

internal fun deleteFriendship(id: Long) {
    Friendships.deleteWhere { Friendships.id eq id.asDbId() }
}

private fun pendingFrom(me: Long, requesterId: Long): FriendshipRecord? {
    val (low, high) = ordered(me, requesterId) ?: return null
    val row = findFriendship(low, high)
    if (row == null || row.status != FriendshipStatus.Pending || row.requestedByUserId != requesterId) {
        return null
    }
    return row
}

private fun ordered(first: Long, second: Long): Pair<Long, Long>? {
    if (first == second) return null
    return if (first < second) first to second else second to first
}

private fun ResultRow.toFriendship(): FriendshipRecord = FriendshipRecord(
    id = this[Friendships.id].toLong(),
    userId = this[Friendships.userId].toLong(),
    friendId = this[Friendships.friendId].toLong(),
    requestedByUserId = this[Friendships.requestedByUserId].toLong(),
    status = this[Friendships.status],
)

internal data class FriendEntry(val user: UserRecord, val locationShared: Boolean)

internal data class InvitationEntry(val friendship: FriendshipRecord, val user: UserRecord)

internal sealed interface FriendRequest {
    data class Created(val friendship: FriendshipRecord, val me: UserRecord, val target: UserRecord) : FriendRequest
    data object MissingUser : FriendRequest
    data object TargetMissing : FriendRequest
    data object Self : FriendRequest
    data object AlreadyFriends : FriendRequest
    data object AlreadySent : FriendRequest
    data object IncomingExists : FriendRequest
}

internal sealed interface FriendList {
    data class Listed(val friends: List<FriendEntry>) : FriendList
    data object MissingUser : FriendList
}

internal sealed interface InvitationList {
    data class Listed(val invitations: List<InvitationEntry>) : InvitationList
    data object MissingUser : InvitationList
}

internal sealed interface FriendAccept {
    data class Accepted(val friendship: FriendshipRecord, val me: UserRecord, val other: UserRecord) : FriendAccept
    data object MissingUser : FriendAccept
    data object InvalidUser : FriendAccept
    data object NotFound : FriendAccept
}

internal sealed interface FriendRemoval {
    data object Deleted : FriendRemoval
    data object MissingUser : FriendRemoval
    data object InvalidUser : FriendRemoval
    data object NotFound : FriendRemoval
}

internal sealed interface AcceptedFriendship {
    data object Friends : AcceptedFriendship
    data object MissingUser : AcceptedFriendship
    data object Self : AcceptedFriendship
    data object NotFriends : AcceptedFriendship
}
