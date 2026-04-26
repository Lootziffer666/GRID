package com.painkiller.domain.github

import com.painkiller.domain.path.PathValidation

/**
 * Gate 6 — single-file commit via the GitHub Git Data API.
 *
 * Orchestrates the safe sequence required by `instructions.md` § Gate 6:
 *
 *   1. get ref          (resolve branch HEAD SHA)
 *   2. get base commit  (resolve base tree SHA)
 *   3. create blob      (upload file bytes)
 *   4. create tree      (one-entry tree on top of base tree)
 *   5. create commit    (one parent, the previous HEAD)
 *   6. update ref       (advance branch to the new commit, expecting the
 *                        original HEAD SHA — any drift is a SHA mismatch)
 *
 * The branch ref is only touched in step 6. Any failure before step 6
 * leaves the repository visibly unchanged. Step 6 itself uses
 * `expectedSha` so a concurrent push from someone else surfaces as
 * [SingleFileCommitResult.ShaMismatch] instead of an overwrite.
 *
 * Gate 6 deliberately does not retry, queue, batch, or conflict-resolve.
 * Multi-file flows are Gate 7. Robustness/error UX polish is Gate 8.
 */
class SingleFileCommitOrchestrator(
    private val gitDataApi: GithubGitDataApi,
) {

    suspend fun execute(input: SingleFileCommitInput): SingleFileCommitResult {
        val validationFailure = validate(input)
        if (validationFailure != null) return validationFailure

        val committedPath = joinPath(
            folder = input.target.targetPath.normalized,
            fileName = input.fileName.trim(),
        )

        // PathValidation already accepted the joined path during validate(),
        // so committedPath is guaranteed to be a normalized GitHub tree path.

        return runFlow(input, committedPath)
    }

    private fun validate(input: SingleFileCommitInput): SingleFileCommitResult.Failure? {
        val fileName = input.fileName.trim()
        if (fileName.isEmpty()) {
            return SingleFileCommitResult.InvalidInput("File name is required.")
        }
        if ('/' in fileName || '\\' in fileName) {
            return SingleFileCommitResult.InvalidInput(
                "File name must not contain path separators. Use the target folder for nesting."
            )
        }
        if (fileName == "." || fileName == "..") {
            return SingleFileCommitResult.InvalidInput("File name is not allowed.")
        }
        if (input.contentBase64.isEmpty()) {
            return SingleFileCommitResult.InvalidInput("File content is empty.")
        }
        if (input.commitMessage.isBlank()) {
            return SingleFileCommitResult.InvalidInput("Commit message is required.")
        }

        val joined = joinPath(input.target.targetPath.normalized, fileName)
        if (!PathValidation.isSafeRepoPath(joined)) {
            return SingleFileCommitResult.InvalidInput(
                "The computed repository path is not safe to commit."
            )
        }
        return null
    }

    private fun joinPath(folder: String, fileName: String): String =
        if (folder.isEmpty()) fileName else "$folder/$fileName"

    private suspend fun runFlow(
        input: SingleFileCommitInput,
        committedPath: String,
    ): SingleFileCommitResult {
        val owner = input.target.owner
        val repo = input.target.repo
        val branchRef = "heads/${input.target.branch.name}"
        val refForUpdate = "refs/heads/${input.target.branch.name}"

        return try {
            val baseRef = gitDataApi.getRef(owner = owner, repo = repo, ref = branchRef)
            val baseSha = baseRef.obj.sha

            val baseCommit = gitDataApi.getCommit(owner = owner, repo = repo, commitSha = baseSha)
            val baseTreeSha = baseCommit.tree.sha

            val blob = gitDataApi.createBlob(
                owner = owner,
                repo = repo,
                request = CreateBlobRequest(content = input.contentBase64, encoding = "base64"),
            )

            val tree = gitDataApi.createTree(
                owner = owner,
                repo = repo,
                request = CreateTreeRequest(
                    baseTree = baseTreeSha,
                    tree = listOf(
                        TreeEntry(
                            path = committedPath,
                            mode = TreeEntry.MODE_FILE,
                            type = TreeEntry.TYPE_BLOB,
                            sha = blob.sha,
                        )
                    ),
                ),
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

            SingleFileCommitResult.Success(
                commitSha = commit.sha,
                commitUrl = commit.htmlUrl,
                committedPath = committedPath,
            )
        } catch (e: GithubGitDataException.AuthRequired) {
            SingleFileCommitResult.AuthError(e.message ?: "GitHub authentication is required.")
        } catch (e: GithubGitDataException.PermissionDenied) {
            SingleFileCommitResult.PermissionError(
                e.message ?: "GitHub denied this operation for the current token."
            )
        } catch (e: GithubGitDataException.RefNotFound) {
            SingleFileCommitResult.BranchNotFound(
                e.message ?: "GitHub branch or ref was not found."
            )
        } catch (e: GithubGitDataException.ProtectedBranch) {
            SingleFileCommitResult.ProtectedBranch(
                e.message ?: "The target branch is protected and refused this update."
            )
        } catch (e: GithubGitDataException.ShaMismatch) {
            SingleFileCommitResult.ShaMismatch(
                e.message
                    ?: "The branch changed on GitHub while Painkiller was preparing this commit."
            )
        } catch (e: GithubGitDataException.NetworkUnavailable) {
            SingleFileCommitResult.NetworkError(e.message ?: "GitHub could not be reached.")
        } catch (e: GithubGitDataException) {
            // Future GithubGitDataException variants land here until Gate 8 widens the mapping.
            SingleFileCommitResult.UnknownError(
                e.message ?: "GitHub returned an unexpected error."
            )
        }
    }
}
