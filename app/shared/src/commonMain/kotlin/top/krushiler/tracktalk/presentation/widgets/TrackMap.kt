package top.krushiler.tracktalk.presentation.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import kotlin.math.pow
import org.jetbrains.compose.resources.stringResource
import tracktalk.app.shared.generated.resources.Res
import tracktalk.app.shared.generated.resources.map_unavailable
import top.krushiler.tracktalk.geo.GeoPoint
import top.krushiler.tracktalk.geo.WebMercator
import top.krushiler.tracktalk.presentation.theme.AppColors

data class MapCamera(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double,
)

data class MapMarker(
    val id: Long,
    val position: GeoPoint,
)

data class MapOverlay(
    val polyline: List<GeoPoint> = emptyList(),
    val dots: List<GeoPoint> = emptyList(),
    val selected: GeoPoint? = null,
    val self: GeoPoint? = null,
)

fun fitCamera(points: List<GeoPoint>, widthPx: Float, heightPx: Float): MapCamera {
    if (points.isEmpty()) return MapCamera(37.7749, -122.4194, 13.0)
    if (points.size == 1) return MapCamera(points.first().latitude, points.first().longitude, 15.0)
    val centerLat = (points.minOf { it.latitude } + points.maxOf { it.latitude }) / 2
    val centerLon = (points.minOf { it.longitude } + points.maxOf { it.longitude }) / 2
    var zoom = 16.0
    while (zoom > 3.0) {
        val z = zoom.toInt().coerceIn(2, 18)
        val scale = 2.0.pow(zoom - z)
        val xs = points.map { WebMercator.worldX(it.longitude, z.toDouble()) }
        val ys = points.map { WebMercator.worldY(it.latitude, z.toDouble()) }
        val width = (xs.max() - xs.min()) * scale
        val height = (ys.max() - ys.min()) * scale
        if (width < widthPx * 0.75f && height < heightPx * 0.65f) break
        zoom -= 0.25
    }
    return MapCamera(centerLat, centerLon, zoom)
}

@Composable
expect fun TrackMap(
    camera: MapCamera,
    onCameraChange: (MapCamera) -> Unit,
    modifier: Modifier = Modifier,
    satellite: Boolean = false,
    markers: List<MapMarker> = emptyList(),
    overlay: MapOverlay = MapOverlay(),
    move: Int = 0,
    onMapClick: ((GeoPoint) -> Unit)? = null,
    markerContent: @Composable (MapMarker) -> Unit = {},
)

@Composable
internal fun MapUnavailable(modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            stringResource(Res.string.map_unavailable),
            color = AppColors.muted,
            fontSize = 14.sp,
        )
    }
}
