package com.painkiller.domain.files

import com.painkiller.domain.model.DiagnosticSeverity

enum class SizeRiskLevel {
    NORMAL,
    WEB_UPLOAD_UNSUITABLE,
    LARGE_REPO_RISK,
    NORMAL_COMMIT_BLOCKED
}

enum class DeferredRecommendation {
    GIT_LFS,
    RELEASE_ASSETS
}

data class SizeDiagnosis(
    val riskLevel: SizeRiskLevel,
    val severity: DiagnosticSeverity,
    val message: String,
    val deferredRecommendations: List<DeferredRecommendation> = emptyList(),
    val isBlockedForNormalCommit: Boolean = false
)

object LargeFileDoctor {

    private const val MB_25_DECIMAL = 25_000_000L
    private const val MIB_50_BINARY = 50L * 1024L * 1024L
    private const val MIB_100_BINARY = 100L * 1024L * 1024L

    fun diagnose(sizeBytes: Long?): SizeDiagnosis {
        if (sizeBytes == null || sizeBytes < 0L) {
            return SizeDiagnosis(
                riskLevel = SizeRiskLevel.NORMAL,
                severity = DiagnosticSeverity.SAFE,
                message = "File size metadata is unavailable; treating as normal for now."
            )
        }

        return when {
            sizeBytes > MIB_100_BINARY -> SizeDiagnosis(
                riskLevel = SizeRiskLevel.NORMAL_COMMIT_BLOCKED,
                severity = DiagnosticSeverity.BLOCKED,
                message = "This does not belong in a normal Git commit. Use GRID Git LFS (single-file) or Release Assets.",
                deferredRecommendations = listOf(
                    DeferredRecommendation.GIT_LFS,
                    DeferredRecommendation.RELEASE_ASSETS
                ),
                isBlockedForNormalCommit = true
            )

            sizeBytes > MIB_50_BINARY -> SizeDiagnosis(
                riskLevel = SizeRiskLevel.LARGE_REPO_RISK,
                severity = DiagnosticSeverity.WARNING,
                message = "GitHub recommends against normal repo files this large because they can make the repo heavy. Consider GRID Git LFS for single-file uploads.",
                deferredRecommendations = listOf(
                    DeferredRecommendation.GIT_LFS,
                    DeferredRecommendation.RELEASE_ASSETS
                )
            )

            sizeBytes > MB_25_DECIMAL -> SizeDiagnosis(
                riskLevel = SizeRiskLevel.WEB_UPLOAD_UNSUITABLE,
                severity = DiagnosticSeverity.WARNING,
                message = "GitHub web upload would be unsuitable or limited; GRID will continue checking hard limits."
            )

            else -> SizeDiagnosis(
                riskLevel = SizeRiskLevel.NORMAL,
                severity = DiagnosticSeverity.SAFE,
                message = "File size is within normal commit limits."
            )
        }
    }
}
