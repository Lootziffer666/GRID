package com.grid.app.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.grid.app.R
import com.grid.app.di.GridContainer
import com.grid.app.ui.theme.GridSpacing

private const val ROUTE_HOME = "home"

/**
 * Shell-level navigation graph.
 *
 * Shows a home screen listing registered feature modules. Feature-specific
 * routes are loaded dynamically from registered modules in later gates.
 *
 * Reactively observes [ModuleRegistry.activeModules] so the UI recomposes
 * whenever modules are registered or disabled at runtime.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridNavGraph(
    container: GridContainer,
    darkModeEnabled: Boolean,
    onToggleDarkMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val activeModules by container.moduleRegistry.activeModules.collectAsState()
    val disabledModules by container.moduleRegistry.disabledModules.collectAsState()

    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME,
        modifier = modifier,
    ) {
        composable(ROUTE_HOME) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.app_name)) },
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
                        .padding(GridSpacing.md)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(GridSpacing.md),
                ) {
                    Text(
                        text = "Registered Modules",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (activeModules.isEmpty()) {
                        Text(
                            text = "No feature modules registered yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        activeModules.forEach { entry ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.padding(GridSpacing.md),
                                    verticalArrangement = Arrangement.spacedBy(GridSpacing.xs),
                                ) {
                                    Text(
                                        text = entry.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        text = "ID: ${entry.id.value}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    if (disabledModules.isNotEmpty()) {
                        Text(
                            text = "Disabled Modules",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        disabledModules.forEach { (id, reason) ->
                            Text(
                                text = "${id.value}: $reason",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}
