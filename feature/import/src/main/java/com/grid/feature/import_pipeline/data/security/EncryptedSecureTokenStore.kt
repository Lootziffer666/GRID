package com.grid.feature.import_pipeline.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Production [SecureTokenStore].
 *
 * Persists the GitHub token in [EncryptedSharedPreferences], encrypted with
 * AES-256-GCM. The master key is generated and held in the Android Keystore
 * (hardware-backed where available) via [MasterKey] with the
 * `AES256_GCM_SPEC` configuration. Keys themselves are encrypted with
 * AES-256-SIV.
 *
 * The token never appears in logs, in [Object.toString], in crash reports,
 * or on disk in plaintext. The on-device storage file is named
 * `painkiller_secure_tokens.xml` and is private to the app's user id.
 *
 * This class is thread-safe: [EncryptedSharedPreferences] synchronises its
 * own writes, and the suspend functions hop to [Dispatchers.IO] so the main
 * thread is never blocked on disk encryption.
 */
class EncryptedSecureTokenStore(
    appContext: Context,
) : SecureTokenStore {

    private val prefs: SharedPreferences = createEncryptedPrefs(appContext.applicationContext)

    override suspend fun readGithubToken(): String? = withContext(Dispatchers.IO) {
        val value = prefs.getString(KEY_GITHUB_TOKEN, null)
        if (value.isNullOrBlank()) null else value
    }

    override suspend fun writeGithubToken(token: String) {
        require(token.isNotBlank()) { "token must not be blank" }
        withContext(Dispatchers.IO) {
            prefs.edit().putString(KEY_GITHUB_TOKEN, token).apply()
        }
    }

    override suspend fun clearGithubToken() {
        withContext(Dispatchers.IO) {
            prefs.edit().remove(KEY_GITHUB_TOKEN).apply()
        }
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private companion object {
        const val PREFS_FILE_NAME = "painkiller_secure_tokens"
        const val KEY_GITHUB_TOKEN = "github_token"
    }
}
