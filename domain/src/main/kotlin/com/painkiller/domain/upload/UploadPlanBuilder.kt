package com.painkiller.domain.upload

import com.painkiller.domain.files.FilePlan
import com.painkiller.domain.files.PlannedFile
import com.painkiller.domain.model.DiagnosticSeverity
import com.painkiller.domain.target.RepoTarget

/**
 * Gate 5 — builds an [UploadPlan] from a [FilePlan] and a [RepoTarget].
 *
 * Iterates [FilePlan.includedFiles] and groups each entry by its
 * [com.painkiller.domain.files.SizeDiagnosis.severity]. Ignored files
 * (those from [FilePlan.ignoredFiles]) are placed in [UploadPlan.ignoredEntries]
 * and never counted as committable.
 *
 * The resulting [UploadPlan] carries a [CommitMessageSuggester] output as the
 * initial [UploadPlan.suggestedCommitMessage]. No GitHub write is initiated
 * here — this is a pure, stateless planning step.
 */
object UploadPlanBuilder {

    fun build(filePlan: FilePlan, target: RepoTarget): UploadPlan {
        val safe = mutableListOf<UploadPlanEntry>()
        val warning = mutableListOf<UploadPlanEntry>()
        val blocked = mutableListOf<UploadPlanEntry>()
        val deferred = mutableListOf<UploadPlanEntry>()
        val ignored = mutableListOf<UploadPlanEntry>()

        for (file in filePlan.includedFiles) {
            val entry = file.toEntry(isIgnored = false)
            when (entry.severity) {
                DiagnosticSeverity.SAFE -> safe += entry
                DiagnosticSeverity.WARNING -> warning += entry
                DiagnosticSeverity.BLOCKED -> blocked += entry
                DiagnosticSeverity.DEFERRED -> deferred += entry
            }
        }

        for (file in filePlan.ignoredFiles) {
            ignored += file.toEntry(isIgnored = true)
        }

        val commitMessage = CommitMessageSuggester.suggest(
            safeEntries = safe,
            warningEntries = warning,
            targetPath = filePlan.targetPath,
        )

        return UploadPlan(
            target = target,
            safeEntries = safe,
            warningEntries = warning,
            blockedEntries = blocked,
            deferredEntries = deferred,
            ignoredEntries = ignored,
            suggestedCommitMessage = commitMessage,
        )
    }

    private fun PlannedFile.toEntry(isIgnored: Boolean) = UploadPlanEntry(
        repoPath = repoPath,
        displayName = sourceDisplayName,
        sizeBytes = sizeBytes,
        sizeDiagnosis = sizeDiagnosis,
        severity = sizeDiagnosis.severity,
        isIgnored = isIgnored,
    )
}
