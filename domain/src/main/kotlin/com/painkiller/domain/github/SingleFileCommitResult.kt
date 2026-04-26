package com.painkiller.domain.github

/**
 * Gate 6 outcome of one single-file commit attempt.
 *
 * On [Success], the new branch HEAD is the commit Painkiller created and the
 * earlier branch state is unchanged in any user-visible way — the safety
 * contract is "one complete commit or no visible branch update".
 *
 * On any [Failure], the orchestrator stopped before updating the branch ref,
 * so the visible repo state is identical to what it was before the call.
 * Gate 8 will widen this surface; Gate 6 covers exactly the failure modes
 * required by `instructions.md` § Gate 6.
 */
sealed interface SingleFileCommitResult {

    data class Success(
        /** SHA of the new commit. */
        val commitSha: String,
        /** Browseable URL on github.com, when the API returned one. */
        val commitUrl: String?,
        /** Final repo path of the committed file (folder + file name). */
        val committedPath: String,
    ) : SingleFileCommitResult

    sealed interface Failure : SingleFileCommitResult {
        /** Short, user-facing explanation. Never contains a token. */
        val message: String
    }

    /** Caller-side problem: blank file name, blank commit message, unsafe path. */
    data class InvalidInput(override val message: String) : Failure

    data class AuthError(override val message: String) : Failure
    data class PermissionError(override val message: String) : Failure
    data class BranchNotFound(override val message: String) : Failure
    data class ProtectedBranch(override val message: String) : Failure
    data class ShaMismatch(override val message: String) : Failure
    data class NetworkError(override val message: String) : Failure

    /** Unmapped API error. Gate 8 will narrow this. */
    data class UnknownError(override val message: String) : Failure
}
