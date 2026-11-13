package top.krushiler.tracktalk.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlin.time.Clock
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource
import tracktalk.app.shared.generated.resources.Res
import tracktalk.app.shared.generated.resources.days_ago
import tracktalk.app.shared.generated.resources.distance_kilometers
import tracktalk.app.shared.generated.resources.distance_meters
import tracktalk.app.shared.generated.resources.hours_ago
import tracktalk.app.shared.generated.resources.just_now
import tracktalk.app.shared.generated.resources.minutes_ago
import tracktalk.app.shared.generated.resources.user_fallback
import top.krushiler.tracktalk.geo.GeoPoint
import top.krushiler.tracktalk.geo.distanceMeters
import top.krushiler.tracktalk.presentation.theme.AppColors

fun personName(username: String, login: String): String {
    val name = username.trim()
    if (name.isNotEmpty()) return name
    return login.trim()
}

fun initialsOf(name: String): String {
    val parts = name.split(Regex("[\\s._-]+")).filter { it.isNotEmpty() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
        name.length >= 2 -> name.take(2).uppercase()
        name.isNotEmpty() -> name.take(1).uppercase()
        else -> "?"
    }
}

fun avatarColor(id: Long): Color =
    AppColors.avatarPalette[(id.mod(AppColors.avatarPalette.size.toLong())).toInt()]

@Composable
fun displayName(username: String, login: String, id: Long): String {
    val name = personName(username, login)
    return name.ifEmpty { stringResource(Res.string.user_fallback, id.toInt()) }
}

@Composable
fun relativeTime(instant: Instant): String {
    val elapsed = Clock.System.now() - instant
    val seconds = elapsed.inWholeSeconds
    if (seconds < 45) return stringResource(Res.string.just_now)
    val minutes = elapsed.inWholeMinutes
    if (minutes < 60) return stringResource(Res.string.minutes_ago, minutes.toInt())
    val hours = elapsed.inWholeHours
    if (hours < 24) return stringResource(Res.string.hours_ago, hours.toInt())
    return stringResource(Res.string.days_ago, elapsed.inWholeDays.toInt())
}

@Composable
fun distanceLabel(from: GeoPoint?, to: GeoPoint): String? {
    from ?: return null
    val meters = distanceMeters(from, to)
    return if (meters < 1000) {
        stringResource(Res.string.distance_meters, meters.toInt())
    } else {
        val kilometers = (meters / 100.0).toInt() / 10.0
        stringResource(Res.string.distance_kilometers, kilometers.toString())
    }
}
