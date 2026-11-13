@file:UseSerializers(IsoInstantSerializer::class)

package top.krushiler.tracktalk.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import top.krushiler.tracktalk.IsoInstantSerializer

@Serializable
data class LocationLogEntry(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val recordedAt: Instant,
)
