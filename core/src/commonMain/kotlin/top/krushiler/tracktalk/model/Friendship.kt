package top.krushiler.tracktalk.model

import kotlinx.serialization.Serializable

@Serializable
data class Friendship(
    val status: FriendshipStatus,
    val requestedByUserId: Long,
    val friend: Friend,
)
