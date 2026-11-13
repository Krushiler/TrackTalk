package top.krushiler.tracktalk.dto

import kotlinx.serialization.Serializable

@Serializable
data class RefreshResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "bearer",
)
