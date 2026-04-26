package com.painkiller.data.security

/**
 * Temporary non-persistent implementation for Gate 3 wiring/tests.
 *
 * A keystore-backed implementation replaces this in a later hardening gate.
 */
class InMemorySecureTokenStore : SecureTokenStore {
    private var githubToken: String? = null

    override suspend fun readGithubToken(): String? = githubToken

    override suspend fun writeGithubToken(token: String) {
        require(token.isNotBlank()) { "token must not be blank" }
        githubToken = token
    }

    override suspend fun clearGithubToken() {
        githubToken = null
    }
}
