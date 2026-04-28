package com.painkiller.data.github

import com.painkiller.domain.github.GithubGitDataException
import com.painkiller.domain.github.SingleFileCommitInput
import com.painkiller.domain.github.SingleFileCommitResult
import com.painkiller.domain.github.UploadPayload
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
        payload: UploadPayload,
        commitMessage: String,
    ): GithubLfsUploadResult {
        val plan = try {
            payload.openStream().use { LfsPointer.buildPlanFromStream(it) }
        } catch (e: Throwable) {
            return GithubLfsUploadResult.Failure("Selected file stream could not be read for Git LFS upload. Nothing was changed on GitHub.")
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
                "Git LFS quota or storage limit was reached. Pointer was not committed. GitHub repo files were not updated."
            } else {
                "Git LFS server rejected this object: ${error.message}. Pointer was not committed. GitHub repo files were not updated."
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
        if (uploadAction.href.isBlank()) {
            return GithubLfsUploadResult.Failure(
                "Git LFS upload URL was missing. Pointer was not committed.",
            )
        }

        try {
            lfsApi.uploadObject(uploadAction, payload)
            actions.verify?.let { verifyAction ->
                lfsApi.verifyObject(verifyAction, plan.oid.value, plan.sizeBytes)
            }
        } catch (e: Throwable) {
            return GithubLfsUploadResult.Failure(
                "LFS upload stream failed before the pointer file was committed. GitHub repo files were not updated.",
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
                "Branch changed before pointer commit (SHA mismatch). LFS object may exist server-side, but GitHub repo files were not updated.",
            )

            else -> GithubLfsUploadResult.Failure(
                "Pointer commit failed after LFS upload. GitHub repo files were not updated.",
            )
        }
    }
}
