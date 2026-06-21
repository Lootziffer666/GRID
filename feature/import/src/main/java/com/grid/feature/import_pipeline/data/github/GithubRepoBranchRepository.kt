package com.grid.feature.import_pipeline.data.github

import com.grid.feature.import_pipeline.data.security.SecureTokenStore
import com.painkiller.domain.github.GithubRepositoryApi

class GithubRepoBranchRepository(
    private val githubRepositoryApi: GithubRepositoryApi,
    private val secureTokenStore: SecureTokenStore
) {

    suspend fun listRepositories(): GithubRepoListResult {
        val token = secureTokenStore.readGithubToken()
        if (token.isNullOrBlank()) {
            return GithubRepoListResult.Failure("Not authenticated. Sign in to GitHub first.")
        }

        return runCatching { githubRepositoryApi.listRepositories() }
            .fold(
                onSuccess = { GithubRepoListResult.Success(it) },
                onFailure = { GithubRepoListResult.Failure("Failed to load repositories.") }
            )
    }

    suspend fun listBranches(owner: String, repo: String): GithubBranchListResult {
        val token = secureTokenStore.readGithubToken()
        if (token.isNullOrBlank()) {
            return GithubBranchListResult.Failure("Not authenticated. Sign in to GitHub first.")
        }
        if (owner.isBlank() || repo.isBlank()) {
            return GithubBranchListResult.Failure("Owner and repository are required.")
        }

        return runCatching { githubRepositoryApi.listBranches(owner = owner, repo = repo) }
            .fold(
                onSuccess = { GithubBranchListResult.Success(it) },
                onFailure = { GithubBranchListResult.Failure("Failed to load branches.") }
            )
    }
}
