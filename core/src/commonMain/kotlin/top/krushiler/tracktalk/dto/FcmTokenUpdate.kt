package top.krushiler.tracktalk.dto

import kotlinx.serialization.Serializable

@Serializable
data class FcmTokenUpdate(
    val token: String,
)
