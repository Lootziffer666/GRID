package com.painkiller.domain.files

/**
 * One candidate file in the generated FilePlan.
 */
data class PlannedFile(
    val sourceId: String,
    val sourceDisplayName: String,
    val sourceRelativePath: String,
    val repoPath: String,
    val sizeBytes: Long?,
    val mimeType: String?,
    val sizeDiagnosis: SizeDiagnosis,
    val ignoredByRule: IgnoreRule? = null
)
