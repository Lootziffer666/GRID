package com.painkiller.domain.model

/**
 * Severity classes used across Painkiller diagnostics.
 *
 * Defined in Gate 0 because both UI components (severity badge) and future
 * domain logic (Large File Doctor in Gate 2) will reference the same enum.
 *
 * No diagnosis logic is implemented in Gate 0. See `instructions.md` Gate 2.
 */
enum class DiagnosticSeverity {
    SAFE,
    WARNING,
    BLOCKED,
    DEFERRED,
}
