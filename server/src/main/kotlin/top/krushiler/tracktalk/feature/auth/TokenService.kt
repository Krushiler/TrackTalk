package top.krushiler.tracktalk.feature.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import com.auth0.jwt.interfaces.Payload
import io.ktor.http.HttpStatusCode
import java.util.Date
import top.krushiler.tracktalk.config.AppConfig
import top.krushiler.tracktalk.http.ApiException

internal class TokenService(config: AppConfig) {
    private val algorithm = Algorithm.HMAC256(config.jwtSecret)
    val verifier: JWTVerifier = JWT.require(algorithm).build()
    private val accessExpireMillis = config.jwtExpireMinutes * 60_000
    private val refreshExpireMillis = config.jwtRefreshExpireMinutes * 60_000

    fun createAccess(login: String, userId: Long): String =
        sign(login, userId, ACCESS, accessExpireMillis)

    fun createRefresh(login: String, userId: Long): String =
        sign(login, userId, REFRESH, refreshExpireMillis)

    fun isAccessToken(payload: Payload): Boolean =
        payload.getClaim(TOKEN_USE).asString() == ACCESS && payload.getClaim("user_id").asLong() != null

    fun userId(token: String): Long = userId(verified(token), ACCESS)

    fun refreshUserId(token: String): Long = userId(verified(token), REFRESH)

    private fun sign(login: String, userId: Long, use: String, expireMillis: Long): String =
        JWT.create()
            .withSubject(login)
            .withClaim("user_id", userId)
            .withClaim(TOKEN_USE, use)
            .withExpiresAt(Date(System.currentTimeMillis() + expireMillis))
            .sign(algorithm)

    private fun verified(token: String): Payload = try {
        verifier.verify(token)
    } catch (_: Exception) {
        throw ApiException(HttpStatusCode.Unauthorized, "Invalid or expired token")
    }

    private fun userId(payload: Payload, expectedUse: String): Long {
        if (payload.getClaim(TOKEN_USE).asString() != expectedUse) {
            throw ApiException(HttpStatusCode.Unauthorized, "Invalid or expired token")
        }
        return payload.getClaim("user_id").asLong()
            ?: throw ApiException(HttpStatusCode.Unauthorized, "Invalid token payload")
    }

    private companion object {
        const val TOKEN_USE = "token_use"
        const val ACCESS = "access"
        const val REFRESH = "refresh"
    }
}
