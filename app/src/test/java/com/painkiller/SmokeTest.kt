package com.painkiller

import com.painkiller.domain.github.RepoCoordinates
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * App-side smoke test. Confirms the :domain module is wired into :app.
 * Domain test coverage lives in the :domain module.
 */
class SmokeTest {

    @Test
    fun domainTypesReachable() {
        val c = RepoCoordinates(owner = "octocat", repo = "hello-world", branch = "main")
        assertEquals("octocat/hello-world", c.fullName)
    }
}
