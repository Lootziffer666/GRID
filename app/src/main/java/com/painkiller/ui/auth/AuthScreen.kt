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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    darkModeEnabled: Boolean,
    onToggleDarkMode: () -> Unit,
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
                actions = {
                    TextButton(onClick = onToggleDarkMode) {
                        Text(if (darkModeEnabled) "Light mode" else "Dark mode")
                    }
                },
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
                supportingText = {
                    Text(
                        text = buildString {
                            append(state.statusHint)
                            state.tokenKindLabel?.let { append("  •  $it") }
                        },
                        color = if (state.formatLooksValid) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
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

            HorizontalDivider()

            PainkillerInfoCard(
                title = "OAuth authorization code (experimental)",
                body = if (state.isOAuthAvailable) {
                    "Optional path: paste the one-time authorization code. " +
                        "PAT remains the default stable login."
                } else {
                    "Disabled in this build. OAuth code exchange backend is not configured. " +
                        "Use Personal Access Token."
                },
            )

            OutlinedTextField(
                value = state.oauthCodeInput,
                onValueChange = viewModel::onAuthorizationCodeChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Authorization code") },
                placeholder = { Text("Paste code") },
                singleLine = true,
            )

            PainkillerPrimaryActionButton(
                text = if (state.isSubmitting) "Exchanging…" else "Sign in with OAuth code",
                onClick = viewModel::signInWithAuthorizationCode,
                enabled = state.canSubmitOAuthCode && state.isOAuthAvailable,
            )

            HorizontalDivider()

            PainkillerInfoCard(
                title = "GitHub App installation (dev-only broker)",
                body = if (state.isGithubAppAvailable) {
                    "Experimental/dev path: paste installation id for broker exchange."
                } else {
                    "Disabled in this build. A hosted token broker is required. " +
                        "Normal users should use PAT sign-in.",
                },
            )

            OutlinedTextField(
                value = state.installationIdInput,
                onValueChange = viewModel::onInstallationIdChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Installation ID") },
                placeholder = { Text("e.g. 12345678") },
                singleLine = true,
            )

            PainkillerPrimaryActionButton(
                text = if (state.isSubmitting) "Signing in…" else "Sign in with GitHub App",
                onClick = viewModel::signInWithGithubAppInstallation,
                enabled = state.canSubmitInstallation && state.isGithubAppAvailable,
            )
        }
    }
}
