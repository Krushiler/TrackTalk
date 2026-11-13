package top.krushiler.tracktalk.location

import android.app.Activity
import android.app.PendingIntent
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.service.quicksettings.PendingIntentActivityWrapper
import androidx.core.service.quicksettings.TileServiceCompat
import top.krushiler.tracktalk.app.shared.R

class LocationSyncTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        val prefs = LocationSyncPreferences(this)
        if (prefs.isEnabled) {
            LocationSyncForegroundService.stop(this)
            updateTileState()
            return
        }

        if (!prefs.hasCredentials()) {
            unlockAndRun { openApp() }
            return
        }

        prefs.isEnabled = true
        LocationSyncForegroundService.start(this)
        updateTileState()
    }

    private fun openApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        TileServiceCompat.startActivityAndCollapse(
            this,
            PendingIntentActivityWrapper(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT,
                false,
            ),
        )
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val enabled = LocationSyncPreferences(this).isEnabled
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.location_sync_tile_label)
        tile.updateTile()
    }

    companion object {
        fun requestUpdate(context: Context) {
            val component = ComponentName(context, LocationSyncTileService::class.java)
            requestListeningState(context, component)
        }

        fun requestAdd(activity: Activity) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
            val prefs = LocationSyncPreferences(activity)
            if (prefs.hasRequestedAddTile) return
            val statusBarManager = activity.getSystemService(StatusBarManager::class.java) ?: return
            prefs.hasRequestedAddTile = true
            statusBarManager.requestAddTileService(
                ComponentName(activity, LocationSyncTileService::class.java),
                activity.getString(R.string.location_sync_tile_label),
                Icon.createWithResource(activity, R.drawable.ic_location_sync_tile),
                activity.mainExecutor,
            ) { }
        }
    }
}
