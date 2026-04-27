package com.painkiller.data.github

import com.painkiller.data.security.SecureTokenStore
import com.painkiller.domain.github.GithubGitDataException
import com.painkiller.domain.github.GithubPullRequestApi
import com.painkiller.domain.github.GithubPullRequestDetail
import com.painkiller.domain.github.GithubPullRequestSummary
import com.painkiller.domain.github.MergePullRequestRequest
import com.painkiller.domain.github.MergePullRequestResponse

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

    suspend fun getPullRequest(owner: String, repo: String, number: Long): GithubPullRequestDetailResult {
        if (owner.isBlank() || repo.isBlank()) {
            return GithubPullRequestDetailResult.Failure("Owner and repo are required.")
        }
        secureTokenStore.readGithubToken()
            ?: return GithubPullRequestDetailResult.Failure("Sign in required.")
        return try {
            GithubPullRequestDetailResult.Success(api.getPullRequest(owner.trim(), repo.trim(), number))
        } catch (e: GithubGitDataException.AuthRequired) {
            GithubPullRequestDetailResult.Failure("Sign in required.")
        } catch (e: GithubGitDataException.PermissionDenied) {
            GithubPullRequestDetailResult.Failure("Token is missing permissions for pull requests.")
        } catch (e: GithubGitDataException.RefNotFound) {
            GithubPullRequestDetailResult.Failure("Pull request not found.")
        } catch (e: GithubGitDataException.NetworkUnavailable) {
            GithubPullRequestDetailResult.Failure("Could not reach GitHub.")
        } catch (e: Throwable) {
            GithubPullRequestDetailResult.Failure("Could not load pull request details.")
        }
    }

    suspend fun mergePullRequest(
        owner: String,
        repo: String,
        number: Long,
        method: PullRequestMergeMethod,
        expectedHeadSha: String?,
    ): GithubPullRequestMergeResult {
        if (owner.isBlank() || repo.isBlank()) {
            return GithubPullRequestMergeResult.Failure("Owner and repo are required.")
        }
        secureTokenStore.readGithubToken()
            ?: return GithubPullRequestMergeResult.Failure("Sign in required.")
        return try {
            val response = api.mergePullRequest(
                owner = owner.trim(),
                repo = repo.trim(),
                number = number,
                request = MergePullRequestRequest(
                    mergeMethod = method.apiValue,
                    sha = expectedHeadSha,
                ),
            )
            if (response.merged) {
                GithubPullRequestMergeResult.Success(response)
            } else {
                GithubPullRequestMergeResult.Failure(response.message.ifBlank { "Merge was rejected." })
            }
        } catch (e: GithubGitDataException.AuthRequired) {
            GithubPullRequestMergeResult.Failure("Sign in required.")
        } catch (e: GithubGitDataException.PermissionDenied) {
            GithubPullRequestMergeResult.Failure("Token is missing permissions to merge pull requests.")
        } catch (e: GithubGitDataException.RefNotFound) {
            GithubPullRequestMergeResult.Failure("Pull request not found.")
        } catch (e: GithubGitDataException.NetworkUnavailable) {
            GithubPullRequestMergeResult.Failure("Could not reach GitHub.")
        } catch (e: Throwable) {
            GithubPullRequestMergeResult.Failure("Could not complete merge.")
        }
    }
}

sealed interface GithubPullRequestListResult {
    data class Success(val pullRequests: List<GithubPullRequestSummary>) : GithubPullRequestListResult
    data class Failure(val reason: String) : GithubPullRequestListResult
}

sealed interface GithubPullRequestDetailResult {
    data class Success(val pullRequest: GithubPullRequestDetail) : GithubPullRequestDetailResult
    data class Failure(val reason: String) : GithubPullRequestDetailResult
}

sealed interface GithubPullRequestMergeResult {
    data class Success(val response: MergePullRequestResponse) : GithubPullRequestMergeResult
    data class Failure(val reason: String) : GithubPullRequestMergeResult
}

enum class PullRequestMergeMethod(val apiValue: String) {
    MERGE("merge"),
    SQUASH("squash"),
    REBASE("rebase"),
}
