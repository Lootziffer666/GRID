package com.painkiller.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.painkiller.ui.components.PainkillerErrorBanner
import com.painkiller.ui.components.PainkillerInfoCard
import com.painkiller.ui.components.PainkillerPrimaryActionButton
import com.painkiller.ui.theme.PainkillerSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) onAuthenticated()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Sign in to GitHub") },
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
                title = "Personal Access Token",
                body = "Create a classic token (ghp_…) or fine-grained token (github_pat_…) " +
                    "with repo write access at github.com → Settings → Developer settings.",
            )

            OutlinedTextField(
                value = state.tokenInput,
                onValueChange = viewModel::onTokenChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Token") },
                placeholder = { Text("ghp_…") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                supportingText = when {
                    state.tokenInput.isBlank() -> null
                    state.formatLooksValid -> {
                        { Text("Format looks valid", color = MaterialTheme.colorScheme.primary) }
                    }
                    else -> {
                        { Text("Prefix not recognised — double-check the token") }
                    }
                },
                isError = state.tokenInput.isNotBlank() && !state.formatLooksValid,
            )

            state.errorMessage?.let { msg ->
                PainkillerErrorBanner(
                    title = "Sign-in failed",
                    body = msg,
                )
            }

            PainkillerPrimaryActionButton(
                text = if (state.isSubmitting) "Signing in…" else "Sign in",
                onClick = viewModel::signIn,
                enabled = state.canSubmit,
            )
        }
    }
}
