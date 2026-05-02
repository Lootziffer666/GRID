package com.painkiller.domain.conflict

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConflictBranchFreshnessGuardTest {

    @Test
    fun stale_whenShaChanged() {
        assertTrue(ConflictBranchFreshnessGuard.isStale("abc", "def"))
    }

    @Test
    fun notStale_whenShaSame() {
        assertFalse(ConflictBranchFreshnessGuard.isStale("abc", "abc"))
    }

    @Test
    fun notStale_whenMissingSha() {
        assertFalse(ConflictBranchFreshnessGuard.isStale(null, "abc"))
        assertFalse(ConflictBranchFreshnessGuard.isStale("abc", null))
    }
}
