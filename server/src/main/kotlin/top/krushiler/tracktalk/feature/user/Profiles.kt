package top.krushiler.tracktalk.feature.user

import top.krushiler.tracktalk.model.Friend
import top.krushiler.tracktalk.model.User

internal class Profiles(
    private val avatars: AvatarStorage,
) {
    suspend fun user(record: UserRecord): User = User(
        id = record.id,
        login = record.login,
        username = record.username,
        avatarUrl = avatars.url(record.avatarKey),
    )

    suspend fun friend(record: UserRecord, locationShared: Boolean = false): Friend = Friend(
        id = record.id,
        login = record.login,
        username = record.username,
        avatarUrl = avatars.url(record.avatarKey),
        locationShared = locationShared,
    )
}
