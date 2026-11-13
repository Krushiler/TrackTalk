package top.krushiler.tracktalk.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.krushiler.tracktalk.app.shared.R

class LocationSyncForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    private var periodicSyncJob: Job? = null
    @Volatile private var lastSyncedLocation: Location? = null
    @Volatile private var lastSyncedAt: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = LocationSyncPreferences(this)
        if (!prefs.isEnabled || !prefs.hasCredentials()) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_REPOST_NOTIFICATION) {
            startAsForeground()
            return START_STICKY
        }

        startAsForeground()
        startLocationUpdates()
        return START_STICKY
    }

    override fun onDestroy() {
        stopLocationUpdates()
        serviceScope.cancel()
        LocationSyncTileService.requestUpdate(this)
        super.onDestroy()
    }

    @SuppressLint("InlinedApi")
    private fun startAsForeground() {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (locationListener != null) return
        if (!LocationSyncClient.hasLocationPermission(this)) {
            stopSelf()
            return
        }

        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager = manager

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                handleLocation(location)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit
        }
        locationListener = listener

        val providers = buildList {
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
        }

        if (providers.isEmpty()) {
            serviceScope.launch { syncCurrentLocation() }
            startPeriodicSync()
            return
        }

        for (provider in providers) {
            try {
                manager.requestLocationUpdates(
                    provider,
                    0L,
                    LocationSyncClient.MIN_DISTANCE_METERS,
                    listener,
                    Looper.getMainLooper(),
                )
            } catch (_: Exception) {
                // Provider may become unavailable; continue with others.
            }
        }

        serviceScope.launch { syncCurrentLocation() }
        startPeriodicSync()
    }

    private fun startPeriodicSync() {
        if (periodicSyncJob?.isActive == true) return

        periodicSyncJob = serviceScope.launch {
            while (isActive) {
                val lastAt = lastSyncedAt
                val wait = if (lastAt == 0L) {
                    0L
                } else {
                    (SYNC_INTERVAL_MS - (System.currentTimeMillis() - lastAt)).coerceAtLeast(0L)
                }
                delay(wait)
                if (!isActive) break
                syncCurrentLocation()
            }
        }
    }

    private suspend fun syncCurrentLocation() {
        syncWithCredentials { token, baseUrl ->
            val location = LocationSyncClient.syncOnce(
                context = this@LocationSyncForegroundService,
                token = token,
                baseUrl = baseUrl,
            )
            if (location != null) {
                recordSuccessfulSync(location)
            }
        }
    }

    private fun handleLocation(location: Location) {
        val previous = lastSyncedLocation
        if (previous != null && previous.distanceTo(location) < LocationSyncClient.MIN_DISTANCE_METERS) {
            return
        }

        serviceScope.launch {
            syncWithCredentials { token, baseUrl ->
                if (LocationSyncClient.syncLocation(this@LocationSyncForegroundService, token, baseUrl, location)) {
                    recordSuccessfulSync(location)
                }
            }
        }
    }

    private fun recordSuccessfulSync(location: Location) {
        lastSyncedLocation = location
        lastSyncedAt = System.currentTimeMillis()
    }

    private suspend fun syncWithCredentials(block: suspend (token: String, baseUrl: String) -> Unit) {
        val prefs = LocationSyncPreferences(this)
        if (!prefs.isEnabled) {
            stopSelf()
            return
        }

        val token = prefs.token
        val baseUrl = prefs.baseUrl
        if (token.isNullOrBlank() || baseUrl.isNullOrBlank()) {
            prefs.disable()
            stopSelf()
            return
        }

        block(token, baseUrl)
    }

    private fun stopLocationUpdates() {
        periodicSyncJob?.cancel()
        periodicSyncJob = null

        val listener = locationListener ?: return
        locationManager?.removeUpdates(listener)
        locationListener = null
        locationManager = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.location_sync_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.location_sync_channel_description)
            setShowBadge(false)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val repostIntent = Intent(this, LocationSyncForegroundService::class.java).apply {
            action = ACTION_REPOST_NOTIFICATION
        }
        val deletePendingIntent = PendingIntent.getService(
            this,
            1,
            repostIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.location_sync_notification_title))
            .setContentText(getString(R.string.location_sync_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setLargeIcon(BitmapFactory.decodeResource(resources, applicationInfo.icon))
            .setDeleteIntent(deletePendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        if (pendingIntent != null) notification.setContentIntent(pendingIntent)
        return notification.build()
    }

    companion object {
        private const val CHANNEL_ID = "location_sync"
        private const val NOTIFICATION_ID = 1001
        private const val SYNC_INTERVAL_MS = 15 * 60 * 1000L
        const val ACTION_REPOST_NOTIFICATION =
            "top.krushiler.tracktalk.action.REPOST_LOCATION_SYNC_NOTIFICATION"

        fun start(context: Context) {
            LocationSyncScheduler.enqueue(context)
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, LocationSyncForegroundService::class.java),
                )
            } catch (_: Exception) {
                // A background start can be rejected. The periodic worker still runs.
            }
            LocationSyncTileService.requestUpdate(context)
        }

        fun stop(context: Context) {
            LocationSyncPreferences(context).disable()
            LocationSyncScheduler.cancel(context)
            context.stopService(Intent(context, LocationSyncForegroundService::class.java))
            LocationSyncTileService.requestUpdate(context)
        }

        fun clear(context: Context) {
            LocationSyncPreferences(context).clear()
            LocationSyncScheduler.cancel(context)
            context.stopService(Intent(context, LocationSyncForegroundService::class.java))
            LocationSyncTileService.requestUpdate(context)
        }
    }
}
