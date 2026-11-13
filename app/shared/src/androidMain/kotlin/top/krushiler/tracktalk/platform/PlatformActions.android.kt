package top.krushiler.tracktalk.platform

import top.krushiler.tracktalk.platform.time.formatAndroidLocalDateTime
import top.krushiler.tracktalk.platform.time.formatAndroidLocalTime
import top.krushiler.tracktalk.platform.url.openAndroidExternalUrl

actual fun openExternalUrl(url: String) = openAndroidExternalUrl(url)

actual fun formatLocalTime(epochMillis: Long): String = formatAndroidLocalTime(epochMillis)

actual fun formatLocalDateTime(epochMillis: Long): String = formatAndroidLocalDateTime(epochMillis)

actual val isWebTarget: Boolean = false
