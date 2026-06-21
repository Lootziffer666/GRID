package com.grid.feature.import_pipeline.data.github

/**
 * OAuth boundary for Gate 3.
 *
 * Network client implementation is intentionally deferred; only the contract
 * is introduced in this gate.
 */
interface GithubOAuthApi {
    suspend fun exchangeAuthorizationCode(code: String): String
}
