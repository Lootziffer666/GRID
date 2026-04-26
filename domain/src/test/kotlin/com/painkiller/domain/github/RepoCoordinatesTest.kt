package com.painkiller.domain.github

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RepoCoordinatesTest {

    @Test
    fun fullName_isOwnerSlashRepo() {
        val c = RepoCoordinates(owner = "octocat", repo = "hello-world", branch = "main")
        assertEquals("octocat/hello-world", c.fullName)
    }

    @Test
    fun refPath_isHeadsSlashBranch() {
        val c = RepoCoordinates(owner = "octocat", repo = "hello-world", branch = "feature/x")
        assertEquals("heads/feature/x", c.refPath)
    }

    @Test
    fun blankOwner_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            RepoCoordinates(owner = " ", repo = "r", branch = "main")
        }
    }

    @Test
    fun blankBranch_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            RepoCoordinates(owner = "o", repo = "r", branch = "")
        }
    }
}
