package top.krushiler.tracktalk.http

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import kotlin.time.Instant

internal fun ApplicationCall.longParam(name: String): Long =
    parameters[name]?.toLongOrNull()
        ?: throw ApiException(HttpStatusCode.BadRequest, "Invalid $name")

internal fun ApplicationCall.optionalLongQuery(name: String): Long? {
    val raw = request.queryParameters[name] ?: return null
    return raw.toLongOrNull()
        ?: throw ApiException(HttpStatusCode.BadRequest, "Invalid $name")
}

internal fun ApplicationCall.limitQuery(): Int {
    val raw = request.queryParameters["limit"] ?: return 50
    return raw.toIntOrNull()
        ?: throw ApiException(HttpStatusCode.BadRequest, "Invalid limit")
}

internal fun ApplicationCall.optionalInstant(name: String): Instant? {
    val raw = request.queryParameters[name] ?: return null
    return try {
        Instant.parse(raw)
    } catch (_: IllegalArgumentException) {
        throw ApiException(HttpStatusCode.BadRequest, "Invalid '$name' timestamp")
    }
}
