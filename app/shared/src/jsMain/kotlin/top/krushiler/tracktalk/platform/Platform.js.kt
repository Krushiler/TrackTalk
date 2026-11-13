package top.krushiler.tracktalk.platform

import kotlin.coroutines.resume
import kotlin.js.Date
import kotlinx.browser.window
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import top.krushiler.tracktalk.geo.GeoPoint

actual fun createDeviceLocation(): DeviceLocation = JsDeviceLocation()

private class JsDeviceLocation : DeviceLocation {
    private val access = MutableStateFlow(LocationAccess.Unknown)
    override val permission: StateFlow<LocationAccess> = access

    override val fixes: Flow<GeoPoint> = callbackFlow {
        val geo = window.navigator.asDynamic().geolocation
        if (geo == null) {
            access.value = LocationAccess.Denied
            close()
            return@callbackFlow
        }
        val watchId = geo.watchPosition(
            { position: dynamic ->
                val coords = position.coords
                access.value = LocationAccess.WhileInUse
                trySend(GeoPoint(coords.latitude as Double, coords.longitude as Double))
            },
            { access.value = LocationAccess.Denied },
        )
        awaitClose { geo.clearWatch(watchId) }
    }

    override suspend fun requestWhileInUse() {
        request()
    }

    override suspend fun requestAlways() {
        request()
    }

    override fun refresh() = Unit

    override fun stop() = Unit

    override fun openSettings() = Unit

    private suspend fun request() {
        val geo = window.navigator.asDynamic().geolocation
        if (geo == null) {
            access.value = LocationAccess.Denied
            return
        }
        suspendCancellableCoroutine { continuation ->
            geo.getCurrentPosition(
                { position: dynamic ->
                    access.value = LocationAccess.WhileInUse
                    if (continuation.isActive) continuation.resume(Unit)
                },
                {
                    access.value = LocationAccess.Denied
                    if (continuation.isActive) continuation.resume(Unit)
                },
            )
        }
    }
}

actual fun createAvatarPicker(): AvatarPicker = object : AvatarPicker {
    override suspend fun pick(): AvatarPick = AvatarPick.Unsupported
}

actual fun openExternalUrl(url: String) {
    window.open(url, "_blank")
}

actual fun formatLocalTime(epochMillis: Long): String =
    Date(epochMillis.toDouble()).toLocaleTimeString()

actual fun formatLocalDateTime(epochMillis: Long): String =
    Date(epochMillis.toDouble()).toLocaleString()

actual val isWebTarget: Boolean = true

actual suspend fun startBackgroundLocationSync(token: String, baseUrl: String) = Unit

actual fun clearBackgroundLocationSync() = Unit

actual fun requestLocationQuickTile() = Unit
