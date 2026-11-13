package top.krushiler.tracktalk.feature.user

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal interface AvatarStorage {
    suspend fun upload(data: ByteArray, contentType: String): String
    suspend fun delete(key: String?)
    suspend fun url(key: String?): String?
}

internal class InMemoryAvatarStorage : AvatarStorage {
    private val objects = ConcurrentHashMap<String, ByteArray>()

    override suspend fun upload(data: ByteArray, contentType: String): String {
        val ext = when (contentType) {
            "image/jpeg", "image/jpg" -> ".jpg"
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            "image/gif" -> ".gif"
            else -> ""
        }
        val key = UUID.randomUUID().toString().replace("-", "") + ext
        objects[key] = data
        return key
    }

    override suspend fun delete(key: String?) {
        if (key != null) objects.remove(key)
    }

    override suspend fun url(key: String?): String? = key?.let { "memory://avatars/$it" }
}
