package top.krushiler.tracktalk.data.repository

import top.krushiler.tracktalk.data.network.AccessTokenStore
import top.krushiler.tracktalk.data.network.HttpRoutes
import top.krushiler.tracktalk.dto.LoginRequest
import top.krushiler.tracktalk.dto.LoginResponse
import top.krushiler.tracktalk.dto.UsernameUpdate
import top.krushiler.tracktalk.model.User

class AuthRepository(
    private val http: HttpRoutes,
    private val accessToken: AccessTokenStore,
) {
    suspend fun login(login: String, password: String): LoginResponse {
        val response = http.login(LoginRequest(login = login, password = password))
        accessToken.update(response.accessToken, response.refreshToken)
        return response
    }

    suspend fun logout() {
        accessToken.update(null, null)
    }

    suspend fun me(): User = http.me()

    suspend fun updateUsername(username: String): User =
        http.updateUsername(UsernameUpdate(username))

    suspend fun updateAvatar(bytes: ByteArray, contentType: String): User =
        http.updateAvatar(bytes, contentType)
}
