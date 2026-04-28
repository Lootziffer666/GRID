package com.painkiller.data.github

import com.painkiller.data.security.SecureTokenStore
import com.painkiller.domain.github.GithubGitDataApi
import com.painkiller.domain.github.SingleFileCommitInput
import com.painkiller.domain.github.SingleFileCommitOrchestrator
import com.painkiller.domain.github.SingleFileCommitResult

/**
 * Gate 6 Android-side wrapper around [SingleFileCommitOrchestrator].
 *
 * Mirrors the Gate 3 pattern (`GithubRepoBranchRepository`):
 *   - reads the token from the [SecureTokenStore]
 *   - short-circuits with [SingleFileCommitResult.AuthError] when no token
 *   - otherwise delegates the 6-step Git Data API flow to the domain
 *     orchestrator
 *
 * The HTTP client implementation of [GithubGitDataApi] is wired in a later
 * hardening step (same pattern as `GithubOAuthApi` and `GithubRepositoryApi`
 * from Gate 3 — Gate 6 ships the orchestration and contract, the network
 * transport lands separately).
 */
open class SingleFileCommitRepository(
    private val gitDataApi: GithubGitDataApi,
    private val secureTokenStore: SecureTokenStore,
) {

    private val orchestrator = SingleFileCommitOrchestrator(gitDataApi)

    open suspend fun commitSingleFile(input: SingleFileCommitInput): SingleFileCommitResult {
        val token = secureTokenStore.readGithubToken()
        if (token.isNullOrBlank()) {
            return SingleFileCommitResult.AuthError(
                "Not authenticated. Sign in to GitHub first."
            )
        }
        return orchestrator.execute(input)
    }
}
