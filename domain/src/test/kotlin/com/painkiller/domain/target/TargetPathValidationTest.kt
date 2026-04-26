package com.painkiller.domain.target

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetPathValidationTest {

    @Test
    fun targetPath_validAndNormalized() {
        val result = TargetPath.fromRaw("docs//release\\notes")

        val valid = result as TargetPathValidationResult.Valid
        assertEquals("docs/release/notes", valid.targetPath.normalized)
    }

    @Test
    fun targetPath_invalidTraversal_returnsMessage() {
        val result = TargetPath.fromRaw("../secrets")

        val invalid = result as TargetPathValidationResult.Invalid
        assertTrue(invalid.message.contains("invalid", ignoreCase = true))
        assertEquals("../secrets", invalid.rawInput)
    }

    @Test
    fun targetPath_blank_isRoot() {
        val result = TargetPath.fromRaw("   ")

        val valid = result as TargetPathValidationResult.Valid
        assertEquals("", valid.targetPath.normalized)
    }
}
