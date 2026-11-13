package top.krushiler.tracktalk.location

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class LocationSyncPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_TOKEN, value) }

    var baseUrl: String?
        get() = prefs.getString(KEY_BASE_URL, null)?.trimEnd('/')
        set(value) = prefs.edit { putString(KEY_BASE_URL, value?.trimEnd('/')) }

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_ENABLED, value) }

    var hasRequestedAddTile: Boolean
        get() = prefs.getBoolean(KEY_REQUESTED_ADD_TILE, false)
        set(value) = prefs.edit { putBoolean(KEY_REQUESTED_ADD_TILE, value) }

    fun saveAndEnable(token: String, baseUrl: String) {
        prefs.edit {
            putString(KEY_TOKEN, token)
                .putString(KEY_BASE_URL, baseUrl.trimEnd('/'))
                .putBoolean(KEY_ENABLED, true)
        }
    }

    fun disable() {
        prefs.edit { putBoolean(KEY_ENABLED, false) }
    }

    fun clear() {
        prefs.edit { clear() }
    }

    fun hasCredentials(): Boolean =
        !token.isNullOrBlank() && !baseUrl.isNullOrBlank()

    companion object {
        private const val PREFS_NAME = "location_sync_prefs"
        private const val KEY_TOKEN = "token"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_REQUESTED_ADD_TILE = "requested_add_tile"
    }
}
