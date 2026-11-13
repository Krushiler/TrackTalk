package top.krushiler.tracktalk.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.FileStorage
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesFileSerializer
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.tink.AeadSerializer
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import top.krushiler.tracktalk.platform.context.androidAppContext
import top.krushiler.tracktalk.platform.context.hasAndroidAppContext

actual fun createTokenStorage(): TokenStorage =
    if (hasAndroidAppContext()) {
        AndroidTokenStorage(androidAppContext)
    } else {
        MemoryTokenStorage()
    }

private class AndroidTokenStorage(
    context: Context,
) : TokenStorage {
    private val appContext = context.applicationContext
    private val dataStore: DataStore<Preferences> by lazy { AndroidAuthDataStore.get(appContext) }

    override suspend fun read(): AuthTokens = try {
        val prefs = dataStore.data.first()
        AuthTokens(
            accessToken = prefs[ACCESS],
            refreshToken = prefs[REFRESH],
        )
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        AuthTokens()
    }

    override suspend fun write(tokens: AuthTokens) {
        dataStore.edit { prefs ->
            prefs.setOrRemove(ACCESS, tokens.accessToken)
            prefs.setOrRemove(REFRESH, tokens.refreshToken)
        }
    }

    private fun MutablePreferences.setOrRemove(key: Preferences.Key<String>, value: String?) {
        if (value.isNullOrBlank()) remove(key) else this[key] = value
    }

    private companion object {
        val ACCESS = stringPreferencesKey("access_token")
        val REFRESH = stringPreferencesKey("refresh_token")
    }
}

private object AndroidAuthDataStore {
    private const val FILE_NAME = "auth_session.preferences_pb"
    private const val KEYSET_NAME = "auth_keyset"
    private const val KEYSET_PREF = "auth_keyset_prefs"
    private const val MASTER_KEY_URI = "android-keystore://tracktalk_auth_master_key"

    @Volatile
    private var instance: DataStore<Preferences>? = null

    fun get(context: Context): DataStore<Preferences> =
        instance ?: synchronized(this) {
            instance ?: create(context.applicationContext).also { instance = it }
        }

    private fun create(context: Context): DataStore<Preferences> {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREF)
            .withKeyTemplate(KeyTemplate.createFrom(PredefinedAeadParameters.AES256_GCM))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        val aead = keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        return DataStoreFactory.create(
            storage = FileStorage(
                serializer = AeadSerializer(
                    aead = aead,
                    wrappedSerializer = PreferencesFileSerializer,
                    associatedData = FILE_NAME.encodeToByteArray(),
                ),
                produceFile = { File(context.filesDir, FILE_NAME) },
            ),
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        )
    }
}
