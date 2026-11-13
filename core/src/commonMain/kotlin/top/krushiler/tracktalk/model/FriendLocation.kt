@file:UseSerializers(IsoInstantSerializer::class)

package top.krushiler.tracktalk.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import top.krushiler.tracktalk.IsoInstantSerializer

@Serializable
data class FriendLocation(
    val user: Friend,
    val latitude: Double,
    val longitude: Double,
    val updatedAt: Instant,
)
