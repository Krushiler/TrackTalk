package top.krushiler.tracktalk.feature.friendship

import io.ktor.http.HttpStatusCode
import top.krushiler.tracktalk.feature.push.PushService
import top.krushiler.tracktalk.feature.user.Profiles
import top.krushiler.tracktalk.http.ApiException
import top.krushiler.tracktalk.model.Friend
import top.krushiler.tracktalk.model.Friendship

internal class FriendshipService(
    private val friendships: FriendshipRepository,
    private val profiles: Profiles,
    private val push: PushService,
) {
    suspend fun request(currentUserId: Long, friendLogin: String): Friendship {
        if (friendLogin.isEmpty() || friendLogin.length > 320) {
            throw ApiException(HttpStatusCode.BadRequest, "Invalid friend login")
        }
        val outcome = when (val request = friendships.request(currentUserId, friendLogin)) {
            FriendRequest.MissingUser -> throw ApiException(HttpStatusCode.Unauthorized, "User not found")
            FriendRequest.TargetMissing ->
                throw ApiException(HttpStatusCode.NotFound, "User with this login does not exist")
            FriendRequest.Self -> throw ApiException(HttpStatusCode.BadRequest, "Cannot friend yourself")
            FriendRequest.AlreadyFriends -> throw ApiException(HttpStatusCode.Conflict, "Already friends")
            FriendRequest.AlreadySent ->
                throw ApiException(HttpStatusCode.Conflict, "Friend request already sent")
            FriendRequest.IncomingExists -> throw ApiException(
                HttpStatusCode.Conflict,
                "This user has already sent you a friend request; accept or reject it",
            )
            is FriendRequest.Created -> request
        }
        push.send(
            userId = outcome.target.id,
            title = "Friend request",
            body = "${outcome.me.username} sent you a friend request",
            data = mapOf(
                "type" to "friend_request",
                "user_id" to outcome.me.id.toString(),
            ),
        )
        return friendship(outcome.friendship, profiles.friend(outcome.target))
    }

    suspend fun listFriends(currentUserId: Long): List<Friend> {
        val rows = when (val listed = friendships.friends(currentUserId)) {
            FriendList.MissingUser -> throw ApiException(HttpStatusCode.Unauthorized, "User not found")
            is FriendList.Listed -> listed.friends
        }
        return rows.map { profiles.friend(it.user, it.locationShared) }
    }

    suspend fun incoming(currentUserId: Long): List<Friendship> = pending(currentUserId, sentByMe = false)

    suspend fun outgoing(currentUserId: Long): List<Friendship> = pending(currentUserId, sentByMe = true)

    suspend fun accept(currentUserId: Long, requesterId: Long): Friendship {
        val outcome = when (val accepted = friendships.accept(currentUserId, requesterId)) {
            FriendAccept.MissingUser -> throw ApiException(HttpStatusCode.Unauthorized, "User not found")
            FriendAccept.InvalidUser -> throw ApiException(HttpStatusCode.BadRequest, "Invalid user")
            FriendAccept.NotFound ->
                throw ApiException(HttpStatusCode.NotFound, "No pending invitation from this user")
            is FriendAccept.Accepted -> accepted
        }
        push.send(
            userId = requesterId,
            title = "Friend request accepted",
            body = "${outcome.me.username} accepted your friend request",
            data = mapOf(
                "type" to "friend_accept",
                "user_id" to outcome.me.id.toString(),
            ),
        )
        return friendship(outcome.friendship, profiles.friend(outcome.other))
    }

    suspend fun reject(currentUserId: Long, requesterId: Long) {
        when (friendships.reject(currentUserId, requesterId)) {
            FriendRemoval.Deleted -> Unit
            FriendRemoval.MissingUser -> throw ApiException(HttpStatusCode.Unauthorized, "User not found")
            FriendRemoval.InvalidUser -> throw ApiException(HttpStatusCode.BadRequest, "Invalid user")
            FriendRemoval.NotFound ->
                throw ApiException(HttpStatusCode.NotFound, "No pending invitation from this user")
        }
    }

    suspend fun cancel(currentUserId: Long, targetId: Long) {
        when (friendships.cancel(currentUserId, targetId)) {
            FriendRemoval.Deleted -> Unit
            FriendRemoval.MissingUser -> throw ApiException(HttpStatusCode.Unauthorized, "User not found")
            FriendRemoval.InvalidUser -> throw ApiException(HttpStatusCode.BadRequest, "Invalid user")
            FriendRemoval.NotFound ->
                throw ApiException(HttpStatusCode.NotFound, "No pending friend request to this user")
        }
    }

    suspend fun requireAccepted(currentUserId: Long, otherId: Long) {
        when (friendships.accepted(currentUserId, otherId)) {
            AcceptedFriendship.Friends -> Unit
            AcceptedFriendship.MissingUser -> throw ApiException(HttpStatusCode.Unauthorized, "User not found")
            AcceptedFriendship.Self -> throw ApiException(HttpStatusCode.BadRequest, "Cannot friend yourself")
            AcceptedFriendship.NotFriends ->
                throw ApiException(HttpStatusCode.NotFound, "Not friends with this user")
        }
    }

    private suspend fun pending(currentUserId: Long, sentByMe: Boolean): List<Friendship> {
        val rows = when (val listed = friendships.invitations(currentUserId, sentByMe)) {
            InvitationList.MissingUser -> throw ApiException(HttpStatusCode.Unauthorized, "User not found")
            is InvitationList.Listed -> listed.invitations
        }
        return rows.map { friendship(it.friendship, profiles.friend(it.user)) }
    }

    private fun friendship(row: FriendshipRecord, friend: Friend): Friendship = Friendship(
        status = row.status,
        requestedByUserId = row.requestedByUserId,
        friend = friend,
    )
}
