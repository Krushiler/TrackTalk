package top.krushiler.tracktalk.data.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import top.krushiler.tracktalk.data.storage.MemoryTokenStorage

class AuthSessionTest {
    @Test
    fun storedSessionIsRestoredAfterARestartAndClearedOnLogout() = runBlocking {
        val storage = MemoryTokenStorage()
        AccessTokenStore(storage).update(access = "access", refresh = "refresh")

        val restored = AccessTokenStore(storage)
        restored.load()
        assertEquals("access", restored.token)
        assertEquals("refresh", restored.refreshToken)
        assertTrue(restored.hasSession())

        restored.update(access = null, refresh = null)
        val signedOut = AccessTokenStore(storage)
        signedOut.load()
        assertNull(signedOut.token)
        assertNull(signedOut.refreshToken)
        assertFalse(signedOut.hasSession())
    }
}
