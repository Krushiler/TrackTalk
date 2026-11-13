package top.krushiler.tracktalk.model

import kotlinx.serialization.Serializable

@Serializable
data class Friend(
    val id: Long,
    val login: String,
    val username: String,
    val avatarUrl: String? = null,
    val locationShared: Boolean = false,
)
