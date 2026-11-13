package top.krushiler.tracktalk.dto

import kotlinx.serialization.Serializable

@Serializable
data class FriendRequestCreate(
    val friendLogin: String,
)
