package top.krushiler.tracktalk.data.storage

import kotlinx.browser.localStorage

actual fun createTokenStorage(): TokenStorage = BrowserTokenStorage()

private class BrowserTokenStorage : TokenStorage {
    override suspend fun read(): AuthTokens = AuthTokens(
        accessToken = item(ACCESS),
        refreshToken = item(REFRESH),
    )

    override suspend fun write(tokens: AuthTokens) {
        put(ACCESS, tokens.accessToken)
        put(REFRESH, tokens.refreshToken)
    }

    private fun item(key: String): String? = try {
        localStorage.getItem(key)?.takeIf { it.isNotBlank() }
    } catch (_: Throwable) {
        null
    }

    private fun put(key: String, value: String?) {
        try {
            if (value.isNullOrBlank()) localStorage.removeItem(key) else localStorage.setItem(key, value)
        } catch (_: Throwable) {
            // Private browsing can reject storage. The session stays in memory for this page.
        }
    }

    private companion object {
        const val ACCESS = "auth_access_token"
        const val REFRESH = "auth_refresh_token"
    }
}
