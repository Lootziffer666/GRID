package com.painkiller.data.github

import com.painkiller.data.security.SecureTokenStore
import com.painkiller.domain.github.GithubGitDataException
import com.painkiller.domain.github.GithubPullRequestApi
import com.painkiller.domain.github.GithubPullRequestSummary

class GithubPullRequestRepository(
    private val api: GithubPullRequestApi,
    private val secureTokenStore: SecureTokenStore,
) {
    suspend fun listOpenPullRequests(owner: String, repo: String): GithubPullRequestListResult {
        if (owner.isBlank() || repo.isBlank()) {
            return GithubPullRequestListResult.Failure("Owner and repo are required.")
        }
        secureTokenStore.readGithubToken()
            ?: return GithubPullRequestListResult.Failure("Sign in required.")
        return try {
            // token pre-check above keeps UX message explicit; API still re-checks.
            val prs = api.listPullRequests(owner.trim(), repo.trim(), state = "open")
            GithubPullRequestListResult.Success(prs)
        } catch (e: GithubGitDataException.AuthRequired) {
            GithubPullRequestListResult.Failure("Sign in required.")
        } catch (e: GithubGitDataException.PermissionDenied) {
            GithubPullRequestListResult.Failure("Token is missing permissions for pull requests.")
        } catch (e: GithubGitDataException.RefNotFound) {
            GithubPullRequestListResult.Failure("Repository not found.")
        } catch (e: GithubGitDataException.NetworkUnavailable) {
            GithubPullRequestListResult.Failure("Could not reach GitHub.")
        } catch (e: Throwable) {
            GithubPullRequestListResult.Failure("Could not load pull requests.")
        }
    }
}

sealed interface GithubPullRequestListResult {
    data class Success(val pullRequests: List<GithubPullRequestSummary>) : GithubPullRequestListResult
    data class Failure(val reason: String) : GithubPullRequestListResult
}
