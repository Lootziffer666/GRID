package com.painkiller.domain.github

/**
 * Gate 7 result for a multi-file atomic commit.
 *
 * [Success] — all files were committed atomically. [committedPaths] lists
 * every repo-tree path written, including any auto-injected `.gitkeep`.
 *
 * Failure variants mirror [SingleFileCommitResult] so callers can handle
 * both with a common Failure branch if needed.
 */
sealed interface MultiFileCommitResult {

    data class Success(
        val commitSha: String,
        val commitUrl: String?,
        val committedPaths: List<String>,
    ) : MultiFileCommitResult

    sealed interface Failure : MultiFileCommitResult {
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
