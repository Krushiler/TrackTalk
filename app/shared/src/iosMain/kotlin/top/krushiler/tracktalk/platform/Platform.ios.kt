package top.krushiler.tracktalk.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLLocationAccuracyNearestTenMeters
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.NSObject
import top.krushiler.tracktalk.geo.GeoPoint

actual fun createDeviceLocation(): DeviceLocation = IosDeviceLocation()

private class IosDeviceLocation : DeviceLocation {
    private val manager = CLLocationManager()
    private val access = MutableStateFlow(LocationAccess.Unknown)
    private val authorizationDelegate = AuthorizationDelegate { publishAuthorization() }
    override val permission: StateFlow<LocationAccess> = access

    init {
        manager.delegate = authorizationDelegate
        manager.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
        publishAuthorization()
    }

    override val fixes: Flow<GeoPoint> = callbackFlow {
        val listener = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
                trySend(location.toPoint())
            }
        }
        manager.delegate = listener
        manager.startUpdatingLocation()
        awaitClose {
            manager.stopUpdatingLocation()
            manager.delegate = authorizationDelegate
        }
    }

    override suspend fun requestWhileInUse() {
        manager.requestWhenInUseAuthorization()
    }

    override suspend fun requestAlways() {
        manager.requestAlwaysAuthorization()
    }

    override fun refresh() {
        publishAuthorization()
    }

    override fun stop() {
        manager.stopUpdatingLocation()
    }

    override fun openSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>(), null)
    }

    private fun publishAuthorization() {
        access.value = when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedAlways -> LocationAccess.Always
            kCLAuthorizationStatusAuthorizedWhenInUse -> LocationAccess.WhileInUse
            kCLAuthorizationStatusDenied -> LocationAccess.Denied
            else -> LocationAccess.Unknown
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class AuthorizationDelegate(
    private val onChange: () -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {
    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        onChange()
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) = Unit
}

@OptIn(ExperimentalForeignApi::class)
private fun CLLocation.toPoint(): GeoPoint {
    return coordinate.useContents {
        GeoPoint(latitude, longitude)
    }
}

actual fun createAvatarPicker(): AvatarPicker = object : AvatarPicker {
    override suspend fun pick(): AvatarPick = AvatarPick.Unsupported
}

actual fun openExternalUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl, emptyMap<Any?, Any>(), null)
}

actual fun formatLocalTime(epochMillis: Long): String = format(epochMillis, "h:mm a")

actual fun formatLocalDateTime(epochMillis: Long): String = format(epochMillis, "MMM d, h:mm a")

actual val isWebTarget: Boolean = false

actual suspend fun startBackgroundLocationSync(token: String, baseUrl: String) = Unit

actual fun clearBackgroundLocationSync() = Unit

actual fun requestLocationQuickTile() = Unit

private fun format(epochMillis: Long, pattern: String): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = pattern
    val date = NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0)
    return formatter.stringFromDate(date).orEmpty()
}
