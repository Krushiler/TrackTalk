package top.krushiler.tracktalk.platform

sealed interface AvatarPick {
    data class Image(val bytes: ByteArray, val contentType: String) : AvatarPick {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Image

            if (!bytes.contentEquals(other.bytes)) return false
            if (contentType != other.contentType) return false

            return true
        }

        override fun hashCode(): Int {
            var result = bytes.contentHashCode()
            result = 31 * result + contentType.hashCode()
            return result
        }
    }

    data object Cancelled : AvatarPick
    data object Unsupported : AvatarPick
}

interface AvatarPicker {
    suspend fun pick(): AvatarPick
}

expect fun createAvatarPicker(): AvatarPicker
