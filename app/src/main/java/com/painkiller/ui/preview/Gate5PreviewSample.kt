package com.painkiller.ui.preview

import com.painkiller.domain.files.LargeFileDoctor
import com.painkiller.domain.model.DiagnosticSeverity
import com.painkiller.domain.target.BranchTarget
import com.painkiller.domain.target.RepoTarget
import com.painkiller.domain.target.TargetPath
import com.painkiller.domain.upload.UploadPlan
import com.painkiller.domain.upload.UploadPlanEntry

/**
 * Gate 5 — deterministic sample [UploadPlan] for Compose @Preview use only.
 *
 * Hard-codes representative entries across all four severity groups so the
 * preview renders the full UI without any SAF, network, or GitHub call.
 * Never used outside @Preview functions.
 */
internal object Gate5PreviewSample {

    val plan: UploadPlan = UploadPlan(
        target = RepoTarget(
            owner = "octocat",
            repo = "my-notes",
            branch = BranchTarget(name = "main"),
            targetPath = TargetPath(normalized = "uploads/docs"),
        ),
        safeEntries = listOf(
            entry("uploads/docs/readme.md", "readme.md", 4_200L, DiagnosticSeverity.SAFE),
            entry("uploads/docs/notes.md", "notes.md", 8_500L, DiagnosticSeverity.SAFE),
        ),
        warningEntries = listOf(
            entry("uploads/docs/presentation.pdf", "presentation.pdf", 30_000_000L, DiagnosticSeverity.WARNING),
        ),
        blockedEntries = listOf(
            entry("uploads/docs/backup.zip", "backup.zip", 120L * 1024L * 1024L, DiagnosticSeverity.BLOCKED),
        ),
        deferredEntries = emptyList(),
        ignoredEntries = listOf(
            entry("uploads/docs/.DS_Store", ".DS_Store", 6_148L, DiagnosticSeverity.SAFE, isIgnored = true),
        ),
        suggestedCommitMessage = "Add readme.md, notes.md, presentation.pdf",
    )

    private fun entry(
        repoPath: String,
        displayName: String,
        sizeBytes: Long,
        severity: DiagnosticSeverity,
        isIgnored: Boolean = false,
    ) = UploadPlanEntry(
        repoPath = repoPath,
        displayName = displayName,
        sizeBytes = sizeBytes,
        sizeDiagnosis = LargeFileDoctor.diagnose(sizeBytes),
        severity = severity,
        isIgnored = isIgnored,
    )
}
