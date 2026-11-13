package top.krushiler.tracktalk.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import top.krushiler.tracktalk.geo.GeoPoint

enum class LocationAccess {
    Unknown,
    Denied,
    WhileInUse,
    Always,
}

interface DeviceLocation {
    val permission: StateFlow<LocationAccess>
    val fixes: Flow<GeoPoint>
    suspend fun requestWhileInUse()
    suspend fun requestAlways()
    fun refresh()
    fun stop()
    fun openSettings()
}

expect fun createDeviceLocation(): DeviceLocation
