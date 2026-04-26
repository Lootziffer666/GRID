package com.painkiller.domain.error

import com.painkiller.domain.github.MultiFileCommitResult
import com.painkiller.domain.github.SingleFileCommitResult

/**
 * Gate 8 — maps all structured Painkiller failure types to [HumanReadableError].
 *
 * ## Token safety
 * [sanitize] must be called on any externally-sourced string before it enters
 * a [HumanReadableError]. All factory functions in this object apply it to
 * the detail text where external input could theoretically appear. Known GitHub
 * token prefixes (`ghp_`, `ghs_`, `gho_`, `github_pat_`) are replaced with
 * `[token redacted]` to prevent accidental leakage into UI or logs.
 *
 * ## No destructive auto-retry
 * [RetrySafety.SAFE_TO_RETRY] means the UI may offer a retry button that the
 * user explicitly taps. It never authorises background re-execution or any
 * write without user confirmation. [RetrySafety.REQUIRES_PLAN_REFRESH] means
 * the plan is stale and must be rebuilt before any new write.
 *
 * ## Coverage
 * - Gate 6 [SingleFileCommitResult.Failure] subtypes
 * - Gate 7 [MultiFileCommitResult.Failure] subtypes
 * - Gate 3 auth / repo / branch listing failures (plain-string reason)
 * - Blocked-file / upload-plan failures (pre-commit gate)
 */
object PainkillerErrorMapper {

    // ─── Gate 6 ───────────────────────────────────────────────────────────────

    fun map(failure: SingleFileCommitResult.Failure): HumanReadableError = when (failure) {
        is SingleFileCommitResult.InvalidInput -> invalidInput()
        is SingleFileCommitResult.AuthError -> authRequired()
        is SingleFileCommitResult.PermissionError -> permissionDenied()
        is SingleFileCommitResult.BranchNotFound -> branchNotFound()
        is SingleFileCommitResult.ProtectedBranch -> protectedBranch()
        is SingleFileCommitResult.ShaMismatch -> shaMismatch()
        is SingleFileCommitResult.NetworkError -> networkUnavailable()
        is SingleFileCommitResult.UnknownError -> unknownError()
    }

    // ─── Gate 7 ───────────────────────────────────────────────────────────────

    fun map(failure: MultiFileCommitResult.Failure): HumanReadableError = when (failure) {
        is MultiFileCommitResult.InvalidInput -> invalidInput()
        is MultiFileCommitResult.AuthError -> authRequired()
        is MultiFileCommitResult.PermissionError -> permissionDenied()
        is MultiFileCommitResult.BranchNotFound -> branchNotFound()
        is MultiFileCommitResult.ProtectedBranch -> protectedBranch()
        is MultiFileCommitResult.ShaMismatch -> shaMismatch()
        is MultiFileCommitResult.NetworkError -> networkUnavailable()
        is MultiFileCommitResult.UnknownError -> unknownError()
    }

    // ─── Gate 3 (plain-string failure reasons) ───────────────────────────────

    /**
     * Maps a Gate 3 OAuth exchange failure. [reason] is the value from
     * [com.painkiller.data.github.GithubAuthResult.Failure.reason].
     */
    fun mapAuthExchange(@Suppress("UNUSED_PARAMETER") reason: String): HumanReadableError =
        HumanReadableError(
            title = "Sign in failed",
            detail = "Painkiller could not complete the GitHub sign-in. Try signing in again.",
            retrySafety = RetrySafety.NOT_RETRYABLE,
            recoveryHint = RecoveryHint.SIGN_IN,
        )

    /**
     * Maps a Gate 3 repository-listing failure. [reason] is the value from
     * [com.painkiller.data.github.GithubRepoListResult.Failure.reason].
     */
    fun mapRepoListing(reason: String): HumanReadableError {
        return if (isAuthFailure(reason)) authRequired()
        else HumanReadableError(
            title = "Could not load repositories",
            detail = "Painkiller could not load your GitHub repositories. " +
                "Check your connection and try again.",
            retrySafety = RetrySafety.SAFE_TO_RETRY,
            recoveryHint = RecoveryHint.CHECK_NETWORK,
        )
    }

    /**
     * Maps a Gate 3 branch-listing failure. [reason] is the value from
     * [com.painkiller.data.github.GithubBranchListResult.Failure.reason].
     */
    fun mapBranchListing(reason: String): HumanReadableError {
        return if (isAuthFailure(reason)) authRequired()
        else HumanReadableError(
            title = "Could not load branches",
            detail = "Painkiller could not load branches for this repository. " +
                "Check your connection and try again.",
            retrySafety = RetrySafety.SAFE_TO_RETRY,
            recoveryHint = RecoveryHint.CHECK_NETWORK,
        )
    }

    // ─── Upload-plan gate (pre-commit) ────────────────────────────────────────

    /**
     * Maps a blocked-file condition from [com.painkiller.domain.upload.UploadPlan].
     * Called when [com.painkiller.domain.upload.UploadPlan.isBlockedForCommit] is
     * true and the user attempts to confirm. No API call is made in this case.
     */
    fun mapBlockedForCommit(): HumanReadableError = HumanReadableError(
        title = "Upload blocked",
        detail = "One or more files are too large for a normal GitHub commit. " +
            "Remove or replace the large files before uploading.",
        retrySafety = RetrySafety.NOT_RETRYABLE,
        recoveryHint = RecoveryHint.REMOVE_LARGE_FILES,
    )

    // ─── Token sanitization ───────────────────────────────────────────────────

    /**
     * Replaces known GitHub token patterns with `[token redacted]`.
     *
     * Call this before including any externally-sourced text in a
     * [HumanReadableError] or any user-visible log line.
     *
     * Covered patterns:
     *   - `ghp_…`  classic personal access token
     *   - `ghs_…`  GitHub App installation token
     *   - `gho_…`  GitHub OAuth token
     *   - `github_pat_…`  fine-grained PAT (2022+)
     *   - `Bearer <token>`  Authorization header value
     */
    fun sanitize(text: String): String =
        text
            .replace(TOKEN_REGEX, "[token redacted]")
            .replace(BEARER_REGEX, "Bearer [token redacted]")

    // ─── Private canonical factories ─────────────────────────────────────────

    private fun authRequired() = HumanReadableError(
        title = "Sign in required",
        detail = "Painkiller is not signed in to GitHub. Sign in and try again.",
        retrySafety = RetrySafety.NOT_RETRYABLE,
        recoveryHint = RecoveryHint.SIGN_IN,
    )

    private fun permissionDenied() = HumanReadableError(
        title = "Permission denied",
        detail = "GitHub refused this operation. Your account or token may not have " +
            "write access to this repository.",
        retrySafety = RetrySafety.NOT_RETRYABLE,
        recoveryHint = RecoveryHint.CHECK_PERMISSIONS,
    )

    private fun branchNotFound() = HumanReadableError(
        title = "Branch not found",
        detail = "The selected branch does not exist in this repository. " +
            "Choose a different branch and try again.",
        retrySafety = RetrySafety.NOT_RETRYABLE,
        recoveryHint = RecoveryHint.NO_ACTION,
    )

    private fun protectedBranch() = HumanReadableError(
        title = "Branch is protected",
        detail = "GitHub refused the update because this branch is protected. " +
            "Choose another branch or change the branch rules in GitHub.",
        retrySafety = RetrySafety.NOT_RETRYABLE,
        recoveryHint = RecoveryHint.CHOOSE_DIFFERENT_BRANCH,
    )

    private fun shaMismatch() = HumanReadableError(
        title = "Branch changed",
        detail = "The branch changed on GitHub while Painkiller was preparing the commit. " +
            "Refresh the plan and try again.",
        retrySafety = RetrySafety.REQUIRES_PLAN_REFRESH,
        recoveryHint = RecoveryHint.REFRESH_PLAN,
    )

    private fun networkUnavailable() = HumanReadableError(
        title = "Cannot reach GitHub",
        detail = "Painkiller could not reach GitHub. " +
            "Check your connection and retry when you are online.",
        retrySafety = RetrySafety.SAFE_TO_RETRY,
        recoveryHint = RecoveryHint.CHECK_NETWORK,
    )

    private fun invalidInput() = HumanReadableError(
        title = "Invalid file path",
        detail = "One or more file paths are not safe to commit. " +
            "Painkiller blocked the upload. Nothing was changed on GitHub.",
        retrySafety = RetrySafety.NOT_RETRYABLE,
        recoveryHint = RecoveryHint.FIX_FILE_PATHS,
    )

    private fun unknownError() = HumanReadableError(
        title = "Unexpected error",
        detail = "GitHub returned an unexpected response. Nothing was written. " +
            "Try again or check GitHub status.",
        retrySafety = RetrySafety.NOT_RETRYABLE,
        recoveryHint = RecoveryHint.NO_ACTION,
    )

    private fun isAuthFailure(reason: String) =
        reason.contains("authenticated", ignoreCase = true) ||
            reason.contains("sign in", ignoreCase = true) ||
            reason.contains("token", ignoreCase = true)

    private val TOKEN_REGEX = Regex(
        """(ghp_|ghs_|gho_|github_pat_)[A-Za-z0-9_]{8,}"""
    )
    private val BEARER_REGEX = Regex(
        """Bearer\s+[A-Za-z0-9._\-]{16,}"""
    )
}
