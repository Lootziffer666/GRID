package com.painkiller.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticSeverityTest {

    @Test
    fun allFourSeveritiesAreDeclared() {
        assertEquals(
            listOf(
                DiagnosticSeverity.SAFE,
                DiagnosticSeverity.WARNING,
                DiagnosticSeverity.BLOCKED,
                DiagnosticSeverity.DEFERRED,
            ),
            DiagnosticSeverity.entries,
        )
    }
}
