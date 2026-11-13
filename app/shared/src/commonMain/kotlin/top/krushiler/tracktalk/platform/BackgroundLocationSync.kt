package top.krushiler.tracktalk.platform

expect suspend fun startBackgroundLocationSync(token: String, baseUrl: String)

expect fun clearBackgroundLocationSync()

expect fun requestLocationQuickTile()
