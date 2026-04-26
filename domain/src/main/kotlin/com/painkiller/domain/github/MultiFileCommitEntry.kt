package com.painkiller.domain.github

/**
 * Gate 7 — one file entry in a multi-file commit.
 *
 * [repoPath] must be a normalized, validated GitHub tree path (no leading
 * slash, no traversal, `/`-separated). Callers must validate every path
 * with [com.painkiller.domain.path.PathValidation.isSafeRepoPath] before
 * constructing an entry. For ZIP sources this is the ZIP-Slip guard: any
 * entry whose path fails that check must be rejected before passing it here.
 *
 * [contentBase64] is the Base64-encoded file bytes. Pass an empty string for
 * empty files (e.g. the auto-injected `.gitkeep`). The orchestrator uses
 * UTF-8 encoding for the empty case and Base64 otherwise.
 *
 * Callers are responsible for Gate 2 size diagnosis before adding entries to
 * a [MultiFileCommitInput]. The orchestrator commits whatever bytes it receives.
 */
data class MultiFileCommitEntry(
    val repoPath: String,
    val contentBase64: String,
)
