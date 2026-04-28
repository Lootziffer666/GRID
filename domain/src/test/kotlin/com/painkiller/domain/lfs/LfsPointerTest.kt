package com.painkiller.domain.lfs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LfsPointerTest {

    @Test
    fun pointerFormat_matchesSpec() {
        val bytes = "hello".toByteArray()
        val plan = LfsPointer.buildPlan(bytes)

        val expectedOid = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
        assertEquals(expectedOid, plan.oid.value)
        assertEquals(5L, plan.sizeBytes)
        assertEquals(
            "version https://git-lfs.github.com/spec/v1\n" +
                "oid sha256:$expectedOid\n" +
                "size 5\n",
            plan.pointerText,
        )
    }

    @Test
    fun sha256_isLowercaseHex() {
        val hex = LfsPointer.sha256Hex(byteArrayOf(0, 1, 2, 3))
        assertTrue(hex.matches(Regex("^[a-f0-9]{64}$")))
    }
}
