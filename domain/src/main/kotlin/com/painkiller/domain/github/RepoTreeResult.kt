package com.painkiller.domain.github

/**
 * Result of a [RepoTreeOrchestrator] execution that applies file-management
 * changes (move, rename, delete, create folder, upload) as a single atomic
 * commit via the GitHub Git Data API.
 */
sealed interface RepoTreeResult {

    data class Success(
        val commitSha: String,
        val commitUrl: String?,
        val summary: String,
    ) : RepoTreeResult

    sealed interface Failure : RepoTreeResult {
        val message: String
    }

    data class InvalidInput(override val message: String) : Failure
    data class AuthError(override val message: String) : Failure
    data class PermissionError(override val message: String) : Failure
    data class BranchNotFound(override val message: String) : Failure
    data class ProtectedBranch(override val message: String) : Failure
    data class ShaMismatch(override val message: String) : Failure
    data class NetworkError(override val message: String) : Failure
    data class UnknownError(override val message: String) : Failure
}
