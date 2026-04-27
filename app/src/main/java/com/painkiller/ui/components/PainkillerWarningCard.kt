package com.painkiller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.painkiller.domain.model.DiagnosticSeverity
import com.painkiller.ui.theme.PainkillerColors
import com.painkiller.ui.theme.PainkillerSpacing

@Composable
fun PainkillerWarningCard(
    severity: DiagnosticSeverity,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    val container = when (severity) {
        DiagnosticSeverity.WARNING -> PainkillerColors.EdgeSoft.copy(alpha = 0.36f)
        DiagnosticSeverity.BLOCKED -> PainkillerColors.Navy.copy(alpha = 0.24f)
        DiagnosticSeverity.DEFERRED -> PainkillerColors.DarkSurfaceVariant.copy(alpha = 0.40f)
        DiagnosticSeverity.SAFE -> PainkillerColors.PrimaryTeal.copy(alpha = 0.24f)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(PainkillerSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs),
        ) {
            PainkillerSeverityBadge(severity = severity)
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
