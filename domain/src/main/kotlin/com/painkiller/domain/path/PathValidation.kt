package com.painkiller.domain.path

/**
 * Dependency-free path validation helpers for GitHub tree paths and upload
 * target paths.
 *
 * Rules (from instructions.md Gate 1 and Gate 4):
 *   - use `/` separators
 *   - collapse duplicate slashes
 *   - reject `.` and `..` traversal segments
 *   - reject absolute local paths (leading `/` or a Windows drive letter)
 *   - blank normalizes to empty string (root of target folder)
 *
 * This logic lives in `:domain` so it can be unit-tested without the
 * Android SDK and reused across Gates 1, 4, and 7 without duplication.
 * Full integration with FilePlan and TargetPath lands in those later gates.
 */
object PathValidation {

    /**
     * Normalize a target repo path to GitHub's expected form.
     *
     * Returns `null` when the path is unsafe (traversal, absolute local path).
     * Callers must treat `null` as a hard rejection — never silently retry
     * with a guessed "fix".
     *
     * Returns `""` for blank input, meaning the root of the chosen target folder.
     */
    fun normalizeRepoPath(raw: String): String? {
        if (raw.isBlank()) return ""
        val unified = raw.replace('\\', '/')
        if (unified.matches(Regex("^[A-Za-z]:.*"))) return null // Windows absolute
        val trimmed = unified.trim('/')
        if (trimmed.isEmpty()) return ""
        val parts = trimmed.split('/').filter { it.isNotEmpty() }
        for (part in parts) {
            if (part == "." || part == "..") return null
        }
        return parts.joinToString("/")
    }

    /** Returns `true` if [path] is safe to use as a GitHub tree path. */
    fun isSafeRepoPath(path: String): Boolean = normalizeRepoPath(path) != null
}
