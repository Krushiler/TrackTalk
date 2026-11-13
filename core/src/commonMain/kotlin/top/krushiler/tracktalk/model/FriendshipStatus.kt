package top.krushiler.tracktalk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FriendshipStatus {
    @SerialName("pending")
    Pending,

    @SerialName("accepted")
    Accepted,
}
