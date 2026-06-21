package com.painkiller.domain.github

/**
 * Orchestrates file-management operations (move, rename, delete, create folder,
 * upload) as a single atomic commit using the GitHub Git Data API.
 *
 * Flow:
 *   1. Validate input (non-empty changes list).
 *   2. getRef         - resolve branch HEAD SHA.
 *   3. getCommit      - resolve the base tree SHA.
 *   4. getTree        - fetch the full recursive tree.
 *   5. Compile changes via [TreeChangeCompiler].
 *   6. Create blobs for UPLOAD entries with newContentBase64.
 *   7. createTree     - submit compiled entries with base tree.
 *   8. createCommit   - with auto-generated German summary message.
 *   9. updateRef      - advance branch (force=false, expectedSha=baseSha).
 *
 * The commit message is auto-generated in German based on change type counts,
 * e.g. "3 Dateien verschoben, 1 Ordner erstellt".
 */
class RepoTreeOrchestrator(
    private val gitDataApi: GithubGitDataApi,
) {

    suspend fun execute(
        owner: String,
        repo: String,
        branch: String,
        changes: List<PendingChange>,
    ): RepoTreeResult {
        if (changes.isEmpty()) {
            return RepoTreeResult.InvalidInput("No changes provided.")
        }

        val branchRef = "heads/$branch"
        val refForUpdate = "refs/heads/$branch"

        return try {
            // 1. Resolve branch HEAD
            val baseRef = gitDataApi.getRef(owner = owner, repo = repo, ref = branchRef)
            val baseSha = baseRef.obj.sha

            // 2. Get base commit to find tree SHA
            val baseCommit = gitDataApi.getCommit(owner = owner, repo = repo, commitSha = baseSha)
            val baseTreeSha = baseCommit.tree.sha

            // 3. Fetch full recursive tree
            val fullTree = gitDataApi.getTree(
                owner = owner,
                repo = repo,
                treeSha = baseTreeSha,
                recursive = true,
            )

            // 4. Compile changes
            val compiledEntries = try {
                TreeChangeCompiler.compile(changes, fullTree.tree)
            } catch (e: IllegalArgumentException) {
                return RepoTreeResult.InvalidInput(e.message ?: "Invalid change.")
            }

            // 5. Create blobs for entries that need them (UPLOAD with newContentBase64)
            val finalEntries = compiledEntries.map { entry ->
                if (entry.sha == null && entry.content != null && entry.content.isNotEmpty()) {
                    // This entry needs a blob created from base64 content
                    val blob = gitDataApi.createBlob(
                        owner = owner,
                        repo = repo,
                        request = CreateBlobRequest(
                            content = entry.content,
                            encoding = "base64",
                        ),
                    )
                    entry.copy(sha = blob.sha, content = null)
                } else {
                    entry
                }
            }

            // 6. Create tree
            val newTree = gitDataApi.createTree(
                owner = owner,
                repo = repo,
                request = CreateTreeRequest(baseTree = baseTreeSha, tree = finalEntries),
            )

            // 7. Create commit with auto-generated message
            val commitMessage = generateCommitMessage(changes)
            val commit = gitDataApi.createCommit(
                owner = owner,
                repo = repo,
                request = CreateCommitRequest(
                    message = commitMessage,
                    tree = newTree.sha,
                    parents = listOf(baseSha),
                ),
            )

            // 8. Update ref (force=false enforced)
            gitDataApi.updateRef(
                owner = owner,
                repo = repo,
                ref = refForUpdate,
                request = UpdateRefRequest(sha = commit.sha, force = false),
                expectedSha = baseSha,
            )

            RepoTreeResult.Success(
                commitSha = commit.sha,
                commitUrl = commit.htmlUrl,
                summary = commitMessage,
            )
        } catch (e: GithubGitDataException.AuthRequired) {
            RepoTreeResult.AuthError(e.message ?: "GitHub authentication is required.")
        } catch (e: GithubGitDataException.PermissionDenied) {
            RepoTreeResult.PermissionError(
                e.message ?: "GitHub denied this operation for the current token."
            )
        } catch (e: GithubGitDataException.RefNotFound) {
            RepoTreeResult.BranchNotFound(
                e.message ?: "GitHub branch or ref was not found."
            )
        } catch (e: GithubGitDataException.ProtectedBranch) {
            RepoTreeResult.ProtectedBranch(
                e.message ?: "The target branch is protected and refused this update."
            )
        } catch (e: GithubGitDataException.ShaMismatch) {
            RepoTreeResult.ShaMismatch(
                e.message
                    ?: "The branch changed on GitHub while Painkiller was preparing this commit."
            )
        } catch (e: GithubGitDataException.NetworkUnavailable) {
            RepoTreeResult.NetworkError(e.message ?: "GitHub could not be reached.")
        } catch (e: GithubGitDataException) {
            RepoTreeResult.UnknownError(
                e.message ?: "GitHub returned an unexpected error."
            )
        }
    }

    companion object {
        internal fun generateCommitMessage(changes: List<PendingChange>): String {
            val counts = changes.groupBy { it.type }.mapValues { it.value.size }
            val parts = mutableListOf<String>()

            counts[PendingChangeType.MOVE]?.let { count ->
                parts += "$count ${if (count == 1) "Datei verschoben" else "Dateien verschoben"}"
            }
            counts[PendingChangeType.RENAME]?.let { count ->
                parts += "$count ${if (count == 1) "Datei umbenannt" else "Dateien umbenannt"}"
            }
            counts[PendingChangeType.DELETE]?.let { count ->
                parts += "$count ${if (count == 1) "Datei entfernt" else "Dateien entfernt"}"
            }
            counts[PendingChangeType.CREATE_FOLDER]?.let { count ->
                parts += "$count ${if (count == 1) "Ordner erstellt" else "Ordner erstellt"}"
            }
            counts[PendingChangeType.UPLOAD]?.let { count ->
                parts += "$count ${if (count == 1) "Datei hochgeladen" else "Dateien hochgeladen"}"
            }

            return parts.joinToString(", ")
        }
    }
}
