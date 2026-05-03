package com.painkiller.domain.conflict

import com.painkiller.domain.files.SourceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConflictWriteBackTest {

    private val conflictText = """
        start
        <<<<<<< HEAD
        current
        =======
        incoming
        >>>>>>> feature
        end
    """.trimIndent() + "\n"

    @Test
    fun presetPlan_includesOnlyFullyResolvedWritableFiles() {
        val plan = ConflictPresetPlanner.buildPreviewPlan(
            files = listOf(
                ConflictSourceFile("a.txt", conflictText),
                ConflictSourceFile("b.txt", conflictText),
            ),
            preset = ConflictPreset.KEEP_CURRENT,
        )

        val writePlan = ConflictWritePlanner.fromPresetPlan(
            previewPlan = plan,
            sources = listOf(
                ConflictSourceFile("a.txt", conflictText, sourceId = "content://a", writableBySaf = true),
                ConflictSourceFile("b.txt", conflictText, sourceId = null, writableBySaf = false),
            ),
        )

        assertEquals(1, writePlan.filesToWrite.size)
        assertEquals("a.txt", writePlan.filesToWrite.first().path)
        assertEquals(1, writePlan.blockedFiles.size)
        assertTrue(writePlan.summary.contains("blocked for safety"))
    }

    @Test
    fun malformedAndManualPreviewEntries_areBlocked() {
        val malformedPlan = ConflictPresetPlanner.buildPreviewPlan(
            files = listOf(
                ConflictSourceFile(
                    path = "broken.txt",
                    content = "<<<<<<< HEAD\nleft\n=======\n",
                ),
            ),
            preset = ConflictPreset.KEEP_CURRENT,
        )

        val writePlan = ConflictWritePlanner.fromPresetPlan(
            previewPlan = malformedPlan,
            sources = listOf(
                ConflictSourceFile("broken.txt", "x", sourceId = "content://x", writableBySaf = true),
            ),
        )

        assertTrue(writePlan.filesToWrite.isEmpty())
        assertEquals(1, writePlan.blockedFiles.size)
        assertTrue(writePlan.blockedFiles.first().blockedReason!!.isNotBlank())
    }

    @Test
    fun previewRequired_beforeWritePlanAndExecution() {
        val writePlan = ConflictWritePlanner.fromPresetPlan(
            previewPlan = null,
            sources = emptyList(),
        )
        assertFalse(writePlan.hasEligibleFiles)

        val result = ConflictWriteExecutor.execute(
            plan = null,
            confirmed = true,
            writer = ConflictFileWriter { _, _ -> ConflictFileWriteOutcome.Success },
        )
        assertFalse(result.didChangeFiles)
        assertTrue(result.summary.contains("Preview is required"))
    }

    @Test
    fun finalConfirmationRequired_beforeExecution() {
        val plan = ConflictWritePlan(
            filesToWrite = listOf(
                ResolvedConflictFile(
                    path = "a.txt",
                    sourceId = "content://a",
                    resolvedContent = "resolved",
                    blockedReason = null,
                ),
            ),
            blockedFiles = emptyList(),
            totalBytes = 8,
            requiresConfirmation = true,
            summary = "ready",
        )

        val result = ConflictWriteExecutor.execute(
            plan = plan,
            confirmed = false,
            writer = ConflictFileWriter { _, _ -> ConflictFileWriteOutcome.Success },
        )
        assertFalse(result.didChangeFiles)
        assertTrue(result.writtenFiles.isEmpty())
        assertTrue(result.summary.contains("cancelled"))
    }

    @Test
    fun successfulWrite_reportsWrittenFilesOnly() {
        val plan = ConflictWritePlan(
            filesToWrite = listOf(
                ResolvedConflictFile("a.txt", "content://a", "ok", null),
            ),
            blockedFiles = listOf(
                ResolvedConflictFile("b.txt", null, null, "Blocked for safety"),
            ),
            totalBytes = 2,
            requiresConfirmation = true,
            summary = "ready",
        )

        val result = ConflictWriteExecutor.execute(
            plan = plan,
            confirmed = true,
            writer = ConflictFileWriter { _, _ -> ConflictFileWriteOutcome.Success },
        )

        assertTrue(result.didChangeFiles)
        assertEquals(listOf("a.txt"), result.writtenFiles)
        assertEquals(listOf("b.txt"), result.blockedFiles)
        assertTrue(result.failedFiles.isEmpty())
    }

    @Test
    fun partialFailure_reportsWrittenAndFailed() {
        val plan = ConflictWritePlan(
            filesToWrite = listOf(
                ResolvedConflictFile("a.txt", "content://a", "ok-a", null),
                ResolvedConflictFile("b.txt", "content://b", "ok-b", null),
            ),
            blockedFiles = emptyList(),
            totalBytes = 8,
            requiresConfirmation = true,
            summary = "ready",
        )

        val result = ConflictWriteExecutor.execute(
            plan = plan,
            confirmed = true,
            writer = ConflictFileWriter { sourceId, _ ->
                if (sourceId.endsWith("b")) ConflictFileWriteOutcome.Failure("Write failed")
                else ConflictFileWriteOutcome.Success
            },
        )

        assertEquals(listOf("a.txt"), result.writtenFiles)
        assertEquals(1, result.failedFiles.size)
        assertEquals("b.txt", result.failedFiles.first().path)
        assertTrue(result.summary.contains("No commit was created"))
    }

    @Test
    fun zipEntriesAreBlocked_forSafety() {
        val plan = ConflictPresetPlanner.buildPreviewPlan(
            files = listOf(ConflictSourceFile("z.txt", conflictText)),
            preset = ConflictPreset.KEEP_CURRENT,
        )

        val writePlan = ConflictWritePlanner.fromPresetPlan(
            previewPlan = plan,
            sources = listOf(
                ConflictSourceFile(
                    path = "z.txt",
                    content = conflictText,
                    sourceId = "content://zip-entry",
                    sourceKind = SourceKind.ZIP,
                    writableBySaf = false,
                ),
            ),
        )

        assertTrue(writePlan.filesToWrite.isEmpty())
        assertEquals(1, writePlan.blockedFiles.size)
        assertTrue(writePlan.blockedFiles.first().blockedReason!!.contains("ZIP entries"))
    }
}
