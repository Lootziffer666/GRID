package com.painkiller.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.painkiller.app.R
import com.painkiller.app.domain.model.DiagnosticSeverity
import com.painkiller.app.ui.components.PainkillerInfoCard
import com.painkiller.app.ui.components.PainkillerPrimaryActionButton
import com.painkiller.app.ui.components.PainkillerWarningCard
import com.painkiller.app.ui.theme.PainkillerSpacing
import com.painkiller.app.ui.theme.PainkillerTheme

/**
 * Gate 0 placeholder screen.
 *
 * Demonstrates the Painkiller theme + reusable components on a single
 * surface so the skeleton is visually verifiable. No upload logic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PainkillerPlaceholderScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.placeholder_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(PainkillerSpacing.l),
            verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.m),
        ) {
            Text(
                text = stringResource(R.string.placeholder_subtitle),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.placeholder_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PainkillerInfoCard(
                title = "Foundation in place",
                body = "Theme, shapes, spacing, and reusable components are " +
                    "ready. The upload pipeline lands in later gates.",
            )

            PainkillerWarningCard(
                title = "Large File Doctor — preview",
                body = "Files larger than 100 MiB will be blocked from a normal repo commit in a later gate.",
                severity = DiagnosticSeverity.WARNING,
            )

            PainkillerPrimaryActionButton(
                text = "Start upload (disabled in Gate 0)",
                onClick = {},
                enabled = false,
            )
        }
    }
}

@Preview
@Composable
private fun PainkillerPlaceholderScreenPreview() {
    PainkillerTheme {
        PainkillerPlaceholderScreen()
    }
}
