package top.krushiler.tracktalk.geo

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

fun distanceMeters(from: GeoPoint, to: GeoPoint): Double {
    val earth = 6_371_000.0
    val lat1 = from.latitude.toRadians()
    val lat2 = to.latitude.toRadians()
    val dLat = (to.latitude - from.latitude).toRadians()
    val dLon = (to.longitude - from.longitude).toRadians()
    val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * earth * asin(sqrt(a))
}

private fun Double.toRadians(): Double = this * PI / 180.0

object WebMercator {
    private const val TileSize = 256.0
    private const val MaxLatitude = 85.05112878

    fun worldX(longitude: Double, zoom: Double): Double =
        (longitude + 180.0) / 360.0 * worldSize(zoom)

    fun worldY(latitude: Double, zoom: Double): Double {
        val lat = latitude.coerceIn(-MaxLatitude, MaxLatitude).toRadians()
        val mercator = ln(tan(lat) + 1 / cos(lat))
        return (1.0 - mercator / PI) / 2.0 * worldSize(zoom)
    }

    fun longitude(worldX: Double, zoom: Double): Double =
        worldX / worldSize(zoom) * 360.0 - 180.0

    fun latitude(worldY: Double, zoom: Double): Double {
        val mercator = PI * (1 - 2 * worldY / worldSize(zoom))
        return atan(sinh(mercator)) * 180.0 / PI
    }

    fun worldSize(zoom: Double): Double = TileSize * 2.0.pow(zoom)
}
