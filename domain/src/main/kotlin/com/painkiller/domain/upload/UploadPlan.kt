package com.painkiller.domain.upload

import com.painkiller.domain.target.RepoTarget

/**
 * Gate 5 — full diagnosis and preview for a planned upload.
 *
 * Built by [UploadPlanBuilder] from a [com.painkiller.domain.files.FilePlan]
 * and a [RepoTarget]. Entries are pre-grouped by severity so the UI can render
 * each section directly without re-sorting or re-filtering.
 *
 * [isBlockedForCommit] is true when [blockedEntries] is non-empty. The UI must
 * disable the Confirm action in this state — Gate 6 / 7 orchestrators enforce
 * the same invariant, but the preview layer is the first user-facing gate.
 *
 * [suggestedCommitMessage] is an editable starting point. The commit screen
 * should allow the user to override it before confirming.
 */
data class UploadPlan(
    val target: RepoTarget,
    val safeEntries: List<UploadPlanEntry>,
    val warningEntries: List<UploadPlanEntry>,
    val blockedEntries: List<UploadPlanEntry>,
    val deferredEntries: List<UploadPlanEntry>,
    val ignoredEntries: List<UploadPlanEntry>,
    val suggestedCommitMessage: String,
) {
    val isBlockedForCommit: Boolean
        get() = blockedEntries.isNotEmpty()

    val willCreateOneCommit: Boolean
        get() = !isBlockedForCommit &&
            (safeEntries.isNotEmpty() || warningEntries.isNotEmpty() || deferredEntries.isNotEmpty())
}
