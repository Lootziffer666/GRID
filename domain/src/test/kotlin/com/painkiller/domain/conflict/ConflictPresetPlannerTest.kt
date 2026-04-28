package com.painkiller.domain.conflict

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConflictPresetPlannerTest {

    private val oneConflict = """
        line-a
        <<<<<<< HEAD
        current
        =======
        incoming
        >>>>>>> feature
        line-b
    """.trimIndent() + "\n"

    @Test
    fun noConflictMarkers_returnsNoChangeSummary() {
        val plan = ConflictPresetPlanner.buildPreviewPlan(
            files = listOf(ConflictSourceFile("a.txt", "hello\nworld\n")),
            preset = ConflictPreset.KEEP_CURRENT,
        )

        assertEquals(0, plan.totalCollisionBlocks)
        assertEquals("No conflict markers were found. Painkiller did not change any files.", plan.summary)
        assertEquals("hello\nworld\n", plan.previews.first().resolvedContent)
    }

    @Test
    fun singleConflict_keepCurrent_usesCurrentBlock() {
        val plan = ConflictPresetPlanner.buildPreviewPlan(
            files = listOf(ConflictSourceFile("a.txt", oneConflict)),
            preset = ConflictPreset.KEEP_CURRENT,
        )

        assertEquals(1, plan.totalCollisionBlocks)
        assertEquals("line-a\ncurrent\nline-b\n", plan.previews.first().resolvedContent)
    }

    @Test
    fun singleConflict_keepIncoming_usesIncomingBlock() {
        val plan = ConflictPresetPlanner.buildPreviewPlan(
            files = listOf(ConflictSourceFile("a.txt", oneConflict)),
            preset = ConflictPreset.KEEP_INCOMING,
        )

        assertEquals("line-a\nincoming\nline-b\n", plan.previews.first().resolvedContent)
    }

    @Test
    fun singleConflict_keepBoth_concatenatesCurrentAndIncoming() {
        val plan = ConflictPresetPlanner.buildPreviewPlan(
            files = listOf(ConflictSourceFile("a.txt", oneConflict)),
            preset = ConflictPreset.KEEP_BOTH,
        )

        assertEquals("line-a\ncurrent\nincoming\nline-b\n", plan.previews.first().resolvedContent)
    }

    @Test
    fun multipleConflictsInOneFile_areAllResolved() {
        val content = """
            start
            <<<<<<< HEAD
            left-1
            =======
            right-1
            >>>>>>> branch-1
            middle
            <<<<<<< HEAD
            left-2
            =======
            right-2
            >>>>>>> branch-2
            end
        """.trimIndent() + "\n"

        val plan = ConflictPresetPlanner.buildPreviewPlan(
            files = listOf(ConflictSourceFile("a.txt", content)),
            preset = ConflictPreset.KEEP_CURRENT,
        )

        assertEquals(2, plan.totalCollisionBlocks)
        assertEquals("start\nleft-1\nmiddle\nleft-2\nend\n", plan.previews.first().resolvedContent)
    }

    @Test
    fun multipleFiles_areHandledInOnePlan() {
        val files = listOf(
            ConflictSourceFile("a.txt", oneConflict),
            ConflictSourceFile("b.txt", "plain\n"),
            ConflictSourceFile("c.txt", oneConflict),
        )

        val plan = ConflictPresetPlanner.buildPreviewPlan(files, ConflictPreset.KEEP_CURRENT)

        assertEquals(3, plan.totalFiles)
        assertEquals(2, plan.filesWithCollisions)
        assertEquals(2, plan.totalCollisionBlocks)
    }

    @Test
    fun malformed_missingSeparator_isBlocked() {
        val malformed = """
            <<<<<<< HEAD
            current
            >>>>>>> feature
        """.trimIndent() + "\n"

        val plan = ConflictPresetPlanner.buildPreviewPlan(
            files = listOf(ConflictSourceFile("a.txt", malformed)),
            preset = ConflictPreset.KEEP_CURRENT,
        )

        assertEquals(1, plan.malformedFiles)
        assertNull(plan.previews.first().resolvedContent)
        assertTrue(plan.previews.first().unresolvedReason!!.contains("separator"))
    }

    @Test
    fun malformed_missingEndMarker_isBlocked() {
        val malformed = """
            <<<<<<< HEAD
            current
            =======
            incoming
        """.trimIndent() + "\n"

        val plan = ConflictPresetPlanner.buildPreviewPlan(
            files = listOf(ConflictSourceFile("a.txt", malformed)),
            preset = ConflictPreset.KEEP_CURRENT,
        )

        assertEquals(1, plan.malformedFiles)
        assertTrue(plan.previews.first().unresolvedReason!!.contains("end marker"))
    }

    @Test
    fun nestedMarkers_failSafely() {
        val malformed = """
            <<<<<<< HEAD
            current-1
            <<<<<<< nested
            current-2
            =======
            incoming-2
            >>>>>>> nested
            =======
            incoming-1
            >>>>>>> feature
        """.trimIndent() + "\n"

        val plan = ConflictPresetPlanner.buildPreviewPlan(
            files = listOf(ConflictSourceFile("a.txt", malformed)),
            preset = ConflictPreset.KEEP_CURRENT,
        )

        assertEquals(1, plan.malformedFiles)
        assertNull(plan.previews.first().resolvedContent)
        assertTrue(plan.previews.first().unresolvedReason!!.contains("Nested"))
    }

    @Test
    fun previewDoesNotMutateOriginalContent() {
        val original = oneConflict
        val plan = ConflictPresetPlanner.buildPreviewPlan(
            files = listOf(ConflictSourceFile("a.txt", original)),
            preset = ConflictPreset.KEEP_INCOMING,
        )

        assertEquals(original, plan.previews.first().originalContent)
        assertTrue(plan.previews.first().resolvedContent!!.contains("incoming"))
        assertTrue(original.contains("<<<<<<< HEAD"))
    }

    @Test
    fun keepCurrentSummary_mentionsPreviewOnly() {
        val plan = ConflictPresetPlanner.buildPreviewPlan(
            files = listOf(ConflictSourceFile("a.txt", oneConflict)),
            preset = ConflictPreset.KEEP_CURRENT,
        )

        assertTrue(plan.summary.contains("preview"))
        assertFalse(plan.writeAllowed)
    }

    @Test
    fun writeIsBlockedForMalformedFiles() {
        val malformed = """
            <<<<<<< HEAD
            current
            =======
        """.trimIndent() + "\n"

        val plan = ConflictPresetPlanner.buildPreviewPlan(
            files = listOf(ConflictSourceFile("a.txt", malformed)),
            preset = ConflictPreset.KEEP_CURRENT,
        )

        assertFalse(plan.writeAllowed)
        assertEquals(1, plan.unresolvedFiles)
    }

    @Test
    fun lineEndings_arePreservedForCrLfInput() {
        val content = "start\r\n<<<<<<< HEAD\r\nleft\r\n=======\r\nright\r\n>>>>>>> feature\r\nend\r\n"

        val plan = ConflictPresetPlanner.buildPreviewPlan(
            files = listOf(ConflictSourceFile("a.txt", content)),
            preset = ConflictPreset.KEEP_CURRENT,
        )

        assertEquals("start\r\nleft\r\nend\r\n", plan.previews.first().resolvedContent)
    }

    @Test
    fun noCommitOrPushPathExistsInPlanner() {
        val plan = ConflictPresetPlanner.buildPreviewPlan(
            files = listOf(ConflictSourceFile("a.txt", oneConflict)),
            preset = ConflictPreset.KEEP_CURRENT,
        )

        assertFalse(plan.writeAllowed)
        assertEquals(ConflictPreset.KEEP_CURRENT, plan.preset)
    }
}
