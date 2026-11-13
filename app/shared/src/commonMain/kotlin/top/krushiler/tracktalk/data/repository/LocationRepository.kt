package top.krushiler.tracktalk.data.repository

import top.krushiler.tracktalk.data.network.HttpRoutes
import top.krushiler.tracktalk.model.Location

class LocationRepository(
    private val http: HttpRoutes,
) {
    suspend fun sync(location: Location) {
        http.postLocation(location)
    }
}
