package top.krushiler.tracktalk.platform.permission

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val FineLocation = android.Manifest.permission.ACCESS_FINE_LOCATION
private const val CoarseLocation = android.Manifest.permission.ACCESS_COARSE_LOCATION
private const val BackgroundLocation = "android.permission.ACCESS_BACKGROUND_LOCATION"

lateinit var locationPermissions: LocationPermissions
    private set

fun installLocationPermissions(activity: ComponentActivity) {
    locationPermissions = LocationPermissions(activity)
}

class LocationPermissions(activity: ComponentActivity) {
    private var locationContinuation: ((Boolean) -> Unit)? = null
    private var backgroundContinuation: ((Boolean) -> Unit)? = null

    private val locationPermission = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        deliver(
            result[FineLocation] == true || result[CoarseLocation] == true,
            locationContinuation
        ) {
            locationContinuation = null
        }
    }

    private val backgroundPermission = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        deliver(granted, backgroundContinuation) { backgroundContinuation = null }
    }

    suspend fun requestWhileInUse(): Boolean = suspendCancellableCoroutine { continuation ->
        locationContinuation = { granted ->
            if (continuation.isActive) continuation.resume(granted)
        }
        continuation.invokeOnCancellation { locationContinuation = null }
        locationPermission.launch(arrayOf(FineLocation, CoarseLocation))
    }

    suspend fun requestBackground(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && suspendCancellableCoroutine { continuation ->
            backgroundContinuation = { granted ->
                if (continuation.isActive) continuation.resume(granted)
            }
            continuation.invokeOnCancellation { backgroundContinuation = null }
            backgroundPermission.launch(BackgroundLocation)
        }
    }
}

private fun deliver(granted: Boolean, resume: ((Boolean) -> Unit)?, clear: () -> Unit) {
    clear()
    resume?.invoke(granted)
}
