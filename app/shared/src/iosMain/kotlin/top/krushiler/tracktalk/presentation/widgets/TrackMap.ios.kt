package top.krushiler.tracktalk.presentation.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.CameraConstraints
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.overlay.MapOverlay as LibraryMapOverlay
import org.maplibre.compose.overlay.include
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import top.krushiler.tracktalk.geo.GeoPoint
import top.krushiler.tracktalk.presentation.theme.AppColors

private val StreetStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty")

private val SatelliteStyle = BaseStyle.Json(
    """
    {
      "version": 8,
      "sources": {
        "satellite": {
          "type": "raster",
          "tileSize": 256,
          "attribution": "Esri",
          "tiles": [
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
          ]
        }
      },
      "layers": [
        { "id": "satellite", "type": "raster", "source": "satellite" }
      ]
    }
    """.trimIndent(),
)

private fun GeoPoint.toPosition(): Position = Position(longitude = longitude, latitude = latitude)

private fun points(points: List<GeoPoint>) =
    FeatureCollection(points.map { Feature(Point(it.toPosition()), properties = null) })

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
    val state = rememberMapState(
        baseStyle = if (satellite) SatelliteStyle else StreetStyle,
        initialCameraPosition = CameraPosition(
            center = Position(longitude = camera.longitude, latitude = camera.latitude),
            zoom = camera.zoom,
        ),
    ) {
        val route = overlay.polyline
        if (route.size >= 2) {
            val source = rememberGeoJsonSource(
                GeoJsonData.Features(LineString(route.map { it.toPosition() })),
            )
            LineLayer(
                id = "route-casing",
                source = source,
                color = const(Color.White),
                width = const(8.dp),
                cap = const(LineCap.Round),
                join = const(LineJoin.Round),
            )
            LineLayer(
                id = "route",
                source = source,
                color = const(AppColors.primary.copy(alpha = 0.85f)),
                width = const(4.dp),
                cap = const(LineCap.Round),
                join = const(LineJoin.Round),
            )
        }
        if (overlay.dots.isNotEmpty()) {
            val source = rememberGeoJsonSource(GeoJsonData.Features(points(overlay.dots)))
            CircleLayer(
                id = "history-dots",
                source = source,
                radius = const(6.dp),
                color = const(Color.White),
                strokeColor = const(AppColors.primary),
                strokeWidth = const(2.dp),
            )
        }
        overlay.selected?.let { selected ->
            val source = rememberGeoJsonSource(GeoJsonData.Features(Point(selected.toPosition())))
            CircleLayer(
                id = "history-selected",
                source = source,
                radius = const(8.dp),
                color = const(AppColors.primary),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp),
            )
        }
        overlay.self?.let { self ->
            val source = rememberGeoJsonSource(GeoJsonData.Features(Point(self.toPosition())))
            CircleLayer(
                id = "self-halo",
                source = source,
                radius = const(18.dp),
                color = const(AppColors.primary.copy(alpha = 0.18f)),
                strokeWidth = const(0.dp),
            )
            CircleLayer(
                id = "self-dot",
                source = source,
                radius = const(8.dp),
                color = const(AppColors.primary),
                strokeColor = const(Color.White),
                strokeWidth = const(3.dp),
            )
        }
    }
    LaunchedEffect(move) {
        state.setCameraPosition(
            CameraPosition(
                center = Position(longitude = camera.longitude, latitude = camera.latitude),
                zoom = camera.zoom,
            ),
        )
    }
    val rendered = state.cameraPosition
    LaunchedEffect(rendered.center.longitude, rendered.center.latitude, rendered.zoom) {
        onCameraChange(
            MapCamera(
                latitude = rendered.center.latitude,
                longitude = rendered.center.longitude,
                zoom = rendered.zoom,
            ),
        )
    }
    MaplibreMap(
        modifier = modifier,
        state = state,
        cameraConstraints = CameraConstraints {
            minZoom = 2.0
            maxZoom = 18.0
            maxPitch = 0.0
        },
        interactions = MapInteractions {
            camera {
                rotate { enabled = false }
                pitch { enabled = false }
            }
            callbacks {
                click {
                    onUnhandled { event ->
                        val position = event.position
                        if (position != null) {
                            onMapClick?.invoke(GeoPoint(position.latitude, position.longitude))
                        }
                        ClickResult.Pass
                    }
                }
            }
        },
        overlay = {
            include(LibraryMapOverlay.AttributionOnly)
            markers.forEach { marker ->
                Box(
                    Modifier
                        .placedAt(marker.position.toPosition(), Alignment.BottomCenter)
                        .size(56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    markerContent(marker)
                }
            }
        },
    )
}
