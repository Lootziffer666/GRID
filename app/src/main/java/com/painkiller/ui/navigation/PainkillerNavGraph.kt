package com.painkiller.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.painkiller.di.PainkillerContainer
import com.painkiller.ui.auth.AuthScreen
import com.painkiller.ui.auth.AuthViewModel
import com.painkiller.ui.components.PainkillerInfoCard
import com.painkiller.ui.components.PainkillerPrimaryActionButton
import com.painkiller.ui.theme.PainkillerSpacing

private const val ROUTE_AUTH = "auth"
private const val ROUTE_UPLOAD = "upload"

@Composable
fun PainkillerNavGraph(
    container: PainkillerContainer,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.factory(container.authRepository),
    )

    NavHost(
        navController = navController,
        startDestination = ROUTE_AUTH,
        modifier = modifier,
    ) {
        composable(ROUTE_AUTH) {
            AuthScreen(
                viewModel = authViewModel,
                onAuthenticated = {
                    navController.navigate(ROUTE_UPLOAD) {
                        popUpTo(ROUTE_AUTH) { inclusive = true }
                    }
                },
            )
        }
        composable(ROUTE_UPLOAD) {
            UploadEntryScreen(
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(ROUTE_AUTH) {
                        popUpTo(ROUTE_UPLOAD) { inclusive = true }
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UploadEntryScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Painkiller") },
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
                .padding(PainkillerSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.md),
        ) {
            PainkillerInfoCard(
                title = "Ready to upload",
                body = "File picker and upload flow coming in the next gate.",
            )
            PainkillerPrimaryActionButton(
                text = "Sign out",
                onClick = onSignOut,
                enabled = true,
            )
        }
    }
}
