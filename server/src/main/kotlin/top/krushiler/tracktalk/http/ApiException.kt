package top.krushiler.tracktalk.http

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

@Serializable
internal data class ErrorResponse(val detail: String)

@Serializable
internal data class UpdateRequiredResponse(
    val detail: String,
    val minBuild: Int,
)

internal class ApiException(
    val status: HttpStatusCode,
    val detail: String,
) : RuntimeException(detail)
