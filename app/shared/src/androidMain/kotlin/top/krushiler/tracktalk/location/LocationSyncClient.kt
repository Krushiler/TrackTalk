package top.krushiler.tracktalk.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

object LocationSyncClient {
    private const val LOCATION_TIMEOUT_MS = 30_000L
    const val MIN_DISTANCE_METERS = 100f

    suspend fun syncOnce(context: Context, token: String, baseUrl: String): Location? {
        if (!hasLocationPermission(context)) return null

        val location = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            getCurrentLocation(context)
        } ?: return null

        return if (syncLocation(context, token, baseUrl, location)) location else null
    }

    fun syncLocation(context: Context, token: String, baseUrl: String, location: Location): Boolean {
        return try {
            postLocation(context, baseUrl.trimEnd('/'), token, location)
        } catch (_: Exception) {
            false
        }
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(context: Context): Location? {
        val appContext = context.applicationContext
        val locationManager =
            appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                LocationManager.NETWORK_PROVIDER
            else -> null
        }

        if (provider == null) {
            return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return suspendCancellableCoroutine { cont ->
                locationManager.getCurrentLocation(
                    provider,
                    null,
                    appContext.mainExecutor,
                ) { location ->
                    if (cont.isActive) {
                        cont.resume(
                            location ?: locationManager.getLastKnownLocation(provider),
                        )
                    }
                }
            }
        }

        return suspendCancellableCoroutine { cont ->
            val resumed = AtomicBoolean(false)
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (resumed.compareAndSet(false, true)) {
                        locationManager.removeUpdates(this)
                        if (cont.isActive) cont.resume(location)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit

                override fun onProviderEnabled(provider: String) = Unit

                override fun onProviderDisabled(provider: String) {
                    if (resumed.compareAndSet(false, true)) {
                        locationManager.removeUpdates(this)
                        if (cont.isActive) {
                            cont.resume(locationManager.getLastKnownLocation(provider))
                        }
                    }
                }
            }

            cont.invokeOnCancellation {
                locationManager.removeUpdates(listener)
            }

            try {
                locationManager.requestLocationUpdates(
                    provider,
                    0L,
                    0f,
                    listener,
                    Looper.getMainLooper(),
                )
            } catch (_: Exception) {
                if (resumed.compareAndSet(false, true) && cont.isActive) {
                    cont.resume(locationManager.getLastKnownLocation(provider))
                }
            }
        }
    }

    private fun postLocation(context: Context, baseUrl: String, token: String, location: Location): Boolean {
        val url = URL("$baseUrl/users/location")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("X-App-Build", appBuild(context).toString())
        }

        return try {
            val body = JSONObject()
                .put("latitude", location.latitude)
                .put("longitude", location.longitude)
                .toString()

            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }

            connection.responseCode in 200..299
        } finally {
            connection.disconnect()
        }
    }

    @Suppress("DEPRECATION")
    private fun appBuild(context: Context): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            info.versionCode.toLong()
        }
    }
}
