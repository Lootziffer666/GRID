package com.painkiller.app.data.github

/**
 * Tiny, dependency-free path validation helpers used by the upload
 * flow. Kept here (in `data.github`) for Gate 0 because the GitHub
 * tree path is the closest concrete consumer of these checks. Later
 * gates may move this into a dedicated `domain.path` package.
 *
 * These rules are also documented in `instructions.md` § "Gate 1" and
 * § "Gate 4":
 * - use `/` separators
 * - collapse duplicate slashes
 * - reject `..` traversal
 * - reject absolute local paths (a leading `/` or a Windows drive)
 * - reject empty path segments outside the leading slash
 */
object PathValidation {

    /**
     * Normalize a target repo path to GitHub's expected form.
     *
     * Returns null when the path is unsafe (path traversal, absolute
     * local path, etc). Callers must treat null as a hard rejection
     * — never silently retry with a "fixed" guess.
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

    /**
     * Returns true if [path] is safe to use as a GitHub tree path.
     */
    fun isSafeRepoPath(path: String): Boolean = normalizeRepoPath(path) != null
}
