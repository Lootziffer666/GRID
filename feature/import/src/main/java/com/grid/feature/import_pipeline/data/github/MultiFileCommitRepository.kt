package com.grid.feature.import_pipeline.data.github

import com.grid.feature.import_pipeline.data.security.SecureTokenStore
import com.painkiller.domain.github.GithubGitDataApi
import com.painkiller.domain.github.MultiFileCommitInput
import com.painkiller.domain.github.MultiFileCommitOrchestrator
import com.painkiller.domain.github.MultiFileCommitResult

/**
 * Gate 7 Android-side wrapper around [MultiFileCommitOrchestrator].
 *
 * Mirrors the Gate 6 [SingleFileCommitRepository] pattern:
 *   - reads the token from the [SecureTokenStore]
 *   - short-circuits with [MultiFileCommitResult.AuthError] when no token
 *   - otherwise delegates the multi-file Git Data API flow to the domain
 *     orchestrator
 *
 * The HTTP client implementation of [GithubGitDataApi] is wired in a later
 * hardening step (same pattern as Gate 3 / Gate 6).
 */
class MultiFileCommitRepository(
    private val gitDataApi: GithubGitDataApi,
    private val secureTokenStore: SecureTokenStore,
) {

    private val orchestrator = MultiFileCommitOrchestrator(gitDataApi)

    suspend fun commitMultipleFiles(input: MultiFileCommitInput): MultiFileCommitResult {
        val token = secureTokenStore.readGithubToken()
        if (token.isNullOrBlank()) {
            return MultiFileCommitResult.AuthError(
                "Not authenticated. Sign in to GitHub first."
            )
        }
        return orchestrator.execute(input)
    }
}
