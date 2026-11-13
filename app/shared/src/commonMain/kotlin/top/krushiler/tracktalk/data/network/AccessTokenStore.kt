package top.krushiler.tracktalk.data.network

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import top.krushiler.tracktalk.data.storage.AuthTokens
import top.krushiler.tracktalk.data.storage.MemoryTokenStorage
import top.krushiler.tracktalk.data.storage.TokenStorage

class AccessTokenStore(
    private val storage: TokenStorage = MemoryTokenStorage(),
) {
    private val mutex = Mutex()
    private var loaded = false

    @Volatile
    var token: String? = null
        private set

    @Volatile
    var refreshToken: String? = null
        private set

    fun hasSession(): Boolean = !token.isNullOrBlank() || !refreshToken.isNullOrBlank()

    suspend fun load() {
        mutex.withLock {
            if (loaded) return
            val stored = storage.read()
            token = stored.accessToken?.takeIf { it.isNotBlank() }
            refreshToken = stored.refreshToken?.takeIf { it.isNotBlank() }
            loaded = true
        }
    }

    suspend fun update(access: String?, refresh: String?) {
        val next = AuthTokens(
            accessToken = access?.takeIf { it.isNotBlank() },
            refreshToken = refresh?.takeIf { it.isNotBlank() },
        )
        mutex.withLock {
            storage.write(next)
            token = next.accessToken
            refreshToken = next.refreshToken
            loaded = true
        }
    }
}
