package com.painkiller.domain.error

/**
 * Gate 8 — classifies whether a failed operation may be retried and how.
 *
 * Painkiller never auto-retries a write. [SAFE_TO_RETRY] means the UI may
 * show a "Try again" button that the user must explicitly tap; it does not
 * authorise background re-execution.
 */
enum class RetrySafety {

    /** Network/transient failure — tapping "Try again" is safe. */
    SAFE_TO_RETRY,

    /**
     * The local plan is stale (e.g. SHA mismatch). The user must re-read the
     * branch state and rebuild the plan before retrying. Do not re-execute the
     * previous plan without user confirmation.
     */
    REQUIRES_PLAN_REFRESH,

    /**
     * User action is required (auth, permissions, path fix). Retrying the same
     * operation will produce the same failure.
     */
    NOT_RETRYABLE,
}
