package com.painkiller.domain.github

/**
 * Structured failure types that the [GithubGitDataApi] HTTP implementation
 * (added in a later hardening step) is expected to throw.
 *
 * Gate 6's orchestrator catches these and maps them to a result variant the
 * UI can render in human language. Keeping the hierarchy sealed lets the
 * orchestrator exhaustively branch on it.
 *
 * No raw token, response body, or stack trace must be put into [message] —
 * Gate 8 will refine the user-facing copy. Gate 6 messages are short and
 * implementation-agnostic.
 */
sealed class GithubGitDataException(message: String) : RuntimeException(message) {

    /** 401 — token missing, expired, or revoked. */
    class AuthRequired(message: String = "GitHub authentication is required.") :
        GithubGitDataException(message)

    /** 403 (without "protected branch" hint) — token lacks scope or permission. */
    class PermissionDenied(message: String = "GitHub denied this operation for the current token.") :
        GithubGitDataException(message)

    /** 404 — branch / ref missing. */
    class RefNotFound(message: String = "GitHub branch or ref was not found.") :
        GithubGitDataException(message)

    /** 422 / 403 with protected-branch indicator. */
    class ProtectedBranch(message: String = "The target branch is protected and refused this update.") :
        GithubGitDataException(message)

    /** 422 from update-ref because the branch advanced to a different SHA. */
    class ShaMismatch(message: String = "The branch changed on GitHub while Painkiller was preparing this commit.") :
        GithubGitDataException(message)

    /** Transport-level failure (no network, DNS, TLS, timeout, GitHub unreachable). */
    class NetworkUnavailable(message: String = "GitHub could not be reached.") :
        GithubGitDataException(message)
}
