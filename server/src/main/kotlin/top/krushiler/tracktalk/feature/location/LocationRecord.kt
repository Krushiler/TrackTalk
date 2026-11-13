package top.krushiler.tracktalk.feature.location

import kotlin.time.Instant

internal data class LocationRecord(
    val userId: Long,
    val latitude: Double,
    val longitude: Double,
    val updatedAt: Instant,
)

internal data class LocationLogRecord(
    val id: Long,
    val userId: Long,
    val latitude: Double,
    val longitude: Double,
    val recordedAt: Instant,
)
