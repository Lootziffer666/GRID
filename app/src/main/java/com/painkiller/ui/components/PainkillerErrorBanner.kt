package com.painkiller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.painkiller.ui.theme.PainkillerColors
import com.painkiller.ui.theme.PainkillerSpacing

@Composable
fun PainkillerErrorBanner(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PainkillerColors.Navy, MaterialTheme.shapes.medium)
            .padding(PainkillerSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = androidx.compose.ui.graphics.Color.White,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = androidx.compose.ui.graphics.Color.White,
        )
    }
}
