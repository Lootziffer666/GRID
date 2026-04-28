package com.painkiller.domain.upload

import com.painkiller.domain.files.SourceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LargeFileRoutingDeciderTest {

    @Test
    fun singleSmallFile_recommendsNormalCommit() {
        val decision = LargeFileRoutingDecider.decide(
            LargeFileRoutingInput(
                sourceKind = SourceKind.SINGLE_FILE,
                hasBlockedEntries = false,
                hasUnsafeZipEntries = false,
                hasSingleLargeFile = false,
                hasAnyLargeFiles = false,
                hasReleaseSelected = false,
            ),
        )

        val normal = decision.options.first { it.route == LargeFileRoute.NORMAL_COMMIT }
        val lfs = decision.options.first { it.actionLabel == "Store with Git LFS" }

        assertTrue(normal.recommended)
        assertTrue(normal.executable)
        assertFalse(lfs.executable)
    }

    @Test
    fun singleLargeFile_blocksNormal_andEnablesLfs() {
        val decision = LargeFileRoutingDecider.decide(
            LargeFileRoutingInput(
                sourceKind = SourceKind.SINGLE_FILE,
                hasBlockedEntries = true,
                hasUnsafeZipEntries = false,
                hasSingleLargeFile = true,
                hasAnyLargeFiles = true,
                hasReleaseSelected = true,
            ),
        )

        val normal = decision.options.first { it.route == LargeFileRoute.NORMAL_COMMIT }
        val lfs = decision.options.first { it.route == LargeFileRoute.GIT_LFS_SINGLE_FILE }
        val release = decision.options.first { it.route == LargeFileRoute.RELEASE_ASSET_SINGLE_FILE }

        assertFalse(normal.executable)
        assertEquals("GitHub blocks normal files above 100 MiB.", normal.reason)
        assertTrue(lfs.executable)
        assertTrue(lfs.recommended)
        assertTrue(release.executable)
    }

    @Test
    fun multiFileWithLargeEntries_disablesLfsAndRelease() {
        val decision = LargeFileRoutingDecider.decide(
            LargeFileRoutingInput(
                sourceKind = SourceKind.MULTIPLE_FILES,
                hasBlockedEntries = true,
                hasUnsafeZipEntries = false,
                hasSingleLargeFile = false,
                hasAnyLargeFiles = true,
                hasReleaseSelected = true,
            ),
        )

        val lfs = decision.options.first { it.actionLabel == "Store with Git LFS" }
        val release = decision.options.first { it.actionLabel == "Publish as Release Asset" }

        assertFalse(lfs.executable)
        assertTrue(lfs.reason?.contains("multi-file") == true)
        assertFalse(release.executable)
        assertTrue(release.reason?.contains("multi-file") == true)
    }

    @Test
    fun zipWithLargeEntries_marksLfsUnsupported() {
        val decision = LargeFileRoutingDecider.decide(
            LargeFileRoutingInput(
                sourceKind = SourceKind.ZIP,
                hasBlockedEntries = true,
                hasUnsafeZipEntries = false,
                hasSingleLargeFile = false,
                hasAnyLargeFiles = true,
                hasReleaseSelected = false,
            ),
        )

        val lfs = decision.options.first { it.actionLabel == "Store with Git LFS" }
        assertFalse(lfs.executable)
        assertTrue(lfs.reason?.contains("ZIP") == true)
    }

    @Test
    fun unsafeZip_isBlockedAndCannotBeBypassed() {
        val decision = LargeFileRoutingDecider.decide(
            LargeFileRoutingInput(
                sourceKind = SourceKind.ZIP,
                hasBlockedEntries = true,
                hasUnsafeZipEntries = true,
                hasSingleLargeFile = false,
                hasAnyLargeFiles = true,
                hasReleaseSelected = true,
            ),
        )

        val blocked = decision.options.first { it.route == LargeFileRoute.BLOCKED }
        val normal = decision.options.first { it.route == LargeFileRoute.NORMAL_COMMIT }

        assertFalse(blocked.executable)
        assertFalse(normal.executable)
        assertTrue(decision.summary.contains("unsafe paths"))
    }

    @Test
    fun releaseRoute_requiresReleaseSelectionToExecute() {
        val decision = LargeFileRoutingDecider.decide(
            LargeFileRoutingInput(
                sourceKind = SourceKind.SINGLE_FILE,
                hasBlockedEntries = true,
                hasUnsafeZipEntries = false,
                hasSingleLargeFile = true,
                hasAnyLargeFiles = true,
                hasReleaseSelected = false,
            ),
        )

        val release = decision.options.first { it.route == LargeFileRoute.RELEASE_ASSET_SINGLE_FILE }
        assertFalse(release.executable)
        assertNotNull(release.reason)
    }

    @Test
    fun unsupportedRoutes_areNeverMarkedExecutable() {
        val decision = LargeFileRoutingDecider.decide(
            LargeFileRoutingInput(
                sourceKind = SourceKind.FOLDER,
                hasBlockedEntries = true,
                hasUnsafeZipEntries = false,
                hasSingleLargeFile = false,
                hasAnyLargeFiles = true,
                hasReleaseSelected = true,
            ),
        )

        decision.options
            .filter { it.route == LargeFileRoute.UNSUPPORTED_FOR_SOURCE }
            .forEach { unsupported ->
                assertFalse(unsupported.executable)
                assertEquals(LargeFileRouteAvailability.UNAVAILABLE, unsupported.availability)
            }
    }
}
