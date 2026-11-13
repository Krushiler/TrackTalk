package top.krushiler.tracktalk.feature.push

import org.slf4j.LoggerFactory
import top.krushiler.tracktalk.feature.realtime.RealtimeService
import top.krushiler.tracktalk.feature.user.UserRepository

internal class PushService(
    private val users: UserRepository,
    private val realtime: RealtimeService,
) {
    private val logger = LoggerFactory.getLogger(PushService::class.java)

    suspend fun send(
        userId: Long,
        title: String,
        body: String,
        data: Map<String, String>,
        skipIfOnline: Boolean = false,
    ) {
        if (skipIfOnline && realtime.isOnline(userId)) return
        val token = users.fcmToken(userId)
        if (token.isNullOrBlank()) return
        logger.info(
            "FCM credentials are not configured; skipped push type={} userId={} title={}",
            data["type"],
            userId,
            title,
        )
        logger.debug("Push body: {}", body)
    }
}
