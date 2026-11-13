package top.krushiler.tracktalk.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val prefs = LocationSyncPreferences(context)
        if (prefs.isEnabled && prefs.hasCredentials()) {
            LocationSyncForegroundService.start(context)
        }
    }
}
