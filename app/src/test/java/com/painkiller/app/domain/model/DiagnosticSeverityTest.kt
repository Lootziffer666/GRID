package com.painkiller.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticSeverityTest {

    @Test
    fun allFourSeveritiesAreDeclared() {
        // Gate 0 fixes the severity vocabulary used everywhere downstream.
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
