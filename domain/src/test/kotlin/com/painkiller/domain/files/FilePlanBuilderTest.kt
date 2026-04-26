package com.painkiller.domain.files

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilePlanBuilderTest {

    @Test
    fun singleFileSource_buildsPlanAtRootPath() {
        val source = SelectedSource(
            kind = SourceKind.SINGLE_FILE,
            items = listOf(
                SelectedSourceItem(
                    sourceId = "1",
                    displayName = "notes.md",
                    sizeBytes = 128L,
                    mimeType = "text/markdown"
                )
            )
        )

        val result = FilePlanBuilder.build(source, targetPathRaw = "")

        val plan = (result as FilePlanBuildResult.Success).plan
        assertEquals("", plan.targetPath)
        assertEquals(1, plan.includedFiles.size)
        assertEquals("notes.md", plan.includedFiles.first().repoPath)
    }

    @Test
    fun multipleFileSource_buildsPlanWithNestedTargetPath() {
        val source = SelectedSource(
            kind = SourceKind.MULTIPLE_FILES,
            items = listOf(
                SelectedSourceItem(sourceId = "2", displayName = "A.txt", relativePath = "A.txt"),
                SelectedSourceItem(sourceId = "1", displayName = "B.txt", relativePath = "B.txt")
            )
        )

        val result = FilePlanBuilder.build(source, targetPathRaw = "docs/releases")

        val plan = (result as FilePlanBuildResult.Success).plan
        assertEquals(
            listOf("docs/releases/A.txt", "docs/releases/B.txt"),
            plan.includedFiles.map { it.repoPath }
        )
    }

    @Test
    fun folderSource_usesRelativePaths() {
        val source = SelectedSource(
            kind = SourceKind.FOLDER,
            items = listOf(
                SelectedSourceItem(sourceId = "1", displayName = "main.kt", relativePath = "src/main.kt"),
                SelectedSourceItem(sourceId = "2", displayName = "test.kt", relativePath = "src/test.kt")
            )
        )

        val result = FilePlanBuilder.build(source, targetPathRaw = "project")

        val plan = (result as FilePlanBuildResult.Success).plan
        assertEquals(
            listOf("project/src/main.kt", "project/src/test.kt"),
            plan.includedFiles.map { it.repoPath }
        )
    }

    @Test
    fun zipSource_supportedAsDistinctKindWithoutExtractionLogic() {
        val source = SelectedSource(
            kind = SourceKind.ZIP,
            items = listOf(
                SelectedSourceItem(sourceId = "z1", displayName = "archive-entry.txt", relativePath = "archive-entry.txt")
            )
        )

        val result = FilePlanBuilder.build(source, targetPathRaw = "imports")

        val plan = (result as FilePlanBuildResult.Success).plan
        assertEquals(SourceKind.ZIP, plan.sourceKind)
        assertEquals("imports/archive-entry.txt", plan.includedFiles.first().repoPath)
    }

    @Test
    fun unsafeTargetPath_returnsValidationError() {
        val source = SelectedSource(
            kind = SourceKind.SINGLE_FILE,
            items = listOf(SelectedSourceItem(sourceId = "1", displayName = "safe.txt"))
        )

        val result = FilePlanBuilder.build(source, targetPathRaw = "../unsafe")

        val error = result as FilePlanBuildResult.ValidationError
        assertTrue(error.issues.any { it.code == FilePlanIssueCode.INVALID_TARGET_PATH })
    }

    @Test
    fun defaultIgnoreRules_moveIgnoredFilesToIgnoredList() {
        val source = SelectedSource(
            kind = SourceKind.FOLDER,
            items = listOf(
                SelectedSourceItem(sourceId = "1", displayName = "config", relativePath = ".git/config"),
                SelectedSourceItem(sourceId = "2", displayName = "app.kt", relativePath = "src/app.kt")
            )
        )

        val result = FilePlanBuilder.build(source, targetPathRaw = "")

        val plan = (result as FilePlanBuildResult.Success).plan
        assertEquals(listOf("src/app.kt"), plan.includedFiles.map { it.repoPath })
        assertEquals(listOf(".git/config"), plan.ignoredFiles.map { it.repoPath })
    }

    @Test
    fun duplicateNormalizedPaths_reportIssueAndKeepFirstEntry() {
        val source = SelectedSource(
            kind = SourceKind.MULTIPLE_FILES,
            items = listOf(
                SelectedSourceItem(sourceId = "1", displayName = "a.txt", relativePath = "folder//a.txt"),
                SelectedSourceItem(sourceId = "2", displayName = "a-copy.txt", relativePath = "folder/a.txt")
            )
        )

        val result = FilePlanBuilder.build(source, targetPathRaw = "")

        val plan = (result as FilePlanBuildResult.Success).plan
        assertEquals(1, plan.includedFiles.size)
        assertEquals("folder/a.txt", plan.includedFiles.first().repoPath)
        assertTrue(plan.issues.any { it.code == FilePlanIssueCode.DUPLICATE_REPO_PATH })
    }

    @Test
    fun emptySourceList_isRejected() {
        val source = SelectedSource(kind = SourceKind.MULTIPLE_FILES, items = emptyList())

        val result = FilePlanBuilder.build(source, targetPathRaw = "")

        val error = result as FilePlanBuildResult.ValidationError
        assertTrue(error.issues.any { it.code == FilePlanIssueCode.EMPTY_SOURCE })
    }

    @Test
    fun deterministicOrdering_isStableAcrossInputOrder() {
        val source = SelectedSource(
            kind = SourceKind.MULTIPLE_FILES,
            items = listOf(
                SelectedSourceItem(sourceId = "3", displayName = "third", relativePath = "z/3.txt"),
                SelectedSourceItem(sourceId = "1", displayName = "first", relativePath = "a/1.txt"),
                SelectedSourceItem(sourceId = "2", displayName = "second", relativePath = "a/2.txt")
            )
        )

        val result = FilePlanBuilder.build(source, targetPathRaw = "")

        val plan = (result as FilePlanBuildResult.Success).plan
        assertEquals(
            listOf("a/1.txt", "a/2.txt", "z/3.txt"),
            plan.includedFiles.map { it.repoPath }
        )
    }
}
