package com.painkiller.domain.error

/**
 * Gate 8 — a fully user-facing error value ready for display.
 *
 * Produced by [PainkillerErrorMapper] from any Painkiller failure type.
 *
 * Invariants (enforced by [PainkillerErrorMapper]):
 *   - [title] is non-blank and ≤ 60 characters.
 *   - [detail] is non-blank and never contains a raw GitHub token.
 *   - [retrySafety] and [recoveryHint] are always set; no null escapes.
 *
 * The UI must not append internal error codes or exception class names to
 * [detail] before showing it to the user.
 */
data class HumanReadableError(
    /** Short headline suitable for a dialog title or error banner. */
    val title: String,
    /** One or two sentences explaining what happened and what to do. */
    val detail: String,
    /** Whether and how the user may retry this operation. */
    val retrySafety: RetrySafety,
    /** The most useful next step for the user. */
    val recoveryHint: RecoveryHint,
)
