package top.krushiler.tracktalk.feature.user

internal data class UserRecord(
    val id: Long,
    val login: String,
    val username: String,
    val passwordHash: String,
    val avatarKey: String?,
    val fcmToken: String?,
)
