package top.krushiler.tracktalk.data.network

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

@Serializable
internal data class ApiErrorBody(
    val detail: String,
    val minBuild: Int? = null,
)

class ApiException(
    val status: HttpStatusCode,
    val detail: String,
    val minBuild: Int? = null,
) : RuntimeException(detail)
