package com.grid.app.ui.components

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
import com.grid.app.ui.theme.GridColors
import com.grid.app.ui.theme.GridSpacing

@Composable
fun GridWarningCard(
    severity: DiagnosticSeverity,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    val container = when (severity) {
        DiagnosticSeverity.WARNING -> GridColors.EdgeSoft.copy(alpha = 0.36f)
        DiagnosticSeverity.BLOCKED -> GridColors.Navy.copy(alpha = 0.24f)
        DiagnosticSeverity.DEFERRED -> GridColors.DarkSurfaceVariant.copy(alpha = 0.40f)
        DiagnosticSeverity.SAFE -> GridColors.PrimaryTeal.copy(alpha = 0.24f)
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
            modifier = Modifier.padding(GridSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GridSpacing.xs),
        ) {
            GridSeverityBadge(severity = severity)
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
