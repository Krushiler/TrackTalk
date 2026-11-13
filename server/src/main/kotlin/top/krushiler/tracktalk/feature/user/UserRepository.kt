package top.krushiler.tracktalk.feature.user

import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import top.krushiler.tracktalk.database.GmsDatabase
import top.krushiler.tracktalk.database.Users
import top.krushiler.tracktalk.database.asDbId

internal class UserRepository(private val database: GmsDatabase) {
    suspend fun find(id: Long): UserRecord? = database.transaction { findUser(id) }

    suspend fun findByLogin(login: String): UserRecord? = database.transaction { findUserByLogin(login) }

    suspend fun createIfAbsent(login: String, username: String, passwordHash: String): UserRecord? =
        database.transaction {
            if (findUserByLogin(login) != null) {
                null
            } else {
                insertUser(login, username, passwordHash)
            }
        }

    suspend fun changeUsername(userId: Long, username: String): UsernameChange = database.transaction {
        val me = findUser(userId) ?: return@transaction UsernameChange.Missing
        val taken = findUserByUsername(username)
        if (taken != null && taken.id != me.id) return@transaction UsernameChange.Taken
        UsernameChange.Updated(saveUser(me.copy(username = username)))
    }

    suspend fun changeFcmToken(userId: Long, token: String): UserRecord? = database.transaction {
        val me = findUser(userId) ?: return@transaction null
        saveUser(me.copy(fcmToken = token))
    }

    suspend fun changeAvatarKey(userId: Long, avatarKey: String?): UserRecord? = database.transaction {
        val me = findUser(userId) ?: return@transaction null
        saveUser(me.copy(avatarKey = avatarKey))
    }

    suspend fun fcmToken(userId: Long): String? = database.transaction { findUser(userId)?.fcmToken }
}

internal sealed interface UsernameChange {
    data class Updated(val user: UserRecord) : UsernameChange
    data object Missing : UsernameChange
    data object Taken : UsernameChange
}

internal fun findUser(id: Long): UserRecord? =
    Users.selectAll()
        .where { Users.id eq id.asDbId() }
        .singleOrNull()
        ?.toUser()

internal fun findUserByLogin(login: String): UserRecord? =
    Users.selectAll()
        .where { Users.login eq login }
        .singleOrNull()
        ?.toUser()

internal fun findUserByUsername(username: String): UserRecord? =
    Users.selectAll()
        .where { Users.username eq username }
        .singleOrNull()
        ?.toUser()

internal fun insertUser(login: String, username: String, passwordHash: String): UserRecord {
    val id = Users.insert {
        it[Users.login] = login
        it[Users.username] = username
        it[Users.passwordHash] = passwordHash
        it[createdAt] = OffsetDateTime.now(ZoneOffset.UTC)
    } get Users.id
    return UserRecord(
        id = id.toLong(),
        login = login,
        username = username,
        passwordHash = passwordHash,
        avatarKey = null,
        fcmToken = null,
    )
}

internal fun saveUser(record: UserRecord): UserRecord {
    val updated = Users.update({ Users.id eq record.id.asDbId() }) {
        it[username] = record.username
        it[passwordHash] = record.passwordHash
        it[avatarKey] = record.avatarKey
        it[fcmToken] = record.fcmToken
    }
    if (updated == 0) error("User ${record.id} is not stored")
    return record
}

private fun ResultRow.toUser(): UserRecord = UserRecord(
    id = this[Users.id].toLong(),
    login = this[Users.login],
    username = this[Users.username],
    passwordHash = this[Users.passwordHash],
    avatarKey = this[Users.avatarKey],
    fcmToken = this[Users.fcmToken],
)
