package com.grid.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.painkiller.domain.model.DiagnosticSeverity
import com.grid.app.ui.theme.GridColors

@Composable
fun GridSeverityBadge(
    severity: DiagnosticSeverity,
    modifier: Modifier = Modifier,
) {
    val (label, container, content) = when (severity) {
        DiagnosticSeverity.SAFE -> Triple("Safe", GridColors.PrimaryTeal, Color.White)
        DiagnosticSeverity.WARNING -> Triple("Warning", GridColors.EdgeSoft, GridColors.OnLight)
        DiagnosticSeverity.BLOCKED -> Triple("Blocked", GridColors.Navy, Color.White)
        DiagnosticSeverity.DEFERRED -> Triple("Deferred", GridColors.DarkSurfaceVariant, GridColors.OnDark)
    }
    Text(
        text = label,
        color = content,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .background(container, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
