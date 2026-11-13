package top.krushiler.tracktalk.feature.friendship

import top.krushiler.tracktalk.model.FriendshipStatus

internal data class FriendshipRecord(
    val id: Long,
    val userId: Long,
    val friendId: Long,
    val requestedByUserId: Long,
    val status: FriendshipStatus,
) {
    fun other(me: Long): Long = if (userId == me) friendId else userId
}
