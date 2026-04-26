package com.painkiller.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.painkiller.app.ui.theme.PainkillerTheme

/**
 * Painkiller's single primary action.
 *
 * Painkiller flows always have one obvious next step ("Confirm and
 * upload", "Try again", "Choose source"). This button anchors that step
 * with consistent shape and color.
 */
@Composable
fun PainkillerPrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Preview
@Composable
private fun PainkillerPrimaryActionButtonPreview() {
    PainkillerTheme {
        PainkillerPrimaryActionButton(text = "Confirm and upload", onClick = {})
    }
}
