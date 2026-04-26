package com.painkiller.domain.error

/**
 * Gate 8 — the most useful next step for the user after a failure.
 *
 * One value per [HumanReadableError]. The UI may use it to show a contextual
 * action button or deep-link, but acting on the hint is always the user's
 * explicit choice.
 */
enum class RecoveryHint {

    /** Navigate to the sign-in screen. */
    SIGN_IN,

    /** Pick a branch that is not write-protected. */
    CHOOSE_DIFFERENT_BRANCH,

    /** Re-read the branch state and rebuild the upload plan from scratch. */
    REFRESH_PLAN,

    /** Verify token scopes or repository permissions on GitHub. */
    CHECK_PERMISSIONS,

    /** Verify network connectivity; retry once online. */
    CHECK_NETWORK,

    /** Remove or swap out files that exceed the normal-commit size limit. */
    REMOVE_LARGE_FILES,

    /** Correct file paths or names that failed validation. */
    FIX_FILE_PATHS,

    /** No specific action available; user may dismiss. */
    NO_ACTION,
}
