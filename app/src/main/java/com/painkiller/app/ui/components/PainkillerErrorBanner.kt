package com.painkiller.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.painkiller.app.domain.model.HumanReadableError
import com.painkiller.app.ui.theme.PainkillerColors
import com.painkiller.app.ui.theme.PainkillerSpacing
import com.painkiller.app.ui.theme.PainkillerTheme

/**
 * Banner used to surface a [HumanReadableError] at the top of a screen.
 *
 * Always shows: title, explanation, whether user data was lost, and the
 * next useful step — exactly the contract from `instructions.md`
 * § "Error Message Style".
 */
@Composable
fun PainkillerErrorBanner(
    error: HumanReadableError,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = PainkillerColors.SeverityBlocked.copy(alpha = 0.18f),
                shape = MaterialTheme.shapes.medium,
            )
            .padding(PainkillerSpacing.l),
        verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.s),
    ) {
        Text(text = error.title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = error.explanation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val dataLine = if (error.userDataLost) {
            "Some data may have been lost."
        } else {
            "No user data was lost."
        }
        Text(
            text = dataLine,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Next: " + error.nextStep,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview
@Composable
private fun PainkillerErrorBannerPreview() {
    PainkillerTheme {
        PainkillerErrorBanner(
            error = HumanReadableError(
                title = "Branch changed on GitHub",
                explanation = "The branch changed on GitHub while Painkiller was preparing your upload. Painkiller stopped before updating the branch.",
                userDataLost = false,
                nextStep = "Refresh the target and try again.",
            )
        )
    }
}
