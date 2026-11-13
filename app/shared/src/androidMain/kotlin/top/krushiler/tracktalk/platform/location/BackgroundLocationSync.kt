package top.krushiler.tracktalk.platform.location

import top.krushiler.tracktalk.location.LocationSyncForegroundService
import top.krushiler.tracktalk.location.LocationSyncPreferences
import top.krushiler.tracktalk.location.LocationSyncTileService
import top.krushiler.tracktalk.platform.context.androidActivity
import top.krushiler.tracktalk.platform.context.androidAppContext
import top.krushiler.tracktalk.platform.permission.notificationPermissions

suspend fun startAndroidBackgroundLocationSync(token: String, baseUrl: String) {
    if (token.isBlank() || baseUrl.isBlank()) return
    notificationPermissions.request()
    LocationSyncPreferences(androidAppContext).saveAndEnable(token, baseUrl)
    LocationSyncForegroundService.start(androidAppContext)
}

fun clearAndroidBackgroundLocationSync() {
    LocationSyncForegroundService.clear(androidAppContext)
}

fun requestAndroidLocationTile() {
    LocationSyncTileService.requestAdd(androidActivity)
}
