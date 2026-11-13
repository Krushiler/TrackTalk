package top.krushiler.tracktalk.feature.user

import io.ktor.http.HttpStatusCode
import kotlin.coroutines.cancellation.CancellationException
import top.krushiler.tracktalk.dto.FcmTokenUpdate
import top.krushiler.tracktalk.dto.UsernameUpdate
import top.krushiler.tracktalk.http.ApiException
import top.krushiler.tracktalk.model.User

internal class UserService(
    private val users: UserRepository,
    private val profiles: Profiles,
    private val avatars: AvatarStorage,
) {
    suspend fun require(userId: Long) {
        current(userId)
    }

    suspend fun getMe(userId: Long): User = profiles.user(current(userId))

    suspend fun updateUsername(userId: Long, body: UsernameUpdate): User {
        val username = body.username.trim()
        if (username.isEmpty()) {
            throw ApiException(HttpStatusCode.BadRequest, "Username cannot be empty")
        }
        if (username.length > 320) {
            throw ApiException(HttpStatusCode.BadRequest, "Username is too long")
        }
        val updated = when (val change = users.changeUsername(userId, username)) {
            UsernameChange.Missing -> throw ApiException(HttpStatusCode.Unauthorized, "User not found")
            UsernameChange.Taken -> throw ApiException(HttpStatusCode.Conflict, "Username is already taken")
            is UsernameChange.Updated -> change.user
        }
        return profiles.user(updated)
    }

    suspend fun updateFcmToken(userId: Long, body: FcmTokenUpdate) {
        val token = body.token.trim()
        if (token.isEmpty() || token.length > 512) {
            throw ApiException(HttpStatusCode.BadRequest, "Invalid token")
        }
        users.changeFcmToken(userId, token)
            ?: throw ApiException(HttpStatusCode.Unauthorized, "User not found")
    }

    suspend fun updateAvatar(userId: Long, bytes: ByteArray, contentType: String): User {
        val normalized = contentType.substringBefore(";").trim().lowercase()
        if (normalized !in ALLOWED_AVATAR_TYPES) {
            throw ApiException(HttpStatusCode.BadRequest, "Avatar must be jpeg, png, webp, or gif")
        }
        if (bytes.isEmpty()) {
            throw ApiException(HttpStatusCode.BadRequest, "Empty file")
        }
        if (bytes.size > MAX_AVATAR_BYTES) {
            throw ApiException(HttpStatusCode.BadRequest, "Avatar must be 5MB or smaller")
        }
        val previous = current(userId)
        val newKey = try {
            avatars.upload(bytes, normalized)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            throw ApiException(
                HttpStatusCode.BadGateway,
                "Failed to upload avatar to storage: ${error.message}",
            )
        }
        val updated = users.changeAvatarKey(userId, newKey)
            ?: throw ApiException(HttpStatusCode.Unauthorized, "User not found")
        if (previous.avatarKey != null && previous.avatarKey != newKey) {
            avatars.delete(previous.avatarKey)
        }
        return profiles.user(updated)
    }

    suspend fun removeAvatar(userId: Long): User {
        val previous = current(userId)
        val updated = users.changeAvatarKey(userId, null)
            ?: throw ApiException(HttpStatusCode.Unauthorized, "User not found")
        avatars.delete(previous.avatarKey)
        return profiles.user(updated)
    }

    private suspend fun current(userId: Long): UserRecord =
        users.find(userId) ?: throw ApiException(HttpStatusCode.Unauthorized, "User not found")

    private companion object {
        val ALLOWED_AVATAR_TYPES = setOf(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif",
        )
        const val MAX_AVATAR_BYTES = 5 * 1024 * 1024
    }
}
