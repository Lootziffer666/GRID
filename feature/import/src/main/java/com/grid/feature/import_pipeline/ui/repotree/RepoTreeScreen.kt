package com.grid.feature.import_pipeline.ui.repotree

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.painkiller.domain.github.GithubBranchSummary
import com.painkiller.domain.github.GithubRepositorySummary
import com.painkiller.domain.github.PendingChangeType
import com.painkiller.domain.github.TreeEntry
import com.grid.app.ui.components.PainkillerErrorBanner
import com.grid.app.ui.components.PainkillerInfoCard
import com.grid.app.ui.components.PainkillerPrimaryActionButton
import com.grid.app.ui.theme.PainkillerSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoTreeScreen(
    viewModel: RepoTreeViewModel,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    val fileLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        uri?.let { viewModel.onFileUriPicked(it) }
    }
    val zipLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        uri?.let { viewModel.onZipUriPicked(it) }
    }

    var showRepoDialog by remember { mutableStateOf(false) }
    var showBranchDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var contextMenuPath by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    if (state.currentPath.isEmpty()) {
                        Text("Dateimanager")
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            state.breadcrumbs.forEachIndexed { index, segment ->
                                if (index > 0) Text(" / ", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = segment,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable {
                                        val path = state.breadcrumbs.take(index + 1).joinToString("/")
                                        viewModel.navigateToFolder(path)
                                    },
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (state.currentPath.isNotEmpty()) {
                        IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                actions = {
                    TextButton(onClick = onSignOut) { Text("Sign out") }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(PainkillerSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.sm),
        ) {
            // ── Repo / Branch selector ───────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(PainkillerSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs),
                        ) {
                            OutlinedTextField(
                                value = state.ownerInput,
                                onValueChange = viewModel::onOwnerChanged,
                                modifier = Modifier.weight(1f),
                                label = { Text("Owner") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = state.repoInput,
                                onValueChange = viewModel::onRepoChanged,
                                modifier = Modifier.weight(1f),
                                label = { Text("Repo") },
                                singleLine = true,
                            )
                        }
                        TextButton(onClick = {
                            viewModel.loadRepositoryList()
                            showRepoDialog = true
                        }) {
                            Text(
                                if (state.isLoadingRepos) "Lade Repositories..."
                                else "Repository auswaehlen",
                            )
                        }
                        OutlinedTextField(
                            value = state.branchInput,
                            onValueChange = viewModel::onBranchChanged,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Branch") },
                            singleLine = true,
                        )
                        TextButton(
                            onClick = {
                                viewModel.loadBranchList()
                                showBranchDialog = true
                            },
                            enabled = state.ownerInput.isNotBlank() && state.repoInput.isNotBlank(),
                        ) {
                            Text(
                                if (state.isLoadingBranches) "Lade Branches..."
                                else "Branch auswaehlen",
                            )
                        }
                        PainkillerPrimaryActionButton(
                            text = if (state.isLoadingTree) "Lade Verzeichnisbaum..." else "Verzeichnisbaum laden",
                            onClick = viewModel::loadTree,
                            enabled = !state.isLoadingTree &&
                                state.ownerInput.isNotBlank() &&
                                state.repoInput.isNotBlank() &&
                                state.branchInput.isNotBlank(),
                        )
                    }
                }
            }

            // ── Error ────────────────────────────────────────────────────────
            state.errorMessage?.let { msg ->
                item {
                    PainkillerErrorBanner(title = "Fehler", body = msg)
                }
            }

            // ── Commit success ───────────────────────────────────────────────
            state.commitResult?.let { result ->
                item {
                    PainkillerInfoCard(
                        title = "Commit erfolgreich",
                        body = buildString {
                            append("SHA: ${result.sha.take(8)}")
                            result.url?.let { append("\n$it") }
                            append("\n${result.summary}")
                        },
                    )
                    TextButton(onClick = viewModel::dismissCommitResult) {
                        Text("Schliessen")
                    }
                }
            }

            // ── Tree listing ─────────────────────────────────────────────────
            if (state.isLoadingTree) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) { CircularProgressIndicator() }
                }
            }

            if (state.hasTree) {
                if (state.treeTruncated) {
                    item {
                        Text(
                            text = "Hinweis: Der Verzeichnisbaum ist zu gross und wurde abgeschnitten. Einige Dateien oder Ordner werden moeglicherweise nicht angezeigt.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = PainkillerSpacing.xs),
                        )
                    }
                }

                // Show virtual folders (from tree entries that are subdirectories)
                val folders = state.currentLevelEntries
                    .filter { it.type == TreeEntry.TYPE_TREE }
                    .sortedBy { it.path.lowercase() }

                val files = state.currentLevelEntries
                    .filter { it.type == TreeEntry.TYPE_BLOB }
                    .sortedBy { it.path.lowercase() }

                items(folders, key = { "folder-${it.path}" }) { entry ->
                    val displayName = entry.path.substringAfterLast('/')
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateToFolder(entry.path) }
                            .padding(vertical = PainkillerSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs),
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = "Folder",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                items(files, key = { "file-${it.path}" }) { entry ->
                    val displayName = entry.path.substringAfterLast('/')
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { contextMenuPath = entry.path }
                            .padding(vertical = PainkillerSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs),
                    ) {
                        Icon(
                            Icons.Default.InsertDriveFile,
                            contentDescription = "File",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // ── Action buttons ───────────────────────────────────────────────
            if (state.hasTree) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = PainkillerSpacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs),
                    ) {
                        TextButton(onClick = { showNewFolderDialog = true }) {
                            Text("+ Neuer Ordner")
                        }
                        TextButton(onClick = { fileLauncher.launch(arrayOf("*/*")) }) {
                            Text("Datei hochladen")
                        }
                        TextButton(onClick = { zipLauncher.launch(arrayOf("application/zip")) }) {
                            Text("ZIP entpacken")
                        }
                    }
                }
            }

            // ── Pending changes ──────────────────────────────────────────────
            if (state.hasPendingChanges) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = PainkillerSpacing.xs))
                    Text(
                        text = "Geplante Aenderungen (${state.pendingChanges.size})",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                itemsIndexed(state.pendingChanges) { index, change ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = formatChangeDescription(change.type, change.sourcePath, change.targetPath),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TextButton(onClick = { viewModel.removePendingChange(index) }) {
                            Text("X", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs),
                    ) {
                        PainkillerPrimaryActionButton(
                            text = if (state.isCommitting) "Speichere..." else "Auf GitHub speichern",
                            onClick = viewModel::commitChanges,
                            enabled = !state.isCommitting,
                        )
                        TextButton(onClick = viewModel::clearPendingChanges) {
                            Text("Verwerfen")
                        }
                    }
                }
            }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────

    if (showRepoDialog && state.repositories.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showRepoDialog = false },
            title = { Text("Repository auswaehlen") },
            text = {
                LazyColumn {
                    items(state.repositories) { repo ->
                        TextButton(
                            onClick = {
                                viewModel.selectRepository(repo)
                                showRepoDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(repo.fullName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRepoDialog = false }) { Text("Abbrechen") }
            },
        )
    }

    if (showBranchDialog && state.branches.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBranchDialog = false },
            title = { Text("Branch auswaehlen") },
            text = {
                LazyColumn {
                    items(state.branches) { branch ->
                        TextButton(
                            onClick = {
                                viewModel.selectBranch(branch)
                                showBranchDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(branch.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBranchDialog = false }) { Text("Abbrechen") }
            },
        )
    }

    if (showNewFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Neuer Ordner") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Ordnername") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            viewModel.addNewFolder(folderName.trim())
                            showNewFolderDialog = false
                        }
                    },
                ) { Text("Erstellen") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Abbrechen") }
            },
        )
    }

    // Context menu for file actions (rename, delete)
    contextMenuPath?.let { path ->
        AlertDialog(
            onDismissRequest = { contextMenuPath = null },
            title = { Text(path.substringAfterLast('/')) },
            text = {
                Column {
                    TextButton(onClick = {
                        showRenameDialog = path
                        contextMenuPath = null
                    }) { Text("Umbenennen") }
                    TextButton(onClick = {
                        viewModel.deleteFile(path)
                        contextMenuPath = null
                    }) { Text("Loeschen") }
                }
            },
            confirmButton = {
                TextButton(onClick = { contextMenuPath = null }) { Text("Abbrechen") }
            },
        )
    }

    showRenameDialog?.let { oldPath ->
        var newName by remember { mutableStateOf(oldPath.substringAfterLast('/')) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Umbenennen") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Neuer Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.renameFile(oldPath, newName.trim())
                            showRenameDialog = null
                        }
                    },
                ) { Text("Umbenennen") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text("Abbrechen") }
            },
        )
    }
}

private fun formatChangeDescription(
    type: PendingChangeType,
    sourcePath: String?,
    targetPath: String?,
): String = when (type) {
    PendingChangeType.MOVE -> "Verschieben: ${sourcePath ?: "?"} -> ${targetPath ?: "?"}"
    PendingChangeType.RENAME -> "Umbenennen: ${sourcePath ?: "?"} -> ${targetPath?.substringAfterLast('/') ?: "?"}"
    PendingChangeType.DELETE -> "Loeschen: ${sourcePath ?: "?"}"
    PendingChangeType.CREATE_FOLDER -> "Neuer Ordner: ${targetPath ?: "?"}"
    PendingChangeType.UPLOAD -> "Hochladen: ${targetPath ?: "?"}"
}
