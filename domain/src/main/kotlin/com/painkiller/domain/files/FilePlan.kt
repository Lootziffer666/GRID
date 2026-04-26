package com.painkiller.domain.files

/**
 * Deterministic planning result for Gate 1 local intake.
 */
data class FilePlan(
    val sourceKind: SourceKind,
    val targetPath: String,
    val includedFiles: List<PlannedFile>,
    val ignoredFiles: List<PlannedFile>,
    val issues: List<FilePlanIssue>
)

sealed interface FilePlanBuildResult {
    data class Success(val plan: FilePlan) : FilePlanBuildResult
    data class ValidationError(val issues: List<FilePlanIssue>) : FilePlanBuildResult
}
