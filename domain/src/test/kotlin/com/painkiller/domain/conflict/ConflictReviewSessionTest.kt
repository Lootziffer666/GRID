package com.painkiller.domain.conflict

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConflictReviewSessionTest {

    private val oneConflict = """
        start
        <<<<<<< HEAD
        current
        =======
        incoming
        >>>>>>> feature
        end
    """.trimIndent() + "\n"

    @Test
    fun reviewSession_creationFromParsedConflicts() {
        val session = ConflictReviewSessionBuilder.create(
            listOf(ConflictSourceFile("a.txt", oneConflict)),
        )

        assertEquals(1, session.totalCollisions)
        assertEquals(1, session.manualCount)
        assertNotNull(session.currentCard)
    }

    @Test
    fun oneConflict_keepCurrent() {
        val base = ConflictReviewSessionBuilder.create(listOf(ConflictSourceFile("a.txt", oneConflict)))
        val session = ConflictReviewSessionReducer.decide(base, ConflictDecision.KEEP_CURRENT)
        val preview = ConflictReviewPreviewPlanner.buildPreview(session)

        assertEquals(0, preview.manualCount)
        assertEquals("start\ncurrent\nend\n", preview.files.first().resolvedContent)
    }

    @Test
    fun oneConflict_keepIncoming() {
        val base = ConflictReviewSessionBuilder.create(listOf(ConflictSourceFile("a.txt", oneConflict)))
        val session = ConflictReviewSessionReducer.decide(base, ConflictDecision.KEEP_INCOMING)
        val preview = ConflictReviewPreviewPlanner.buildPreview(session)

        assertEquals("start\nincoming\nend\n", preview.files.first().resolvedContent)
    }

    @Test
    fun oneConflict_keepBoth() {
        val base = ConflictReviewSessionBuilder.create(listOf(ConflictSourceFile("a.txt", oneConflict)))
        val session = ConflictReviewSessionReducer.decide(base, ConflictDecision.KEEP_BOTH)
        val preview = ConflictReviewPreviewPlanner.buildPreview(session)

        assertEquals("start\ncurrent\nincoming\nend\n", preview.files.first().resolvedContent)
    }

    @Test
    fun reviewLater_manual_blocksPreviewWrite() {
        val session = ConflictReviewSessionBuilder.create(listOf(ConflictSourceFile("a.txt", oneConflict)))
        val preview = ConflictReviewPreviewPlanner.buildPreview(session)

        assertFalse(preview.writeAllowed)
        assertEquals(1, preview.manualCount)
        assertNull(preview.files.first().resolvedContent)
    }

    @Test
    fun multipleConflicts_preserveDecisionsAcrossNavigation() {
        val twoConflicts = """
            a
            <<<<<<< HEAD
            left-1
            =======
            right-1
            >>>>>>> b1
            m
            <<<<<<< HEAD
            left-2
            =======
            right-2
            >>>>>>> b2
            z
        """.trimIndent() + "\n"

        var session = ConflictReviewSessionBuilder.create(listOf(ConflictSourceFile("a.txt", twoConflicts)))
        session = ConflictReviewSessionReducer.decide(session, ConflictDecision.KEEP_CURRENT)
        session = ConflictReviewSessionReducer.next(session)
        session = ConflictReviewSessionReducer.decide(session, ConflictDecision.KEEP_INCOMING)
        session = ConflictReviewSessionReducer.previous(session)

        assertEquals(ConflictDecision.KEEP_CURRENT, session.currentCard!!.selectedDecision)

        val preview = ConflictReviewPreviewPlanner.buildPreview(session)
        assertNotNull(preview.files.first().resolvedContent)
        assertEquals("a\nleft-1\nm\nright-2\nz\n", preview.files.first().resolvedContent)
    }

    @Test
    fun summaryCounts_areCorrect() {
        var session = ConflictReviewSessionBuilder.create(
            listOf(
                ConflictSourceFile("a.txt", oneConflict),
                ConflictSourceFile("b.txt", oneConflict.replace("current", "c2").replace("incoming", "i2")),
            ),
        )
        session = ConflictReviewSessionReducer.decide(session, ConflictDecision.KEEP_CURRENT)
        session = ConflictReviewSessionReducer.next(session)
        session = ConflictReviewSessionReducer.decide(session, ConflictDecision.KEEP_BOTH)

        val preview = ConflictReviewPreviewPlanner.buildPreview(session)
        assertEquals(2, preview.totalCollisions)
        assertEquals(1, preview.keepCurrentCount)
        assertEquals(0, preview.keepIncomingCount)
        assertEquals(1, preview.keepBothCount)
        assertEquals(0, preview.manualCount)
    }

    @Test
    fun unresolvedManualDecisions_blockWrite() {
        var session = ConflictReviewSessionBuilder.create(listOf(ConflictSourceFile("a.txt", oneConflict)))
        session = ConflictReviewSessionReducer.decide(session, ConflictDecision.REVIEW_MANUALLY)
        val preview = ConflictReviewPreviewPlanner.buildPreview(session)

        assertFalse(preview.writeAllowed)
        assertTrue(preview.summary.contains("did not write any files"))
    }

    @Test
    fun malformedConflicts_cannotEnterUnsafeWriteFlow() {
        val malformed = """
            <<<<<<< HEAD
            current
            =======
        """.trimIndent() + "\n"

        val session = ConflictReviewSessionBuilder.create(listOf(ConflictSourceFile("a.txt", malformed)))
        val preview = ConflictReviewPreviewPlanner.buildPreview(session)

        assertEquals(1, session.malformedFiles.size)
        assertEquals(1, preview.malformedCount)
        assertFalse(preview.writeAllowed)
    }

    @Test
    fun noCommitOrPushCodePathInvoked() {
        val session = ConflictReviewSessionBuilder.create(listOf(ConflictSourceFile("a.txt", oneConflict)))
        val preview = ConflictReviewPreviewPlanner.buildPreview(
            ConflictReviewSessionReducer.decide(session, ConflictDecision.KEEP_CURRENT),
        )

        assertFalse(preview.writeAllowed)
    }
}
