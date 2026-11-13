package top.krushiler.tracktalk.presentation.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.krushiler.tracktalk.geo.GeoPoint

@Composable
actual fun TrackMap(
    camera: MapCamera,
    onCameraChange: (MapCamera) -> Unit,
    modifier: Modifier,
    satellite: Boolean,
    markers: List<MapMarker>,
    overlay: MapOverlay,
    move: Int,
    onMapClick: ((GeoPoint) -> Unit)?,
    markerContent: @Composable (MapMarker) -> Unit,
) {
    MapUnavailable(modifier)
}
