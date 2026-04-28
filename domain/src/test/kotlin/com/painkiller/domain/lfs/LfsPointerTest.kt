package com.painkiller.domain.lfs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.FilterInputStream

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
    fun buildPlanFromStream_matchesByteArrayPlan() {
        val bytes = "hello stream".toByteArray()
        val fromBytes = LfsPointer.buildPlan(bytes)
        val fromStream = ByteArrayInputStream(bytes).use { LfsPointer.buildPlanFromStream(it) }

        assertEquals(fromBytes.oid.value, fromStream.oid.value)
        assertEquals(fromBytes.sizeBytes, fromStream.sizeBytes)
        assertEquals(fromBytes.pointerText, fromStream.pointerText)
    }

    @Test
    fun digestStream_countsBytesIncrementally() {
        val bytes = ByteArray(32 * 1024) { (it % 113).toByte() }
        val digest = ByteArrayInputStream(bytes).use { LfsPointer.digestStream(it) }

        assertEquals(bytes.size.toLong(), digest.sizeBytes)
        assertEquals(LfsPointer.sha256Hex(bytes), digest.sha256Hex)
    }

    @Test
    fun sha256_isLowercaseHex() {
        val hex = LfsPointer.sha256Hex(byteArrayOf(0, 1, 2, 3))
        assertTrue(hex.matches(Regex("^[a-f0-9]{64}$")))
    }

    @Test(expected = RuntimeException::class)
    fun digestStream_propagatesReadFailure() {
        val broken = object : FilterInputStream(ByteArrayInputStream(byteArrayOf(1, 2, 3))) {
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                throw RuntimeException("boom")
            }
        }
        broken.use { LfsPointer.digestStream(it) }
    }
}
