package com.painkiller.domain.files

import com.painkiller.domain.model.DiagnosticSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LargeFileDoctorTest {

    @Test
    fun belowThreshold_isSafe() {
        val diagnosis = LargeFileDoctor.diagnose(10_000_000L)

        assertEquals(SizeRiskLevel.NORMAL, diagnosis.riskLevel)
        assertEquals(DiagnosticSeverity.SAFE, diagnosis.severity)
        assertFalse(diagnosis.isBlockedForNormalCommit)
    }

    @Test
    fun above25Mb_isWarning() {
        val diagnosis = LargeFileDoctor.diagnose(25_000_001L)

        assertEquals(SizeRiskLevel.WEB_UPLOAD_UNSUITABLE, diagnosis.riskLevel)
        assertEquals(DiagnosticSeverity.WARNING, diagnosis.severity)
        assertFalse(diagnosis.isBlockedForNormalCommit)
    }

    @Test
    fun above50Mib_isStrongWarning() {
        val diagnosis = LargeFileDoctor.diagnose((50L * 1024L * 1024L) + 1L)

        assertEquals(SizeRiskLevel.LARGE_REPO_RISK, diagnosis.riskLevel)
        assertEquals(DiagnosticSeverity.WARNING, diagnosis.severity)
        assertTrue(diagnosis.deferredRecommendations.contains(DeferredRecommendation.GIT_LFS))
        assertFalse(diagnosis.isBlockedForNormalCommit)
    }

    @Test
    fun above100Mib_isBlocked() {
        val diagnosis = LargeFileDoctor.diagnose((100L * 1024L * 1024L) + 1L)

        assertEquals(SizeRiskLevel.NORMAL_COMMIT_BLOCKED, diagnosis.riskLevel)
        assertEquals(DiagnosticSeverity.BLOCKED, diagnosis.severity)
        assertTrue(diagnosis.deferredRecommendations.contains(DeferredRecommendation.GIT_LFS))
        assertTrue(diagnosis.deferredRecommendations.contains(DeferredRecommendation.RELEASE_ASSETS))
        assertTrue(diagnosis.isBlockedForNormalCommit)
    }

    @Test
    fun unknownSize_isConservativelySafeButNotBlocked() {
        val diagnosis = LargeFileDoctor.diagnose(null)

        assertEquals(SizeRiskLevel.NORMAL, diagnosis.riskLevel)
        assertEquals(DiagnosticSeverity.SAFE, diagnosis.severity)
        assertFalse(diagnosis.isBlockedForNormalCommit)
    }
}
