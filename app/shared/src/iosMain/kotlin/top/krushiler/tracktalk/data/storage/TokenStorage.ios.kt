package top.krushiler.tracktalk.data.storage

import platform.Foundation.NSUserDefaults

actual fun createTokenStorage(): TokenStorage = IosTokenStorage()

private class IosTokenStorage : TokenStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun read(): AuthTokens = AuthTokens(
        accessToken = defaults.stringForKey(ACCESS),
        refreshToken = defaults.stringForKey(REFRESH),
    )

    override suspend fun write(tokens: AuthTokens) {
        write(ACCESS, tokens.accessToken)
        write(REFRESH, tokens.refreshToken)
    }

    private fun write(key: String, value: String?) {
        if (value.isNullOrBlank()) {
            defaults.removeObjectForKey(key)
        } else {
            defaults.setObject(value, forKey = key)
        }
    }

    private companion object {
        const val ACCESS = "auth_access_token"
        const val REFRESH = "auth_refresh_token"
    }
}
