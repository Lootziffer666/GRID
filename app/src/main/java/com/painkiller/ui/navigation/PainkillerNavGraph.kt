package com.painkiller.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.painkiller.di.PainkillerContainer
import com.painkiller.ui.auth.AuthScreen
import com.painkiller.ui.auth.AuthViewModel
import com.painkiller.ui.flow.UploadFlowScreen
import com.painkiller.ui.flow.UploadFlowViewModel

private const val ROUTE_AUTH = "auth"
private const val ROUTE_UPLOAD = "upload"

@Composable
fun PainkillerNavGraph(
    container: PainkillerContainer,
    darkModeEnabled: Boolean,
    onToggleDarkMode: () -> Unit,
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
                darkModeEnabled = darkModeEnabled,
                onToggleDarkMode = onToggleDarkMode,
                onAuthenticated = {
                    navController.navigate(ROUTE_UPLOAD) {
                        popUpTo(ROUTE_AUTH) { inclusive = true }
                    }
                },
            )
        }
        composable(ROUTE_UPLOAD) {
            val uploadViewModel: UploadFlowViewModel = viewModel(
                factory = UploadFlowViewModel.factory(
                    safFileReader = container.safFileReader,
                    repoBranchRepository = container.repoBranchRepository,
                    pullRequestRepository = container.pullRequestRepository,
                    releaseRepository = container.releaseRepository,
                    singleFileCommitRepository = container.singleFileCommitRepository,
                    multiFileCommitRepository = container.multiFileCommitRepository,
                    lfsRepository = container.lfsRepository,
                    settingsStore = container.repoTargetSettingsStore,
                    conflictFileWriter = container.safFileWriter,
                ),
            )
            UploadFlowScreen(
                viewModel = uploadViewModel,
                safFolderReader = container.safFolderReader,
                safZipReader = container.safZipReader,
                darkModeEnabled = darkModeEnabled,
                onToggleDarkMode = onToggleDarkMode,
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
