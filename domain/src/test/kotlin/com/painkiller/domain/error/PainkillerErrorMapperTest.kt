package com.painkiller.domain.error

import com.painkiller.domain.github.MultiFileCommitResult
import com.painkiller.domain.github.SingleFileCommitResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PainkillerErrorMapperTest {

    // ─── Gate 6: SingleFileCommitResult.Failure mapping ──────────────────────

    @Test
    fun gate6_authError_titleIsSignInRequired() {
        val error = PainkillerErrorMapper.map(SingleFileCommitResult.AuthError("auth failed"))
        assertEquals("Sign in required", error.title)
    }

    @Test
    fun gate6_authError_hintIsSignIn_notRetryable() {
        val error = PainkillerErrorMapper.map(SingleFileCommitResult.AuthError("auth failed"))
        assertEquals(RecoveryHint.SIGN_IN, error.recoveryHint)
        assertEquals(RetrySafety.NOT_RETRYABLE, error.retrySafety)
    }

    @Test
    fun gate6_permissionError_titleIsPermissionDenied() {
        val error = PainkillerErrorMapper.map(SingleFileCommitResult.PermissionError("denied"))
        assertEquals("Permission denied", error.title)
    }

    @Test
    fun gate6_permissionError_hintIsCheckPermissions_notRetryable() {
        val error = PainkillerErrorMapper.map(SingleFileCommitResult.PermissionError("denied"))
        assertEquals(RecoveryHint.CHECK_PERMISSIONS, error.recoveryHint)
        assertEquals(RetrySafety.NOT_RETRYABLE, error.retrySafety)
    }

    @Test
    fun gate6_permissionError_detailDoesNotImplyBadCredentials() {
        val error = PainkillerErrorMapper.map(SingleFileCommitResult.PermissionError("403"))
        // Must say "access" or "permissions" — must NOT say "bad credentials" or "wrong token"
        assertFalse(
            "Detail must not blame credentials",
            error.detail.lowercase().contains("bad credential") ||
                error.detail.lowercase().contains("wrong token") ||
                error.detail.lowercase().contains("invalid token"),
        )
    }

    @Test
    fun gate6_branchNotFound_titleIsBranchNotFound_notRetryable() {
        val error = PainkillerErrorMapper.map(SingleFileCommitResult.BranchNotFound("404"))
        assertEquals("Branch not found", error.title)
        assertEquals(RetrySafety.NOT_RETRYABLE, error.retrySafety)
    }

    @Test
    fun gate6_protectedBranch_hintIsChooseDifferentBranch() {
        val error = PainkillerErrorMapper.map(SingleFileCommitResult.ProtectedBranch("protected"))
        assertEquals("Branch is protected", error.title)
        assertEquals(RecoveryHint.CHOOSE_DIFFERENT_BRANCH, error.recoveryHint)
        assertEquals(RetrySafety.NOT_RETRYABLE, error.retrySafety)
    }

    @Test
    fun gate6_shaMismatch_hintIsRefreshPlan_requiresPlanRefresh() {
        val error = PainkillerErrorMapper.map(SingleFileCommitResult.ShaMismatch("sha changed"))
        assertEquals("Branch changed", error.title)
        assertEquals(RecoveryHint.REFRESH_PLAN, error.recoveryHint)
        assertEquals(RetrySafety.REQUIRES_PLAN_REFRESH, error.retrySafety)
    }

    @Test
    fun gate6_shaMismatch_detailMentionsBranchChanged() {
        val error = PainkillerErrorMapper.map(SingleFileCommitResult.ShaMismatch("sha mismatch"))
        assertTrue(
            "SHA mismatch detail should mention branch changed",
            error.detail.lowercase().contains("changed") || error.detail.lowercase().contains("refresh"),
        )
    }

    @Test
    fun gate6_networkError_retrySafetyIsSafeToRetry() {
        val error = PainkillerErrorMapper.map(SingleFileCommitResult.NetworkError("timeout"))
        assertEquals(RetrySafety.SAFE_TO_RETRY, error.retrySafety)
        assertEquals(RecoveryHint.CHECK_NETWORK, error.recoveryHint)
    }

    @Test
    fun gate6_invalidInput_hintIsFixFilePaths_notRetryable() {
        val error = PainkillerErrorMapper.map(SingleFileCommitResult.InvalidInput("bad path"))
        assertEquals("Invalid file path", error.title)
        assertEquals(RecoveryHint.FIX_FILE_PATHS, error.recoveryHint)
        assertEquals(RetrySafety.NOT_RETRYABLE, error.retrySafety)
    }

    @Test
    fun gate6_unknownError_failsClosed_noAutoRetry() {
        val error = PainkillerErrorMapper.map(SingleFileCommitResult.UnknownError("unexpected"))
        assertEquals("Unexpected error", error.title)
        assertEquals(RetrySafety.NOT_RETRYABLE, error.retrySafety)
    }

    @Test
    fun gate6_unknownError_detailStatesNothingWasWritten() {
        val error = PainkillerErrorMapper.map(SingleFileCommitResult.UnknownError("unexpected"))
        assertTrue(
            "Unknown error detail should state nothing was written",
            error.detail.lowercase().contains("nothing was written") ||
                error.detail.lowercase().contains("nothing written"),
        )
    }

    // ─── Gate 7: MultiFileCommitResult.Failure mapping ───────────────────────

    @Test
    fun gate7_authError_sameMappingAsGate6() {
        val g6 = PainkillerErrorMapper.map(SingleFileCommitResult.AuthError(""))
        val g7 = PainkillerErrorMapper.map(MultiFileCommitResult.AuthError(""))
        assertEquals(g6.title, g7.title)
        assertEquals(g6.recoveryHint, g7.recoveryHint)
        assertEquals(g6.retrySafety, g7.retrySafety)
    }

    @Test
    fun gate7_invalidInput_unsafePath_hintIsFixFilePaths() {
        val error = PainkillerErrorMapper.map(
            MultiFileCommitResult.InvalidInput("path ../../etc/passwd is unsafe")
        )
        assertEquals(RecoveryHint.FIX_FILE_PATHS, error.recoveryHint)
        assertEquals(RetrySafety.NOT_RETRYABLE, error.retrySafety)
    }

    @Test
    fun gate7_protectedBranch_hintIsChooseDifferentBranch() {
        val error = PainkillerErrorMapper.map(MultiFileCommitResult.ProtectedBranch("protected"))
        assertEquals(RecoveryHint.CHOOSE_DIFFERENT_BRANCH, error.recoveryHint)
    }

    @Test
    fun gate7_shaMismatch_requiresPlanRefresh() {
        val error = PainkillerErrorMapper.map(MultiFileCommitResult.ShaMismatch("sha mismatch"))
        assertEquals(RetrySafety.REQUIRES_PLAN_REFRESH, error.retrySafety)
    }

    @Test
    fun gate7_networkError_safeToRetry() {
        val error = PainkillerErrorMapper.map(MultiFileCommitResult.NetworkError("offline"))
        assertEquals(RetrySafety.SAFE_TO_RETRY, error.retrySafety)
    }

    @Test
    fun gate7_unknownError_failsClosed() {
        val error = PainkillerErrorMapper.map(MultiFileCommitResult.UnknownError("?"))
        assertEquals(RetrySafety.NOT_RETRYABLE, error.retrySafety)
        assertEquals(RecoveryHint.NO_ACTION, error.recoveryHint)
    }

    @Test
    fun gate7_permissionError_notRetryable() {
        val error = PainkillerErrorMapper.map(MultiFileCommitResult.PermissionError("403"))
        assertEquals(RetrySafety.NOT_RETRYABLE, error.retrySafety)
        assertEquals(RecoveryHint.CHECK_PERMISSIONS, error.recoveryHint)
    }

    // ─── Gate 3: auth / repo / branch listing mapping ────────────────────────

    @Test
    fun gate3_authExchange_titleIsSignInFailed() {
        val error = PainkillerErrorMapper.mapAuthExchange("GitHub authentication failed.")
        assertEquals("Sign in failed", error.title)
        assertEquals(RecoveryHint.SIGN_IN, error.recoveryHint)
        assertEquals(RetrySafety.NOT_RETRYABLE, error.retrySafety)
    }

    @Test
    fun gate3_repoListing_unauthenticated_mapsToAuthRequired() {
        val error = PainkillerErrorMapper.mapRepoListing("Not authenticated. Sign in to GitHub first.")
        assertEquals(RecoveryHint.SIGN_IN, error.recoveryHint)
    }

    @Test
    fun gate3_repoListing_networkFailure_safeToRetry() {
        val error = PainkillerErrorMapper.mapRepoListing("Failed to load repositories.")
        assertEquals(RetrySafety.SAFE_TO_RETRY, error.retrySafety)
        assertEquals("Could not load repositories", error.title)
    }

    @Test
    fun gate3_branchListing_networkFailure_safeToRetry() {
        val error = PainkillerErrorMapper.mapBranchListing("Failed to load branches.")
        assertEquals(RetrySafety.SAFE_TO_RETRY, error.retrySafety)
        assertEquals("Could not load branches", error.title)
    }

    @Test
    fun gate3_branchListing_unauthenticated_mapsToSignIn() {
        val error = PainkillerErrorMapper.mapBranchListing("Not authenticated. Sign in to GitHub first.")
        assertEquals(RecoveryHint.SIGN_IN, error.recoveryHint)
    }

    // ─── Blocked-file (pre-commit gate) mapping ───────────────────────────────

    @Test
    fun blockedForCommit_hintIsRemoveLargeFiles_notRetryable() {
        val error = PainkillerErrorMapper.mapBlockedForCommit()
        assertEquals("Upload blocked", error.title)
        assertEquals(RecoveryHint.REMOVE_LARGE_FILES, error.recoveryHint)
        assertEquals(RetrySafety.NOT_RETRYABLE, error.retrySafety)
    }

    // ─── Token sanitization / redaction ──────────────────────────────────────

    @Test
    fun tokenRedaction_ghpToken_isRedacted() {
        val token = "ghp_ABCDefgh12345678901234567890abcd1234"
        val result = PainkillerErrorMapper.sanitize("token=$token")
        assertFalse("Token must be redacted", result.contains(token))
        assertTrue("Redaction marker present", result.contains("[token redacted]"))
    }

    @Test
    fun tokenRedaction_ghsToken_isRedacted() {
        val token = "ghs_ABCDefgh12345678901234567890abcd1234"
        val result = PainkillerErrorMapper.sanitize("Authorization: $token")
        assertFalse("GHS token must be redacted", result.contains(token))
    }

    @Test
    fun tokenRedaction_githubPatToken_isRedacted() {
        val token = "github_pat_ABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890"
        val result = PainkillerErrorMapper.sanitize("token=$token")
        assertFalse("Fine-grained PAT must be redacted", result.contains(token))
    }

    @Test
    fun tokenRedaction_bearerHeader_isRedacted() {
        val result = PainkillerErrorMapper.sanitize("Authorization: Bearer ghp_abc123def456ghi789jkl012mno345pqr678stu")
        assertFalse("Bearer token must be redacted", result.contains("ghp_"))
        assertTrue("Redaction marker present", result.contains("[token redacted]"))
    }

    @Test
    fun tokenRedaction_cleanMessage_isUnchanged() {
        val clean = "The branch changed. Refresh the plan and try again."
        assertEquals(clean, PainkillerErrorMapper.sanitize(clean))
    }

    @Test
    fun tokenRedaction_partialTokenPattern_withoutPrefix_isNotRedacted() {
        // A random alphanumeric string that does NOT start with a known prefix
        // must not be incorrectly redacted.
        val safe = "commit-sha-abc123def456"
        val result = PainkillerErrorMapper.sanitize(safe)
        assertEquals(safe, result)
    }

    // ─── Retry / destructive write safety ────────────────────────────────────

    @Test
    fun retryClassification_networkError_isRetryable_authError_isNot() {
        val network = PainkillerErrorMapper.map(SingleFileCommitResult.NetworkError("offline"))
        val auth = PainkillerErrorMapper.map(SingleFileCommitResult.AuthError("401"))
        assertEquals(RetrySafety.SAFE_TO_RETRY, network.retrySafety)
        assertEquals(RetrySafety.NOT_RETRYABLE, auth.retrySafety)
    }

    @Test
    fun noDestructiveAutoWrite_shaMismatch_requiresRefreshBeforeWrite() {
        val error = PainkillerErrorMapper.map(SingleFileCommitResult.ShaMismatch("stale"))
        // SHA mismatch must NOT be SAFE_TO_RETRY — it must force a plan refresh
        // before the user is allowed to write again.
        assertTrue(
            "SHA mismatch must require plan refresh, not be immediately retryable",
            error.retrySafety == RetrySafety.REQUIRES_PLAN_REFRESH,
        )
    }

    @Test
    fun allMappedErrors_haveNonBlankTitleAndDetail() {
        val failures6: List<SingleFileCommitResult.Failure> = listOf(
            SingleFileCommitResult.InvalidInput("x"),
            SingleFileCommitResult.AuthError("x"),
            SingleFileCommitResult.PermissionError("x"),
            SingleFileCommitResult.BranchNotFound("x"),
            SingleFileCommitResult.ProtectedBranch("x"),
            SingleFileCommitResult.ShaMismatch("x"),
            SingleFileCommitResult.NetworkError("x"),
            SingleFileCommitResult.UnknownError("x"),
        )
        val failures7: List<MultiFileCommitResult.Failure> = listOf(
            MultiFileCommitResult.InvalidInput("x"),
            MultiFileCommitResult.AuthError("x"),
            MultiFileCommitResult.PermissionError("x"),
            MultiFileCommitResult.BranchNotFound("x"),
            MultiFileCommitResult.ProtectedBranch("x"),
            MultiFileCommitResult.ShaMismatch("x"),
            MultiFileCommitResult.NetworkError("x"),
            MultiFileCommitResult.UnknownError("x"),
        )
        failures6.forEach { f ->
            val e = PainkillerErrorMapper.map(f)
            assertTrue("title blank for ${f::class.simpleName}", e.title.isNotBlank())
            assertTrue("detail blank for ${f::class.simpleName}", e.detail.isNotBlank())
        }
        failures7.forEach { f ->
            val e = PainkillerErrorMapper.map(f)
            assertTrue("title blank for ${f::class.simpleName}", e.title.isNotBlank())
            assertTrue("detail blank for ${f::class.simpleName}", e.detail.isNotBlank())
        }
    }

    @Test
    fun allMappedErrors_detailDoesNotContainRawToken() {
        val tokenInMessage = "ghp_FAKEFAKEFAKEFAKEFAKEFAKEFAKEFAKEFAKE"
        // If a future failure message accidentally includes a token, the mapper
        // would still return a fixed, pre-written detail string (not the raw message),
        // so no token can leak. Verify the fixed strings contain no token pattern.
        val error = PainkillerErrorMapper.map(
            SingleFileCommitResult.UnknownError(tokenInMessage)
        )
        assertFalse(
            "HumanReadableError.detail must not contain the raw token",
            error.detail.contains(tokenInMessage),
        )
        assertFalse(
            "HumanReadableError.title must not contain the raw token",
            error.title.contains(tokenInMessage),
        )
    }
}
