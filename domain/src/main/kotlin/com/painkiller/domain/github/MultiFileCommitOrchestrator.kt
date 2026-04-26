package com.painkiller.domain.github

import com.painkiller.domain.path.PathValidation

/**
 * Gate 7 — multi-file atomic commit via the GitHub Git Data API.
 *
 * Extends the Gate 6 single-file flow to commit N files in one atomic operation:
 *
 *   1. Validate all inputs (paths, commit message, no duplicates).
 *   2. Determine effective entries: if the caller passed no entries, inject a
 *      `.gitkeep` at the target path so the folder appears on GitHub.
 *   3. get ref          (resolve branch HEAD SHA = baseSha)
 *   4. get base commit  (resolve base tree SHA)
 *   5. create blob      (one per effective entry, in sorted path order)
 *   6. create tree      (all entries in one call, on top of base tree)
 *   7. create commit    (single parent = baseSha)
 *   8. update ref       (advance branch to new commit; force=false,
 *                        expectedSha=baseSha so concurrent pushes surface
 *                        as [MultiFileCommitResult.ShaMismatch])
 *
 * The branch ref is only touched in step 8. Any failure before step 8 leaves
 * the repository visibly unchanged. All blobs are uploaded before the tree is
 * assembled, so no partial tree can exist in the repository.
 *
 * ZIP-Slip prevention: every [MultiFileCommitEntry.repoPath] is validated via
 * [PathValidation.isSafeRepoPath] before any API call. Entries whose paths
 * contain traversal segments (`..`) or absolute roots are rejected with
 * [MultiFileCommitResult.InvalidInput]. Callers constructing entries from ZIP
 * central-directory metadata must normalize each entry path before passing it
 * here; passing a dangerous path is a hard rejection, not a recoverable error.
 *
 * Gate 7 does not retry, queue, or conflict-resolve. Robustness is Gate 8.
 */
class MultiFileCommitOrchestrator(
    private val gitDataApi: GithubGitDataApi,
) {

    companion object {
        private const val GITKEEP_NAME = ".gitkeep"
    }

    suspend fun execute(input: MultiFileCommitInput): MultiFileCommitResult {
        val validationFailure = validate(input)
        if (validationFailure != null) return validationFailure

        val effectiveEntries = buildEffectiveEntries(input)

        val owner = input.target.owner
        val repo = input.target.repo
        val branchRef = "heads/${input.target.branch.name}"
        val refForUpdate = "refs/heads/${input.target.branch.name}"

        return try {
            val baseRef = gitDataApi.getRef(owner = owner, repo = repo, ref = branchRef)
            val baseSha = baseRef.obj.sha

            val baseCommit = gitDataApi.getCommit(owner = owner, repo = repo, commitSha = baseSha)
            val baseTreeSha = baseCommit.tree.sha

            val treeEntries = effectiveEntries.map { entry ->
                val blobRequest = if (entry.contentBase64.isEmpty()) {
                    // Empty file (e.g. auto-injected .gitkeep) — send as UTF-8 empty string.
                    CreateBlobRequest(content = "", encoding = "utf-8")
                } else {
                    CreateBlobRequest(content = entry.contentBase64, encoding = "base64")
                }
                val blob = gitDataApi.createBlob(owner = owner, repo = repo, request = blobRequest)
                TreeEntry(
                    path = entry.repoPath,
                    mode = TreeEntry.MODE_FILE,
                    type = TreeEntry.TYPE_BLOB,
                    sha = blob.sha,
                )
            }

            val tree = gitDataApi.createTree(
                owner = owner,
                repo = repo,
                request = CreateTreeRequest(baseTree = baseTreeSha, tree = treeEntries),
            )

            val commit = gitDataApi.createCommit(
                owner = owner,
                repo = repo,
                request = CreateCommitRequest(
                    message = input.commitMessage,
                    tree = tree.sha,
                    parents = listOf(baseSha),
                ),
            )

            gitDataApi.updateRef(
                owner = owner,
                repo = repo,
                ref = refForUpdate,
                request = UpdateRefRequest(sha = commit.sha, force = false),
                expectedSha = baseSha,
            )

            MultiFileCommitResult.Success(
                commitSha = commit.sha,
                commitUrl = commit.htmlUrl,
                committedPaths = effectiveEntries.map { it.repoPath },
            )
        } catch (e: GithubGitDataException.AuthRequired) {
            MultiFileCommitResult.AuthError(e.message ?: "GitHub authentication is required.")
        } catch (e: GithubGitDataException.PermissionDenied) {
            MultiFileCommitResult.PermissionError(
                e.message ?: "GitHub denied this operation for the current token."
            )
        } catch (e: GithubGitDataException.RefNotFound) {
            MultiFileCommitResult.BranchNotFound(
                e.message ?: "GitHub branch or ref was not found."
            )
        } catch (e: GithubGitDataException.ProtectedBranch) {
            MultiFileCommitResult.ProtectedBranch(
                e.message ?: "The target branch is protected and refused this update."
            )
        } catch (e: GithubGitDataException.ShaMismatch) {
            MultiFileCommitResult.ShaMismatch(
                e.message
                    ?: "The branch changed on GitHub while Painkiller was preparing this commit."
            )
        } catch (e: GithubGitDataException.NetworkUnavailable) {
            MultiFileCommitResult.NetworkError(e.message ?: "GitHub could not be reached.")
        } catch (e: GithubGitDataException) {
            MultiFileCommitResult.UnknownError(
                e.message ?: "GitHub returned an unexpected error."
            )
        }
    }

    private fun validate(input: MultiFileCommitInput): MultiFileCommitResult.Failure? {
        if (input.commitMessage.isBlank()) {
            return MultiFileCommitResult.InvalidInput("Commit message is required.")
        }

        for (entry in input.entries) {
            // ZIP-Slip guard: the entry's repoPath must already be in normalized
            // canonical form (no leading slash, no `..`, no duplicate slashes,
            // no Windows drive prefix). Any path that would change under
            // normalization is suspicious and is rejected before any API call.
            val normalized = PathValidation.normalizeRepoPath(entry.repoPath)
            if (normalized == null || normalized.isBlank() || normalized != entry.repoPath) {
                return MultiFileCommitResult.InvalidInput(
                    "File path is not safe to commit: \"${entry.repoPath}\". " +
                        "This ZIP contains a path that tries to escape the target folder. " +
                        "Painkiller blocked it before upload. Nothing was changed on GitHub."
                )
            }
        }

        val seen = mutableSetOf<String>()
        for (entry in input.entries) {
            if (!seen.add(entry.repoPath)) {
                return MultiFileCommitResult.InvalidInput(
                    "Duplicate repository path: \"${entry.repoPath}\"."
                )
            }
        }

        return null
    }

    /**
     * Returns the final list of entries to commit, sorted deterministically by
     * [MultiFileCommitEntry.repoPath]. If the caller provided no entries, injects
     * a single `.gitkeep` at the target path so the folder is visible on GitHub.
     */
    private fun buildEffectiveEntries(input: MultiFileCommitInput): List<MultiFileCommitEntry> {
        if (input.entries.isEmpty()) {
            val gitkeepPath = if (input.target.targetPath.normalized.isEmpty()) {
                GITKEEP_NAME
            } else {
                "${input.target.targetPath.normalized}/$GITKEEP_NAME"
            }
            return listOf(MultiFileCommitEntry(repoPath = gitkeepPath, contentBase64 = ""))
        }
        return input.entries.sortedBy { it.repoPath }
    }
}
