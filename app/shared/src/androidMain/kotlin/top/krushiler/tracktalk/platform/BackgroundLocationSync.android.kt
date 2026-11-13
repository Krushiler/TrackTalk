package top.krushiler.tracktalk.platform

import top.krushiler.tracktalk.platform.location.clearAndroidBackgroundLocationSync
import top.krushiler.tracktalk.platform.location.requestAndroidLocationTile
import top.krushiler.tracktalk.platform.location.startAndroidBackgroundLocationSync

actual suspend fun startBackgroundLocationSync(token: String, baseUrl: String) {
    startAndroidBackgroundLocationSync(token, baseUrl)
}

actual fun clearBackgroundLocationSync() {
    clearAndroidBackgroundLocationSync()
}

actual fun requestLocationQuickTile() {
    requestAndroidLocationTile()
}
