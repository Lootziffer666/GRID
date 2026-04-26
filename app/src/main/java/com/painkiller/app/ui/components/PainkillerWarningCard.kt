package com.painkiller.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.painkiller.app.domain.model.DiagnosticSeverity
import com.painkiller.app.ui.theme.PainkillerColors
import com.painkiller.app.ui.theme.PainkillerSpacing
import com.painkiller.app.ui.theme.PainkillerTheme

/**
 * Warning / error surface in the CATALON-GUARD grammar.
 *
 * Uses a tinted container so it reads as "attention required" without
 * shouting. The leading badge anchors the severity. Pass
 * [DiagnosticSeverity.BLOCKED] for blocking states.
 */
@Composable
fun PainkillerWarningCard(
    title: String,
    body: String,
    severity: DiagnosticSeverity,
    modifier: Modifier = Modifier,
) {
    val tint = when (severity) {
        DiagnosticSeverity.BLOCKED -> PainkillerColors.SeverityBlocked.copy(alpha = 0.16f)
        DiagnosticSeverity.WARNING -> PainkillerColors.SeverityWarning.copy(alpha = 0.16f)
        DiagnosticSeverity.DEFERRED -> PainkillerColors.SeverityDeferred.copy(alpha = 0.16f)
        DiagnosticSeverity.SAFE -> PainkillerColors.SeveritySafe.copy(alpha = 0.12f)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = tint,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(PainkillerSpacing.l),
            verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.s),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(PainkillerSpacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PainkillerSeverityBadge(severity = severity)
                Text(text = title, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun PainkillerWarningCardPreview() {
    PainkillerTheme {
        PainkillerWarningCard(
            title = "File too large",
            body = "This file is too large for a normal GitHub repository commit.",
            severity = DiagnosticSeverity.BLOCKED,
        )
    }
}
