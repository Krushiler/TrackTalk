package top.krushiler.tracktalk.presentation.navigation

import kotlinx.serialization.Serializable
import top.krushiler.tracktalk.model.Friend

@Serializable
data class PersonRef(
    val userId: Long,
    val login: String,
    val username: String,
    val avatarUrl: String? = null,
)

fun Friend.toPerson(): PersonRef = PersonRef(
    userId = id,
    login = login,
    username = username,
    avatarUrl = avatarUrl,
)

fun PersonRef.toFriend(): Friend = Friend(
    id = userId,
    login = login,
    username = username,
    avatarUrl = avatarUrl,
)
