package com.painkiller.app.domain.model

/**
 * Painkiller diagnosis severity classes.
 *
 * Defined in `instructions.md` § "Large File Doctor Rules" and reused
 * across the upload pipeline:
 *
 * - SAFE     — operation can proceed without warnings.
 * - WARNING  — operation can proceed but the user should know.
 * - BLOCKED  — operation must not proceed in v0.
 * - DEFERRED — handled by a future module (Git LFS, Release Assets, ...).
 */
enum class DiagnosticSeverity {
    SAFE,
    WARNING,
    BLOCKED,
    DEFERRED,
}
