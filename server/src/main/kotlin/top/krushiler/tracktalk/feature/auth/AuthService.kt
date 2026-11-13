package top.krushiler.tracktalk.feature.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.http.HttpStatusCode
import top.krushiler.tracktalk.config.AppConfig
import top.krushiler.tracktalk.dto.LoginRequest
import top.krushiler.tracktalk.dto.LoginResponse
import top.krushiler.tracktalk.dto.RefreshRequest
import top.krushiler.tracktalk.dto.RefreshResponse
import top.krushiler.tracktalk.feature.user.Profiles
import top.krushiler.tracktalk.feature.user.UserRecord
import top.krushiler.tracktalk.feature.user.UserRepository
import top.krushiler.tracktalk.http.ApiException

internal class AuthService(
    private val users: UserRepository,
    private val tokens: TokenService,
    private val profiles: Profiles,
    config: AppConfig,
) {
    private val bcryptCost = config.bcryptCost.coerceIn(4, 31)

    suspend fun login(request: LoginRequest): LoginResponse {
        if (request.login.isEmpty() || request.login.length > 320 ||
            request.password.isEmpty() || request.password.length > 72
        ) {
            throw ApiException(HttpStatusCode.BadRequest, "Invalid login or password")
        }

        val existing = users.findByLogin(request.login)
        if (existing == null) {
            val created = users.createIfAbsent(
                login = request.login,
                username = request.login,
                passwordHash = hash(request.password),
            )
            if (created != null) return response(created, isNewUser = true)
            val winner = users.findByLogin(request.login)
                ?: throw ApiException(HttpStatusCode.Unauthorized, "Invalid login or password")
            if (!verify(request.password, winner.passwordHash)) {
                throw ApiException(HttpStatusCode.Unauthorized, "Invalid login or password")
            }
            return response(winner, isNewUser = false)
        }

        if (!verify(request.password, existing.passwordHash)) {
            throw ApiException(HttpStatusCode.Unauthorized, "Invalid login or password")
        }
        return response(existing, isNewUser = false)
    }

    suspend fun refresh(request: RefreshRequest): RefreshResponse {
        val userId = tokens.refreshUserId(request.refreshToken)
        val user = users.find(userId)
            ?: throw ApiException(HttpStatusCode.Unauthorized, "Invalid or expired token")
        return RefreshResponse(
            accessToken = tokens.createAccess(user.login, user.id),
            refreshToken = tokens.createRefresh(user.login, user.id),
        )
    }

    private suspend fun response(user: UserRecord, isNewUser: Boolean): LoginResponse {
        val publicUser = profiles.user(user)
        return LoginResponse(
            accessToken = tokens.createAccess(user.login, user.id),
            refreshToken = tokens.createRefresh(user.login, user.id),
            userId = publicUser.id,
            login = publicUser.login,
            username = publicUser.username,
            avatarUrl = publicUser.avatarUrl,
            isNewUser = isNewUser,
        )
    }

    private fun hash(password: String): String =
        BCrypt.withDefaults().hashToString(bcryptCost, password.toCharArray())

    private fun verify(password: String, passwordHash: String): Boolean =
        BCrypt.verifyer().verify(password.toCharArray(), passwordHash).verified
}
