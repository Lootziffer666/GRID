package com.painkiller.domain.upload

import com.painkiller.domain.files.SizeDiagnosis
import com.painkiller.domain.model.DiagnosticSeverity

/**
 * Gate 5 — one file entry in the [UploadPlan] preview.
 *
 * Derived from a [com.painkiller.domain.files.PlannedFile]. The [severity]
 * is read directly from [sizeDiagnosis] so the UI does not need to re-derive
 * it from size thresholds. [isIgnored] is true when the file was excluded by
 * an ignore rule and will not be committed.
 */
data class UploadPlanEntry(
    val repoPath: String,
    val displayName: String,
    val sizeBytes: Long?,
    val sizeDiagnosis: SizeDiagnosis,
    val severity: DiagnosticSeverity,
    val isIgnored: Boolean = false,
)
