package top.krushiler.tracktalk.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "bearer",
    val userId: Long,
    val login: String,
    val username: String,
    val avatarUrl: String? = null,
    val isNewUser: Boolean,
)
