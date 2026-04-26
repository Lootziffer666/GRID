package com.painkiller.domain.upload

/**
 * Gate 5 — generates a short, human-readable commit message suggestion.
 *
 * The suggestion is an editable starting point; the commit screen must let
 * the user override it. Logic:
 *
 *   0 committable files  → "Add .gitkeep"          (empty folder → Gate 7 injects .gitkeep)
 *   1 file               → "Add <filename>"
 *   2–4 files            → "Add <a>, <b>, <c>"
 *   5+ files, with path  → "Add 7 files to uploads/docs"
 *   5+ files, root       → "Add 7 files"
 *
 * Only [safeEntries] and [warningEntries] count as committable. Blocked and
 * deferred entries are excluded because they prevent or defer the operation.
 */
object CommitMessageSuggester {

    fun suggest(
        safeEntries: List<UploadPlanEntry>,
        warningEntries: List<UploadPlanEntry>,
        targetPath: String,
    ): String {
        val committable = safeEntries + warningEntries
        if (committable.isEmpty()) return "Add .gitkeep"

        if (committable.size == 1) {
            val name = committable[0].repoPath.substringAfterLast('/')
            return "Add $name"
        }

        if (committable.size <= 4) {
            val names = committable.map { it.repoPath.substringAfterLast('/') }
            return "Add ${names.joinToString(", ")}"
        }

        val loc = if (targetPath.isBlank()) "" else " to $targetPath"
        return "Add ${committable.size} files$loc"
    }
}
