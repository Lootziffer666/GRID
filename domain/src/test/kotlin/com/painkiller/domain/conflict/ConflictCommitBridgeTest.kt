package com.painkiller.domain.conflict

import com.painkiller.domain.files.SourceKind
import com.painkiller.domain.target.BranchTarget
import com.painkiller.domain.target.RepoTarget
import com.painkiller.domain.target.TargetPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConflictCommitBridgeTest {

    private val target = RepoTarget("o", "r", BranchTarget("main"), TargetPath("docs"))

    @Test
    fun plan_fromWrittenFiles_excludesZipAndMarkerFiles() {
        val plan = ConflictCommitPlanner.buildPlan(
            target = target,
            writtenFiles = listOf(
                ResolvedCommitCandidate("a.txt", "a.txt", "id-a", "YQ==", "ok", SourceKind.SINGLE_FILE),
                ResolvedCommitCandidate("b.txt", "b.txt", "id-b", "Yg==", "<<<<<<< HEAD", SourceKind.SINGLE_FILE),
                ResolvedCommitCandidate("c.txt", "c.txt", "id-c", "Yw==", "ok", SourceKind.ZIP),
            ),
            blockedByWrite = emptyList(),
            failedByWrite = emptyList(),
        )

        assertEquals(1, plan.candidates.size)
        assertEquals(2, plan.blockedFiles.size)
        assertFalse(plan.canCommit)
    }

    @Test
    fun plan_blocksWhenTargetMissing() {
        val plan = ConflictCommitPlanner.buildPlan(
            target = null,
            writtenFiles = listOf(
                ResolvedCommitCandidate("a.txt", "a.txt", "id-a", "YQ==", "ok", SourceKind.SINGLE_FILE),
            ),
            blockedByWrite = emptyList(),
            failedByWrite = emptyList(),
        )
        assertFalse(plan.canCommit)
        assertTrue(plan.summary.contains("missing"))
    }

    @Test
    fun suggestion_usesFileCount() {
        val single = ConflictCommitPlanner.buildPlan(target, listOf(
            ResolvedCommitCandidate("a.txt", "a.txt", "id-a", "YQ==", "ok", SourceKind.SINGLE_FILE),
        ), emptyList(), emptyList())
        val multi = ConflictCommitPlanner.buildPlan(target, listOf(
            ResolvedCommitCandidate("a.txt", "a.txt", "id-a", "YQ==", "ok", SourceKind.SINGLE_FILE),
            ResolvedCommitCandidate("b.txt", "b.txt", "id-b", "Yg==", "ok", SourceKind.SINGLE_FILE),
        ), emptyList(), emptyList())

        assertEquals("Resolve Codex conflicts", single.commitMessageSuggestion)
        assertEquals("Resolve conflicts in 2 files", multi.commitMessageSuggestion)
    }
}
