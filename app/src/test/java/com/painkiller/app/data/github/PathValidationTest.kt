package com.painkiller.app.data.github

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PathValidationTest {

    @Test
    fun blankPathNormalizesToEmpty() {
        assertEquals("", PathValidation.normalizeRepoPath(""))
        assertEquals("", PathValidation.normalizeRepoPath("   "))
    }

    @Test
    fun simplePathPassesThrough() {
        assertEquals("docs/notes.md", PathValidation.normalizeRepoPath("docs/notes.md"))
    }

    @Test
    fun backslashesAreNormalizedToForwardSlash() {
        assertEquals("docs/notes.md", PathValidation.normalizeRepoPath("docs\\notes.md"))
    }

    @Test
    fun duplicateSlashesAreCollapsed() {
        assertEquals("a/b/c", PathValidation.normalizeRepoPath("a//b///c"))
    }

    @Test
    fun leadingAndTrailingSlashesAreStripped() {
        assertEquals("a/b", PathValidation.normalizeRepoPath("/a/b/"))
    }

    @Test
    fun parentTraversalIsRejected() {
        assertNull(PathValidation.normalizeRepoPath("a/../b"))
        assertNull(PathValidation.normalizeRepoPath(".."))
        assertFalse(PathValidation.isSafeRepoPath("a/../b"))
    }

    @Test
    fun singleDotSegmentIsRejected() {
        assertNull(PathValidation.normalizeRepoPath("a/./b"))
    }

    @Test
    fun windowsAbsolutePathIsRejected() {
        assertNull(PathValidation.normalizeRepoPath("C:/users/me"))
    }

    @Test
    fun safePathIsSafe() {
        assertTrue(PathValidation.isSafeRepoPath("docs/notes.md"))
        assertTrue(PathValidation.isSafeRepoPath(""))
    }
}
