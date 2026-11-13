package top.krushiler.tracktalk.feature.user

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.put
import io.ktor.utils.io.readBuffer
import kotlinx.io.readByteArray
import top.krushiler.tracktalk.dto.FcmTokenUpdate
import top.krushiler.tracktalk.dto.UsernameUpdate
import top.krushiler.tracktalk.http.ApiException
import top.krushiler.tracktalk.http.userId

internal fun Route.userRoutes(users: UserService) {
    get("/me") {
        call.respond(users.getMe(call.userId()))
    }
    patch("/me") {
        val body = call.receive<UsernameUpdate>()
        call.respond(users.updateUsername(call.userId(), body))
    }
    put("/me/fcm-token") {
        users.updateFcmToken(call.userId(), call.receive<FcmTokenUpdate>())
        call.respond(HttpStatusCode.NoContent)
    }
    put("/me/avatar") {
        val upload = call.receiveAvatar()
        call.respond(users.updateAvatar(call.userId(), upload.bytes, upload.contentType))
    }
    delete("/me/avatar") {
        call.respond(users.removeAvatar(call.userId()))
    }
}

private class AvatarUpload(
    val bytes: ByteArray,
    val contentType: String,
)

private suspend fun ApplicationCall.receiveAvatar(): AvatarUpload {
    val multipart = receiveMultipart()
    var bytes: ByteArray? = null
    var contentType: String? = null
    multipart.forEachPart { part ->
        try {
            if (bytes == null && part is PartData.FileItem) {
                contentType = part.contentType?.withoutParameters()?.toString()
                bytes = part.provider().readBuffer().readByteArray()
            }
        } finally {
            part.release()
        }
    }
    return AvatarUpload(
        bytes = bytes ?: throw ApiException(HttpStatusCode.BadRequest, "Empty file"),
        contentType = contentType.orEmpty(),
    )
}
