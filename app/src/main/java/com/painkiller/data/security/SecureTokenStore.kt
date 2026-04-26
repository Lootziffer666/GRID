package com.painkiller.data.security

/**
 * Gate 3 token boundary.
 *
 * Implementations must use Android Keystore / AndroidX Security and must never
 * log or expose raw token values.
 */
interface SecureTokenStore {
    suspend fun readGithubToken(): String?
    suspend fun writeGithubToken(token: String)
    suspend fun clearGithubToken()
}
