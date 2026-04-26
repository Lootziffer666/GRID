package com.painkiller.ui

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
import com.painkiller.R
import com.painkiller.domain.model.DiagnosticSeverity
import com.painkiller.ui.components.PainkillerInfoCard
import com.painkiller.ui.components.PainkillerPrimaryActionButton
import com.painkiller.ui.components.PainkillerWarningCard
import com.painkiller.ui.theme.PainkillerSpacing
import com.painkiller.ui.theme.PainkillerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PainkillerApp() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.gate0_headline)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(PainkillerSpacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.md),
        ) {
            PainkillerInfoCard(
                title = stringResource(R.string.gate0_subhead),
                body = stringResource(R.string.gate0_body),
            )
            PainkillerWarningCard(
                severity = DiagnosticSeverity.DEFERRED,
                title = "Upload not available",
                body = "Gate 0 ships only the skeleton. File intake, GitHub auth, " +
                    "preview and commit will be added in Gates 1 through 7.",
            )
            PainkillerPrimaryActionButton(
                text = "Start upload (disabled in Gate 0)",
                onClick = { /* No action in Gate 0 by design. */ },
                enabled = false,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PainkillerAppPreview() {
    PainkillerTheme { PainkillerApp() }
}
