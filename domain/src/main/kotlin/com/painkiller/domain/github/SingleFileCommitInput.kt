package com.painkiller.domain.github

import com.painkiller.domain.target.RepoTarget

/**
 * Gate 6 input for [SingleFileCommitOrchestrator].
 *
 * Intentionally narrow — single file only. Multi-file, folder, and ZIP
 * uploads are Gate 7 scope.
 *
 * The caller is responsible for:
 * - sourcing the file bytes and base64-encoding them into [contentBase64]
 *   (Painkiller does not stream; files large enough to warrant streaming
 *   are blocked by the Large File Doctor before reaching Gate 6)
 * - selecting the [target] via the Gate 4 RepoTarget flow
 * - confirming size / blocked status with the Gate 2 Large File Doctor
 *   *before* invoking the orchestrator (the orchestrator does not re-check
 *   size; it commits whatever bytes it is handed)
 */
data class SingleFileCommitInput(
    /** Owner, repo, branch, and target folder. */
    val target: RepoTarget,

    /**
     * Repo-relative file name to commit. Must not contain `/` or `..`.
     * The orchestrator joins it onto [RepoTarget.targetPath] and re-validates
     * the combined path with [com.painkiller.domain.path.PathValidation].
     */
    val fileName: String,

    /** Base64-encoded file bytes. Must be non-empty for a valid blob. */
    val contentBase64: String,

    /** Single-line commit message. Must not be blank. */
    val commitMessage: String,
)
