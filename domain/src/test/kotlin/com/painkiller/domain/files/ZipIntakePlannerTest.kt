package com.painkiller.domain.files

import com.painkiller.domain.target.BranchTarget
import com.painkiller.domain.target.RepoTarget
import com.painkiller.domain.target.TargetPath
import com.painkiller.domain.github.MultiFileCommitEntry
import com.painkiller.domain.upload.UploadPlanBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZipIntakePlannerTest {

    @Test
    fun unsafePaths_areBlockedAndReported() {
        val result = ZipIntakePlanner.build(
            listOf(
                entry("safe/readme.md"),
                entry("../escape.txt"),
                entry("/absolute.txt"),
                entry("C:/windows.txt"),
            ),
        )

        assertEquals(listOf("absolute.txt", "safe/readme.md"), result.source.items.mapNotNull { it.relativePath }.sorted())
        assertTrue(result.hasUnsafeEntries)
        assertEquals(2, result.issues.count { it.code == ZipIntakeIssueCode.UNSAFE_PATH })
    }

    @Test
    fun singleRootFolder_isStripped() {
        val result = ZipIntakePlanner.build(
            listOf(
                entry("archive/src/A.kt"),
                entry("archive/src/B.kt"),
            ),
        )

        assertEquals(
            listOf("src/A.kt", "src/B.kt"),
            result.source.items.mapNotNull { it.relativePath }.sorted(),
        )
    }

    @Test
    fun collisions_areReportedAndFirstEntryWins() {
        val result = ZipIntakePlanner.build(
            listOf(
                entry("pkg/a.txt", content = "first"),
                entry("pkg//a.txt", content = "second"),
            ),
        )

        assertEquals(1, result.source.items.size)
        assertEquals(1, result.issues.count { it.code == ZipIntakeIssueCode.COLLISION })
        assertEquals("first", result.contentByRelativePath["a.txt"])
    }

    @Test
    fun uploadPlanIntegration_safeZipEntriesFlowIntoPlan() {
        val zip = ZipIntakePlanner.build(
            listOf(
                entry("bundle/docs/readme.md"),
                entry("bundle/docs/changelog.md"),
            ),
        )
        val target = RepoTarget(
            owner = "octo",
            repo = "painkiller",
            branch = BranchTarget("main"),
            targetPath = TargetPath("imports"),
        )

        val filePlan = (FilePlanBuilder.build(zip.source, target.targetPath.normalized) as FilePlanBuildResult.Success).plan
        val uploadPlan = UploadPlanBuilder.build(filePlan, target)

        assertEquals(2, uploadPlan.safeEntries.size + uploadPlan.warningEntries.size)
        assertEquals(
            listOf("imports/docs/changelog.md", "imports/docs/readme.md"),
            uploadPlan.safeEntries.map { it.repoPath }.sorted(),
        )
    }

    @Test
    fun ignoredZipEntries_areClassifiedByDefaultIgnoreRules() {
        val zip = ZipIntakePlanner.build(
            listOf(
                entry("bundle/.git/config"),
                entry("bundle/src/App.kt"),
            ),
        )

        val filePlan = (FilePlanBuilder.build(zip.source, targetPathRaw = "") as FilePlanBuildResult.Success).plan

        assertEquals(listOf("src/App.kt"), filePlan.includedFiles.map { it.repoPath })
        assertEquals(listOf(".git/config"), filePlan.ignoredFiles.map { it.repoPath })
    }

    @Test
    fun safeZipEntries_canBeConvertedToMultiFileCommitEntries() {
        val zip = ZipIntakePlanner.build(
            listOf(
                entry("wrapper/docs/a.md", content = "A"),
                entry("wrapper/docs/b.md", content = "B"),
            ),
        )
        val target = RepoTarget(
            owner = "octo",
            repo = "painkiller",
            branch = BranchTarget("main"),
            targetPath = TargetPath("imports"),
        )
        val filePlan = (FilePlanBuilder.build(zip.source, target.targetPath.normalized) as FilePlanBuildResult.Success).plan

        val commitEntries = filePlan.includedFiles.map { planned ->
            MultiFileCommitEntry(
                repoPath = planned.repoPath,
                contentBase64 = zip.contentByRelativePath.getValue(planned.sourceId),
            )
        }

        assertEquals(listOf("imports/docs/a.md", "imports/docs/b.md"), commitEntries.map { it.repoPath }.sorted())
        assertEquals(listOf("A", "B"), commitEntries.map { it.contentBase64 }.sorted())
    }

    private fun entry(path: String, content: String = "x"): ZipIntakeEntry = ZipIntakeEntry(
        entryName = path,
        sizeBytes = content.length.toLong(),
        contentBase64 = content,
    )
}
