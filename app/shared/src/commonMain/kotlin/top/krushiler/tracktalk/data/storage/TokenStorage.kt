package top.krushiler.tracktalk.data.storage

data class AuthTokens(
    val accessToken: String? = null,
    val refreshToken: String? = null,
)

interface TokenStorage {
    suspend fun read(): AuthTokens

    suspend fun write(tokens: AuthTokens)
}

class MemoryTokenStorage : TokenStorage {
    private var tokens = AuthTokens()

    override suspend fun read(): AuthTokens = tokens

    override suspend fun write(tokens: AuthTokens) {
        this.tokens = tokens
    }
}

expect fun createTokenStorage(): TokenStorage
