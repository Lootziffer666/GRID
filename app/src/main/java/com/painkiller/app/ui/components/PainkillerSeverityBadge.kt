package com.painkiller.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.painkiller.app.domain.model.DiagnosticSeverity
import com.painkiller.app.ui.theme.PainkillerColors
import com.painkiller.app.ui.theme.PainkillerShapes
import com.painkiller.app.ui.theme.PainkillerTheme

/**
 * Compact pill-style badge that shows a [DiagnosticSeverity] label.
 *
 * Visual grammar: small corner radius (4.dp), tinted background, high
 * contrast text. Used by file plan cards and the preview screen.
 */
@Composable
fun PainkillerSeverityBadge(
    severity: DiagnosticSeverity,
    modifier: Modifier = Modifier,
    label: String = severity.defaultLabel(),
) {
    val container = severity.containerColor()
    val onContainer = severity.onContainerColor()
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = onContainer,
        modifier = modifier
            .background(container, PainkillerShapes.Small)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

private fun DiagnosticSeverity.defaultLabel(): String = when (this) {
    DiagnosticSeverity.SAFE -> "SAFE"
    DiagnosticSeverity.WARNING -> "WARNING"
    DiagnosticSeverity.BLOCKED -> "BLOCKED"
    DiagnosticSeverity.DEFERRED -> "DEFERRED"
}

private fun DiagnosticSeverity.containerColor(): Color = when (this) {
    DiagnosticSeverity.SAFE -> PainkillerColors.SeveritySafe
    DiagnosticSeverity.WARNING -> PainkillerColors.SeverityWarning
    DiagnosticSeverity.BLOCKED -> PainkillerColors.SeverityBlocked
    DiagnosticSeverity.DEFERRED -> PainkillerColors.SeverityDeferred
}

private fun DiagnosticSeverity.onContainerColor(): Color = when (this) {
    DiagnosticSeverity.SAFE -> PainkillerColors.OnDark
    DiagnosticSeverity.WARNING -> PainkillerColors.OnLight
    DiagnosticSeverity.BLOCKED -> PainkillerColors.OnDark
    DiagnosticSeverity.DEFERRED -> PainkillerColors.OnDark
}

@Preview
@Composable
private fun PainkillerSeverityBadgePreview() {
    PainkillerTheme {
        PainkillerSeverityBadge(severity = DiagnosticSeverity.WARNING)
    }
}
