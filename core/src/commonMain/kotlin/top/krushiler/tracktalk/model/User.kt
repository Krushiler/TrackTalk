package top.krushiler.tracktalk.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long,
    val login: String,
    val username: String,
    val avatarUrl: String? = null,
)
