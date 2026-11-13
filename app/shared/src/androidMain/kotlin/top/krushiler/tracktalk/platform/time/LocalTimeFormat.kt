package top.krushiler.tracktalk.platform.time

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatAndroidLocalTime(epochMillis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMillis))

fun formatAndroidLocalDateTime(epochMillis: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(epochMillis))
