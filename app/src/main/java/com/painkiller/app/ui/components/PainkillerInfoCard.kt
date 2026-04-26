package com.painkiller.app.ui.components

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
import androidx.compose.ui.tooling.preview.Preview
import com.painkiller.app.ui.theme.PainkillerSpacing
import com.painkiller.app.ui.theme.PainkillerTheme

/**
 * Surface/Card-based technical summary, in the CATALON-GUARD grammar.
 *
 * Uses the Material3 surface color and the medium corner radius so it
 * sits naturally next to FilePlan / TargetCard surfaces in later gates.
 */
@Composable
fun PainkillerInfoCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(PainkillerSpacing.l),
            verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.s),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
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
private fun PainkillerInfoCardPreview() {
    PainkillerTheme {
        PainkillerInfoCard(
            title = "Upload target",
            body = "user/repo @ main → docs/notes/",
        )
    }
}
