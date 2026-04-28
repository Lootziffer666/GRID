package com.painkiller.data.github

import com.painkiller.domain.github.GithubGitDataException
import com.painkiller.domain.github.SingleFileCommitInput
import com.painkiller.domain.github.SingleFileCommitResult
import com.painkiller.domain.lfs.LfsPointer
import com.painkiller.domain.target.RepoTarget

sealed interface GithubLfsUploadResult {
    data class Success(
        val commitSha: String,
        val commitUrl: String,
        val committedPath: String,
    ) : GithubLfsUploadResult

    data class Failure(
        val reason: String,
    ) : GithubLfsUploadResult
}

class GithubLfsRepository(
    private val lfsApi: KtorGithubLfsApi,
    private val singleFileCommitRepository: SingleFileCommitRepository,
) {

    suspend fun uploadSingleFileAndCommitPointer(
        target: RepoTarget,
        fileName: String,
        contentBase64: String,
        commitMessage: String,
    ): GithubLfsUploadResult {
        val bytes = try {
            java.util.Base64.getDecoder().decode(contentBase64)
        } catch (e: Throwable) {
            return GithubLfsUploadResult.Failure("Selected file could not be read for Git LFS upload.")
        }

        val plan = try {
            LfsPointer.buildPlan(bytes)
        } catch (e: Throwable) {
            return GithubLfsUploadResult.Failure("Could not build a valid Git LFS pointer for this file.")
        }

        val batch = try {
            lfsApi.requestUploadAction(
                owner = target.owner,
                repo = target.repo,
                oid = plan.oid.value,
                size = plan.sizeBytes,
                refName = target.branch.name,
            )
        } catch (e: GithubGitDataException.AuthRequired) {
            return GithubLfsUploadResult.Failure("Git LFS authentication failed. Sign in again and retry.")
        } catch (e: GithubGitDataException.PermissionDenied) {
            return GithubLfsUploadResult.Failure("Git LFS upload is not permitted for this repository/token.")
        } catch (e: Throwable) {
            return GithubLfsUploadResult.Failure("Git LFS batch request failed before pointer commit. Nothing was changed on GitHub.")
        }

        val objectResult = batch.objects.firstOrNull { it.oid == plan.oid.value }
            ?: return GithubLfsUploadResult.Failure(
                "Git LFS server did not return upload instructions for this object. Nothing was changed on GitHub.",
            )

        val error = objectResult.error
        if (error != null) {
            val mapped = if (error.code == 507 || error.message.contains("quota", ignoreCase = true)) {
                "Git LFS quota or storage limit was reached. Pointer was not committed."
            } else {
                "Git LFS server rejected this object: ${error.message}. Pointer was not committed."
            }
            return GithubLfsUploadResult.Failure(mapped)
        }

        val actions = objectResult.actions
            ?: return GithubLfsUploadResult.Failure(
                "Git LFS response was missing upload actions. Pointer was not committed.",
            )
        val uploadAction = actions.upload
            ?: return GithubLfsUploadResult.Failure(
                "Git LFS response did not include an upload action. Pointer was not committed.",
            )
        val uploadHref = uploadAction.href
        if (uploadHref.isBlank()) {
            return GithubLfsUploadResult.Failure(
                "Git LFS upload URL was missing. Pointer was not committed.",
            )
        }
        try {
            lfsApi.uploadObject(uploadAction, bytes)
            val verifyAction = actions.verify
            if (verifyAction != null) {
                lfsApi.verifyObject(verifyAction, plan.oid.value, plan.sizeBytes)
            }
        } catch (e: Throwable) {
            return GithubLfsUploadResult.Failure(
                "LFS upload failed before the pointer file was committed. Nothing was changed on GitHub.",
            )
        }

        val pointerBase64 = java.util.Base64.getEncoder().encodeToString(plan.pointerText.toByteArray())
        val commitResult = singleFileCommitRepository.commitSingleFile(
            SingleFileCommitInput(
                target = target,
                fileName = fileName,
                contentBase64 = pointerBase64,
                commitMessage = commitMessage,
            ),
        )
        return when (commitResult) {
            is SingleFileCommitResult.Success -> GithubLfsUploadResult.Success(
                commitSha = commitResult.commitSha,
                commitUrl = commitResult.commitUrl
                    ?: return GithubLfsUploadResult.Failure(
                        "Pointer commit finished without a commit URL. Treating as failed for safety.",
                    ),
                committedPath = commitResult.committedPath,
            )
            is SingleFileCommitResult.ShaMismatch -> GithubLfsUploadResult.Failure(
                "Branch changed before pointer commit (SHA mismatch). LFS object may be uploaded, but repo pointer was not updated.",
            )
            else -> GithubLfsUploadResult.Failure(
                "Pointer commit failed after LFS upload. GitHub branch was not updated.",
            )
        }
    }
}
