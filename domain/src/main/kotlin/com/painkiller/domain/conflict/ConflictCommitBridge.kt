package com.painkiller.domain.conflict

import com.painkiller.domain.files.SourceKind
import com.painkiller.domain.target.RepoTarget

data class ResolvedCommitCandidate(
    val repoPath: String,
    val sourcePath: String,
    val sourceId: String,
    val contentBase64: String,
    val textContent: String,
    val sourceKind: SourceKind,
)

data class ConflictCommitBlockedFile(
    val path: String,
    val reason: String,
)

data class ConflictCommitPlan(
    val target: RepoTarget?,
    val candidates: List<ResolvedCommitCandidate>,
    val blockedFiles: List<ConflictCommitBlockedFile>,
    val commitMessageSuggestion: String,
    val requiresConfirmation: Boolean,
    val summary: String,
) {
    val canCommit: Boolean get() = target != null && candidates.isNotEmpty() && blockedFiles.isEmpty()
}

data class ConflictCommitResult(
    val committedFiles: List<String>,
    val blockedFiles: List<ConflictCommitBlockedFile>,
    val failedReason: String?,
    val commitSha: String?,
    val commitUrl: String?,
    val didCreateCommit: Boolean,
)

object ConflictCommitPlanner {
    private val markers = listOf("<<<<<<<", "=======", ">>>>>>>")

    fun buildPlan(
        target: RepoTarget?,
        writtenFiles: List<ResolvedCommitCandidate>,
        blockedByWrite: List<String>,
        failedByWrite: List<String>,
    ): ConflictCommitPlan {
        val blocked = mutableListOf<ConflictCommitBlockedFile>()
        blockedByWrite.forEach { blocked += ConflictCommitBlockedFile(it, "Blocked during resolved-file write plan.") }
        failedByWrite.forEach { blocked += ConflictCommitBlockedFile(it, "Failed during resolved-file write-back.") }

        val candidates = mutableListOf<ResolvedCommitCandidate>()
        writtenFiles.forEach { c ->
            when {
                c.sourceKind == SourceKind.ZIP -> blocked += ConflictCommitBlockedFile(c.sourcePath, "Blocked for safety: ZIP-entry sources cannot be committed in this flow.")
                containsMarkers(c.textContent) -> blocked += ConflictCommitBlockedFile(c.sourcePath, "Conflict markers still found.")
                else -> candidates += c
            }
        }

        val suggestion = if (candidates.size <= 1) "Resolve Codex conflicts" else "Resolve conflicts in ${candidates.size} files"
        val summary = when {
            target == null -> "Target repo/branch is missing. Painkiller did not create a commit."
            candidates.isEmpty() -> "No resolved files are eligible for commit."
            blocked.isNotEmpty() -> "Blocked for safety: ${blocked.size} file(s) need review before commit."
            else -> "Review what will be committed. No push will happen automatically."
        }
        return ConflictCommitPlan(target, candidates, blocked, suggestion, true, summary)
    }

    private fun containsMarkers(text: String): Boolean = markers.any { text.contains(it) }
}
