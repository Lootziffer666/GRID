package com.painkiller.data.github

import com.painkiller.data.security.SecureTokenStore
import com.painkiller.domain.github.GithubGitDataApi
import com.painkiller.domain.github.PendingChange
import com.painkiller.domain.github.RepoTreeOrchestrator
import com.painkiller.domain.github.RepoTreeResult
import com.painkiller.domain.github.TreeEntry

/**
 * Android-side wrapper around [RepoTreeOrchestrator] for the repo file manager.
 *
 * Mirrors the [MultiFileCommitRepository] pattern:
 *   - reads the token from [SecureTokenStore]
 *   - short-circuits with an appropriate result when no token
 *   - otherwise delegates to domain orchestrator / API
 */
class RepoTreeRepository(
    private val gitDataApi: GithubGitDataApi,
    private val secureTokenStore: SecureTokenStore,
) {

    private val orchestrator = RepoTreeOrchestrator(gitDataApi)

    /**
     * Loads the recursive tree for a branch. Returns the flat list of [TreeEntry]
     * items on success, or a [RepoTreeResult.Failure] on error.
     */
    suspend fun loadTree(owner: String, repo: String, branch: String): RepoTreeLoadResult {
        val token = secureTokenStore.readGithubToken()
        if (token.isNullOrBlank()) {
            return RepoTreeLoadResult.AuthError("Not authenticated. Sign in to GitHub first.")
        }

        return try {
            val ref = gitDataApi.getRef(owner = owner, repo = repo, ref = "heads/$branch")
            val commit = gitDataApi.getCommit(owner = owner, repo = repo, commitSha = ref.obj.sha)
            val tree = gitDataApi.getTree(
                owner = owner,
                repo = repo,
                treeSha = commit.tree.sha,
                recursive = true,
            )
            RepoTreeLoadResult.Success(
                entries = tree.tree,
                truncated = tree.truncated,
            )
        } catch (e: com.painkiller.domain.github.GithubGitDataException.AuthRequired) {
            RepoTreeLoadResult.AuthError("GitHub authentication is required.")
        } catch (e: com.painkiller.domain.github.GithubGitDataException.RefNotFound) {
            RepoTreeLoadResult.NotFound("Branch '$branch' was not found.")
        } catch (e: com.painkiller.domain.github.GithubGitDataException) {
            RepoTreeLoadResult.NetworkError("Could not load the repository tree.")
        }
    }

    /**
     * Commits the given [changes] atomically to the specified branch using
     * the Git Data API (get ref, compile tree, create blobs/tree/commit, update ref).
     */
    suspend fun commitChanges(
        owner: String,
        repo: String,
        branch: String,
        changes: List<PendingChange>,
    ): RepoTreeResult {
        val token = secureTokenStore.readGithubToken()
        if (token.isNullOrBlank()) {
            return RepoTreeResult.AuthError("Not authenticated. Sign in to GitHub first.")
        }
        return orchestrator.execute(
            owner = owner,
            repo = repo,
            branch = branch,
            changes = changes,
        )
    }
}

sealed interface RepoTreeLoadResult {
    data class Success(
        val entries: List<TreeEntry>,
        val truncated: Boolean,
    ) : RepoTreeLoadResult

    sealed interface Failure : RepoTreeLoadResult {
        val message: String
    }

    data class AuthError(override val message: String) : Failure
    data class NotFound(override val message: String) : Failure
    data class NetworkError(override val message: String) : Failure
}
