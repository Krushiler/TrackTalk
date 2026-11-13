package top.krushiler.tracktalk.platform

import kotlin.time.Instant
import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import top.krushiler.tracktalk.geo.GeoPoint

actual fun createDeviceLocation(): DeviceLocation = WasmDeviceLocation()

private class WasmDeviceLocation : DeviceLocation {
    override val permission: StateFlow<LocationAccess> = MutableStateFlow(LocationAccess.Unknown)
    override val fixes: Flow<GeoPoint> = emptyFlow()

    override suspend fun requestWhileInUse() = Unit

    override suspend fun requestAlways() = Unit

    override fun refresh() = Unit

    override fun stop() = Unit

    override fun openSettings() = Unit
}

actual fun createAvatarPicker(): AvatarPicker = object : AvatarPicker {
    override suspend fun pick(): AvatarPick = AvatarPick.Unsupported
}

actual fun openExternalUrl(url: String) {
    window.open(url, "_blank")
}

actual fun formatLocalTime(epochMillis: Long): String =
    Instant.fromEpochMilliseconds(epochMillis).toString().substringAfter('T').take(5)

actual fun formatLocalDateTime(epochMillis: Long): String =
    Instant.fromEpochMilliseconds(epochMillis).toString().take(16).replace('T', ' ')

actual val isWebTarget: Boolean = true

actual suspend fun startBackgroundLocationSync(token: String, baseUrl: String) = Unit

actual fun clearBackgroundLocationSync() = Unit

actual fun requestLocationQuickTile() = Unit
