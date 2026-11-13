package top.krushiler.tracktalk.platform.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import top.krushiler.tracktalk.geo.GeoPoint
import top.krushiler.tracktalk.platform.DeviceLocation
import top.krushiler.tracktalk.platform.LocationAccess
import top.krushiler.tracktalk.platform.context.androidAppContext
import top.krushiler.tracktalk.platform.permission.locationPermissions

private const val FineLocation = android.Manifest.permission.ACCESS_FINE_LOCATION
private const val CoarseLocation = android.Manifest.permission.ACCESS_COARSE_LOCATION
private const val BackgroundLocation = android.Manifest.permission.ACCESS_BACKGROUND_LOCATION

@SuppressLint("MissingPermission")
class AndroidDeviceLocation(
    private val context: Context = androidAppContext,
) : DeviceLocation {
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val access = MutableStateFlow(currentAccess())
    override val permission: StateFlow<LocationAccess> = access

    override val fixes: Flow<GeoPoint> = callbackFlow {
        if (!hasPermission()) {
            close()
            return@callbackFlow
        }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location.toPoint())
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit
        }
        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider == null) {
            close()
            return@callbackFlow
        }
        manager.getLastKnownLocation(provider)?.let { trySend(it.toPoint()) }
        manager.requestLocationUpdates(provider, 5_000L, 10f, listener, Looper.getMainLooper())
        awaitClose { manager.removeUpdates(listener) }
    }

    override suspend fun requestWhileInUse() {
        request()
    }

    override suspend fun requestAlways() {
        if (!hasPermission()) request()
        if (!hasPermission()) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || hasBackground()) {
            access.value = LocationAccess.Always
            return
        }
        val granted = locationPermissions.requestBackground()
        access.value = if (granted && hasBackground()) LocationAccess.Always else LocationAccess.WhileInUse
    }

    override fun refresh() {
        access.value = currentAccess()
    }

    override fun stop() = Unit

    override fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private suspend fun request() {
        if (!hasPermission()) {
            val granted = locationPermissions.requestWhileInUse()
            access.value = if (granted && hasPermission()) LocationAccess.WhileInUse else LocationAccess.Denied
        } else {
            access.value = LocationAccess.WhileInUse
        }
    }

    private fun hasPermission(): Boolean =
        context.checkSelfPermission(FineLocation) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(CoarseLocation) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun hasBackground(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            context.checkSelfPermission(BackgroundLocation) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun currentAccess(): LocationAccess = when {
        !hasPermission() -> LocationAccess.Unknown
        hasBackground() -> LocationAccess.Always
        else -> LocationAccess.WhileInUse
    }
}

private fun Location.toPoint() = GeoPoint(latitude, longitude)
