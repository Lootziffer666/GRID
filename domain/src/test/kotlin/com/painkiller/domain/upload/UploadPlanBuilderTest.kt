package com.painkiller.domain.upload

import com.painkiller.domain.files.FilePlan
import com.painkiller.domain.files.IgnoreRule
import com.painkiller.domain.files.LargeFileDoctor
import com.painkiller.domain.files.PlannedFile
import com.painkiller.domain.files.SourceKind
import com.painkiller.domain.model.DiagnosticSeverity
import com.painkiller.domain.target.BranchTarget
import com.painkiller.domain.target.RepoTarget
import com.painkiller.domain.target.TargetPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadPlanBuilderTest {

    private val target = RepoTarget(
        owner = "octocat",
        repo = "hello-world",
        branch = BranchTarget(name = "main"),
        targetPath = TargetPath(normalized = "uploads"),
    )

    private fun safeFile(name: String) = PlannedFile(
        sourceId = name,
        sourceDisplayName = name,
        sourceRelativePath = name,
        repoPath = "uploads/$name",
        sizeBytes = 1_000L,
        mimeType = "text/plain",
        sizeDiagnosis = LargeFileDoctor.diagnose(1_000L),
    )

    private fun warningFile(name: String) = PlannedFile(
        sourceId = name,
        sourceDisplayName = name,
        sourceRelativePath = name,
        repoPath = "uploads/$name",
        sizeBytes = 30_000_000L, // > 25 MB decimal
        mimeType = "application/octet-stream",
        sizeDiagnosis = LargeFileDoctor.diagnose(30_000_000L),
    )

    private fun blockedFile(name: String) = PlannedFile(
        sourceId = name,
        sourceDisplayName = name,
        sourceRelativePath = name,
        repoPath = "uploads/$name",
        sizeBytes = 110L * 1024L * 1024L, // > 100 MiB
        mimeType = "application/octet-stream",
        sizeDiagnosis = LargeFileDoctor.diagnose(110L * 1024L * 1024L),
    )

    private fun ignoredFile(name: String) = PlannedFile(
        sourceId = name,
        sourceDisplayName = name,
        sourceRelativePath = name,
        repoPath = "uploads/$name",
        sizeBytes = 500L,
        mimeType = "text/plain",
        sizeDiagnosis = LargeFileDoctor.diagnose(500L),
        ignoredByRule = IgnoreRule(name = "DS_Store filter", folderName = ".DS_Store"),
    )

    private fun planWith(
        included: List<PlannedFile> = emptyList(),
        ignored: List<PlannedFile> = emptyList(),
        targetPath: String = "uploads",
    ) = FilePlan(
        sourceKind = SourceKind.MULTIPLE_FILES,
        targetPath = targetPath,
        includedFiles = included,
        ignoredFiles = ignored,
        issues = emptyList(),
        isBlockedForNormalCommit = included.any { it.sizeDiagnosis.isBlockedForNormalCommit },
    )

    // ─── plan creation ────────────────────────────────────────────────────────

    @Test
    fun singleSafeFile_plan_hasOneEntryInSafeGroup() {
        val plan = UploadPlanBuilder.build(planWith(included = listOf(safeFile("a.md"))), target)

        assertEquals(1, plan.safeEntries.size)
        assertTrue(plan.warningEntries.isEmpty())
        assertTrue(plan.blockedEntries.isEmpty())
        assertEquals("uploads/a.md", plan.safeEntries[0].repoPath)
        assertEquals(DiagnosticSeverity.SAFE, plan.safeEntries[0].severity)
    }

    @Test
    fun warningFile_plan_hasOneEntryInWarningGroup() {
        val plan = UploadPlanBuilder.build(planWith(included = listOf(warningFile("big.bin"))), target)

        assertEquals(1, plan.warningEntries.size)
        assertTrue(plan.safeEntries.isEmpty())
        assertTrue(plan.blockedEntries.isEmpty())
        assertEquals(DiagnosticSeverity.WARNING, plan.warningEntries[0].severity)
    }

    @Test
    fun blockedFile_plan_isBlockedForCommit() {
        val plan = UploadPlanBuilder.build(planWith(included = listOf(blockedFile("huge.iso"))), target)

        assertEquals(1, plan.blockedEntries.size)
        assertTrue(plan.isBlockedForCommit)
        assertFalse(plan.willCreateOneCommit)
        assertEquals(DiagnosticSeverity.BLOCKED, plan.blockedEntries[0].severity)
    }

    @Test
    fun ignoredFile_plan_hasOneEntryInIgnoredGroup_notBlocking() {
        val plan = UploadPlanBuilder.build(
            planWith(ignored = listOf(ignoredFile(".DS_Store"))),
            target,
        )

        assertEquals(1, plan.ignoredEntries.size)
        assertTrue(plan.ignoredEntries[0].isIgnored)
        assertTrue(plan.safeEntries.isEmpty())
        assertFalse(plan.isBlockedForCommit)
    }

    @Test
    fun mixedFiles_plan_groupsCorrectly() {
        val plan = UploadPlanBuilder.build(
            planWith(
                included = listOf(safeFile("a.md"), warningFile("b.bin"), blockedFile("c.iso")),
                ignored = listOf(ignoredFile(".DS_Store")),
            ),
            target,
        )

        assertEquals(1, plan.safeEntries.size)
        assertEquals(1, plan.warningEntries.size)
        assertEquals(1, plan.blockedEntries.size)
        assertEquals(1, plan.ignoredEntries.size)
        assertTrue(plan.isBlockedForCommit)
        assertFalse(plan.willCreateOneCommit)
    }

    @Test
    fun noBlockedFiles_willCreateOneCommit_isTrue() {
        val plan = UploadPlanBuilder.build(
            planWith(included = listOf(safeFile("a.md"), warningFile("b.bin"))),
            target,
        )

        assertFalse(plan.isBlockedForCommit)
        assertTrue(plan.willCreateOneCommit)
    }

    @Test
    fun emptyPlan_willCreateOneCommit_isFalse_andNotBlocked() {
        val plan = UploadPlanBuilder.build(planWith(), target)

        assertFalse(plan.isBlockedForCommit)
        assertFalse(plan.willCreateOneCommit)
    }

    @Test
    fun target_isPreservedInPlan() {
        val plan = UploadPlanBuilder.build(planWith(included = listOf(safeFile("a.md"))), target)
        assertEquals(target, plan.target)
    }

    // ─── commit message suggestion ────────────────────────────────────────────

    @Test
    fun commitMessage_noEntries_suggestsGitkeep() {
        val plan = UploadPlanBuilder.build(planWith(), target)
        assertEquals("Add .gitkeep", plan.suggestedCommitMessage)
    }

    @Test
    fun commitMessage_singleFile_addsFileName() {
        val plan = UploadPlanBuilder.build(planWith(included = listOf(safeFile("notes.md"))), target)
        assertEquals("Add notes.md", plan.suggestedCommitMessage)
    }

    @Test
    fun commitMessage_twoFiles_listsBothNames() {
        val plan = UploadPlanBuilder.build(
            planWith(included = listOf(safeFile("a.md"), safeFile("b.md"))),
            target,
        )
        assertEquals("Add a.md, b.md", plan.suggestedCommitMessage)
    }

    @Test
    fun commitMessage_fourFiles_listsAllNames() {
        val plan = UploadPlanBuilder.build(
            planWith(included = listOf(safeFile("a.md"), safeFile("b.md"), safeFile("c.md"), safeFile("d.md"))),
            target,
        )
        assertEquals("Add a.md, b.md, c.md, d.md", plan.suggestedCommitMessage)
    }

    @Test
    fun commitMessage_fiveFiles_usesCountAndTargetPath() {
        val plan = UploadPlanBuilder.build(
            planWith(
                included = listOf(
                    safeFile("a.md"), safeFile("b.md"), safeFile("c.md"),
                    safeFile("d.md"), safeFile("e.md"),
                ),
                targetPath = "uploads",
            ),
            target,
        )
        assertEquals("Add 5 files to uploads", plan.suggestedCommitMessage)
    }

    @Test
    fun commitMessage_fiveFiles_rootTarget_usesCountOnly() {
        val rootTarget = target.copy(targetPath = TargetPath(normalized = ""))
        val plan = UploadPlanBuilder.build(
            planWith(
                included = listOf(
                    safeFile("a.md"), safeFile("b.md"), safeFile("c.md"),
                    safeFile("d.md"), safeFile("e.md"),
                ),
                targetPath = "",
            ),
            rootTarget,
        )
        assertEquals("Add 5 files", plan.suggestedCommitMessage)
    }

    @Test
    fun commitMessage_includesWarningFiles_inCount() {
        val plan = UploadPlanBuilder.build(
            planWith(
                included = listOf(
                    safeFile("a.md"), safeFile("b.md"), safeFile("c.md"),
                    warningFile("d.bin"), warningFile("e.bin"),
                ),
                targetPath = "docs",
            ),
            target.copy(targetPath = TargetPath(normalized = "docs")),
        )
        assertEquals("Add 5 files to docs", plan.suggestedCommitMessage)
    }

    @Test
    fun commitMessage_blockedFilesNotCommittable_notCountedInSuggestion() {
        // Only 1 safe file + 1 blocked file. Suggestion is based on safe only.
        val plan = UploadPlanBuilder.build(
            planWith(included = listOf(safeFile("readme.md"), blockedFile("huge.iso"))),
            target,
        )
        // Only "readme.md" is committable in the suggestion.
        assertEquals("Add readme.md", plan.suggestedCommitMessage)
    }
}
