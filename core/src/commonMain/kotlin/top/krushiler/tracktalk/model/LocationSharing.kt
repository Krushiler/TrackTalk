package top.krushiler.tracktalk.model

import kotlinx.serialization.Serializable

@Serializable
data class LocationSharing(
    val friend: Friend,
    val locationShared: Boolean,
)
