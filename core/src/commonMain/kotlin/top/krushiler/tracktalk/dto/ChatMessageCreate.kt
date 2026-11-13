package top.krushiler.tracktalk.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageCreate(
    val body: String,
)
