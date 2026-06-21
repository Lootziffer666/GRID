package com.grid.feature.import_pipeline.data.github

import com.grid.feature.import_pipeline.data.security.SecureTokenStore
import com.painkiller.domain.github.CreateReleaseRequest
import com.painkiller.domain.github.GithubGitDataException
import com.painkiller.domain.github.GithubReleaseAssetSummary
import com.painkiller.domain.github.GithubReleaseApi
import com.painkiller.domain.github.ReleaseAssetValidation
import com.painkiller.domain.github.GithubReleaseSummary
import com.painkiller.domain.github.UploadReleaseAssetRequest

class GithubReleaseRepository(
    private val api: GithubReleaseApi,
    private val secureTokenStore: SecureTokenStore,
) {
    suspend fun listReleases(owner: String, repo: String): GithubReleaseListResult {
        if (owner.isBlank() || repo.isBlank()) {
            return GithubReleaseListResult.Failure("Owner and repo are required.")
        }
        secureTokenStore.readGithubToken() ?: return GithubReleaseListResult.Failure("Sign in required.")
        return try {
            GithubReleaseListResult.Success(api.listReleases(owner.trim(), repo.trim()))
        } catch (e: GithubGitDataException.AuthRequired) {
            GithubReleaseListResult.Failure("Sign in required.")
        } catch (e: GithubGitDataException.PermissionDenied) {
            GithubReleaseListResult.Failure("Token is missing permissions for releases.")
        } catch (e: GithubGitDataException.RefNotFound) {
            GithubReleaseListResult.Failure("Repository not found.")
        } catch (e: GithubGitDataException.NetworkUnavailable) {
            GithubReleaseListResult.Failure("Could not reach GitHub.")
        } catch (e: Throwable) {
            GithubReleaseListResult.Failure("Could not load releases.")
        }
    }

    suspend fun createRelease(
        owner: String,
        repo: String,
        request: CreateReleaseRequest,
    ): GithubReleaseCreateResult {
        if (owner.isBlank() || repo.isBlank()) {
            return GithubReleaseCreateResult.Failure("Owner and repo are required.")
        }
        if (request.tagName.isBlank()) {
            return GithubReleaseCreateResult.Failure("Release tag is required.")
        }
        secureTokenStore.readGithubToken() ?: return GithubReleaseCreateResult.Failure("Sign in required.")
        return try {
            GithubReleaseCreateResult.Success(api.createRelease(owner.trim(), repo.trim(), request))
        } catch (e: GithubGitDataException.AuthRequired) {
            GithubReleaseCreateResult.Failure("Sign in required.")
        } catch (e: GithubGitDataException.PermissionDenied) {
            GithubReleaseCreateResult.Failure("Token is missing permissions for releases.")
        } catch (e: GithubGitDataException.RefNotFound) {
            GithubReleaseCreateResult.Failure("Repository not found.")
        } catch (e: GithubGitDataException.NetworkUnavailable) {
            GithubReleaseCreateResult.Failure("Could not reach GitHub.")
        } catch (e: Throwable) {
            GithubReleaseCreateResult.Failure("Could not create release.")
        }
    }

    suspend fun uploadReleaseAsset(
        owner: String,
        repo: String,
        releaseId: Long,
        request: UploadReleaseAssetRequest,
    ): GithubReleaseAssetUploadResult {
        if (owner.isBlank() || repo.isBlank() || releaseId <= 0L) {
            return GithubReleaseAssetUploadResult.Failure("Owner, repo, and release are required.")
        }
        when (ReleaseAssetValidation.validate(request)) {
            ReleaseAssetValidation.ValidationError.NameRequired -> {
                return GithubReleaseAssetUploadResult.Failure("Asset file name is required.")
            }

            ReleaseAssetValidation.ValidationError.ContentTypeRequired -> {
                return GithubReleaseAssetUploadResult.Failure("Asset content type is required.")
            }

            ReleaseAssetValidation.ValidationError.DataRequired -> {
                return GithubReleaseAssetUploadResult.Failure("Asset data is empty.")
            }

            null -> Unit
        }
        secureTokenStore.readGithubToken() ?: return GithubReleaseAssetUploadResult.Failure("Sign in required.")
        return try {
            GithubReleaseAssetUploadResult.Success(
                api.uploadReleaseAsset(owner.trim(), repo.trim(), releaseId, request),
            )
        } catch (e: GithubGitDataException.AuthRequired) {
            GithubReleaseAssetUploadResult.Failure("Sign in required.")
        } catch (e: GithubGitDataException.PermissionDenied) {
            GithubReleaseAssetUploadResult.Failure("Token is missing permissions for release uploads.")
        } catch (e: GithubGitDataException.RefNotFound) {
            GithubReleaseAssetUploadResult.Failure("Release or repository not found.")
        } catch (e: GithubGitDataException.NetworkUnavailable) {
            GithubReleaseAssetUploadResult.Failure("Could not reach GitHub.")
        } catch (e: Throwable) {
            GithubReleaseAssetUploadResult.Failure("Could not stream upload release asset. GitHub repo files were not changed.")
        }
    }
}

sealed interface GithubReleaseListResult {
    data class Success(val releases: List<GithubReleaseSummary>) : GithubReleaseListResult
    data class Failure(val reason: String) : GithubReleaseListResult
}

sealed interface GithubReleaseCreateResult {
    data class Success(val release: GithubReleaseSummary) : GithubReleaseCreateResult
    data class Failure(val reason: String) : GithubReleaseCreateResult
}

sealed interface GithubReleaseAssetUploadResult {
    data class Success(val asset: GithubReleaseAssetSummary) : GithubReleaseAssetUploadResult
    data class Failure(val reason: String) : GithubReleaseAssetUploadResult
}
