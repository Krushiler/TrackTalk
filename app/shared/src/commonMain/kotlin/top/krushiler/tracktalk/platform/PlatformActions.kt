package top.krushiler.tracktalk.platform

expect fun openExternalUrl(url: String)

expect fun formatLocalTime(epochMillis: Long): String

expect fun formatLocalDateTime(epochMillis: Long): String

expect val isWebTarget: Boolean
