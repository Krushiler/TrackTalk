package top.krushiler.tracktalk.database

import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.time.Instant
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.postgresql.util.PGobject
import top.krushiler.tracktalk.model.FriendshipStatus

internal object GmsDialect {
    @Volatile
    var postgres: Boolean = false
}

internal object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val login = varchar("login", 320)
    val username = varchar("username", 320)
    val passwordHash = varchar("password_hash", 255)
    val avatarKey = varchar("avatar_key", 255).nullable()
    val fcmToken = varchar("fcm_token", 512).nullable()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}

internal object Friendships : Table("friendships") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id")
    val friendId = integer("friend_id")
    val requestedByUserId = integer("requested_by_user_id")
    val status = friendshipStatus()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}

internal object LocationShares : Table("location_shares") {
    val sharerId = integer("sharer_id")
    val viewerId = integer("viewer_id")
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(sharerId, viewerId)
}

internal object UserLocations : Table("user_locations") {
    val userId = integer("user_id")
    val latitude = double("latitude")
    val longitude = double("longitude")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(userId)
}

internal object UserLocationLogs : Table("user_location_logs") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id")
    val latitude = double("latitude")
    val longitude = double("longitude")
    val recordedAt = timestampWithTimeZone("recorded_at")

    override val primaryKey = PrimaryKey(id)
}

internal object ChatMessages : Table("chat_messages") {
    val id = integer("id").autoIncrement()
    val senderId = integer("sender_id")
    val recipientId = integer("recipient_id")
    val body = varchar("body", 4000)
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}

private fun Table.friendshipStatus(): Column<FriendshipStatus> = customEnumeration(
    name = "status",
    sql = "friendship_status",
    fromDb = { value ->
        when (statusLabel(value)) {
            "pending" -> FriendshipStatus.Pending
            "accepted" -> FriendshipStatus.Accepted
            else -> error("Unknown friendship status: $value")
        }
    },
    toDb = { status ->
        val label = when (status) {
            FriendshipStatus.Pending -> "pending"
            FriendshipStatus.Accepted -> "accepted"
        }
        if (!GmsDialect.postgres) {
            label
        } else {
            PGobject().apply {
                type = "friendship_status"
                value = label
            }
        }
    },
)

private fun statusLabel(value: Any): String = when (value) {
    is PGobject -> value.value.orEmpty()
    else -> value.toString()
}.lowercase()

internal fun Long.asDbId(): Int {
    val id = toInt()
    require(id.toLong() == this) { "Database id does not fit in the gms integer column: $this" }
    return id
}

internal fun Instant.toOffsetDateTime(): OffsetDateTime =
    OffsetDateTime.ofInstant(
        java.time.Instant.ofEpochSecond(epochSeconds, nanosecondsOfSecond.toLong()),
        ZoneOffset.UTC,
    )

internal fun OffsetDateTime.toKotlinInstant(): Instant {
    val javaInstant = toInstant()
    return Instant.fromEpochSeconds(javaInstant.epochSecond, javaInstant.nano.toLong())
}
