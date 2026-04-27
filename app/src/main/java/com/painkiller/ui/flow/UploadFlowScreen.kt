package com.painkiller.ui.flow

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.painkiller.domain.error.RetrySafety
import com.painkiller.domain.github.GithubBranchSummary
import com.painkiller.domain.github.GithubRepositorySummary
import com.painkiller.ui.components.PainkillerErrorBanner
import com.painkiller.ui.components.PainkillerInfoCard
import com.painkiller.ui.components.PainkillerPrimaryActionButton
import com.painkiller.ui.theme.PainkillerSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadFlowScreen(
    viewModel: UploadFlowViewModel,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val launcher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        uri?.let { viewModel.onSourceUriPicked(it) }
    }
    var showRepoDialog by remember { mutableStateOf(false) }
    var showBranchDialog by remember { mutableStateOf(false) }

    if (state.hasSucceeded) {
        SuccessScreen(
            sha = state.successCommitSha ?: "",
            url = state.successCommitUrl ?: "",
            path = state.successCommittedPath ?: "",
            onStartOver = viewModel::startOver,
            modifier = modifier,
        )
        return
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Upload to GitHub") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(PainkillerSpacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.md),
        ) {
            // ── File section ────────────────────────────────────────────────
            SectionCard(title = "Source file") {
                if (state.loadedFile == null) {
                    PainkillerPrimaryActionButton(
                        text = "Pick a file…",
                        onClick = { launcher.launch(arrayOf("*/*")) },
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.loadedFile!!.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = formatBytes(state.loadedFile!!.sizeBytes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = viewModel::clearLoadedFile) { Text("Clear") }
                    }
                }
            }

            // ── Target section ──────────────────────────────────────────────
            SectionCard(title = "Target") {
                Column(verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
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
                    TextButton(
                        onClick = {
                            viewModel.loadRepositoryList()
                            showRepoDialog = true
                        },
                    ) {
                        Text(
                            if (state.isLoadingRepos) "Loading repositories…"
                            else "Pick from my repositories",
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
                            if (state.isLoadingBranches) "Loading branches…"
                            else "Pick branch",
                        )
                    }
                    OutlinedTextField(
                        value = state.targetPathInput,
                        onValueChange = viewModel::onTargetPathChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Target path (optional)") },
                        placeholder = { Text("e.g. docs/uploads") },
                        singleLine = true,
                    )
                }
            }

            // ── Errors ──────────────────────────────────────────────────────
            state.errorMessage?.let { msg ->
                PainkillerErrorBanner(title = "Error", body = msg)
            }
            state.humanError?.let { err ->
                Column(verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs)) {
                    PainkillerErrorBanner(title = err.title, body = err.detail)
                    Row(horizontalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs)) {
                        TextButton(onClick = viewModel::dismissError) { Text("Dismiss") }
                        if (err.retrySafety == RetrySafety.SAFE_TO_RETRY) {
                            TextButton(onClick = viewModel::confirmUpload) { Text("Try again") }
                        }
                    }
                }
            }

            // ── Plan or build-plan button ────────────────────────────────────
            if (state.plan == null) {
                PainkillerPrimaryActionButton(
                    text = "Review upload",
                    onClick = viewModel::buildPlan,
                    enabled = state.loadedFile != null && !state.isCommitting,
                )
            } else {
                val plan = state.plan!!
                SectionCard(title = "Upload plan") {
                    Column(verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs)) {
                        Text(
                            "${plan.target.owner}/${plan.target.repo}  ·  ${plan.target.branch.name}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (plan.target.targetPath.normalized.isNotEmpty()) {
                            Text(
                                "Path: ${plan.target.targetPath.normalized}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        val safeCount = plan.safeEntries.size + plan.warningEntries.size
                        Text(
                            "$safeCount file(s) to commit" +
                                if (plan.blockedEntries.isNotEmpty())
                                    "  ·  ${plan.blockedEntries.size} blocked" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (plan.isBlockedForCommit)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = viewModel::startOver) { Text("Change file or target") }
                    }
                }

                OutlinedTextField(
                    value = state.commitMessageInput,
                    onValueChange = viewModel::onCommitMessageChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Commit message") },
                    minLines = 2,
                    maxLines = 4,
                )

                if (state.isCommitting) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    PainkillerPrimaryActionButton(
                        text = if (plan.isBlockedForCommit) "Blocked — resolve large files first"
                        else "Confirm upload",
                        onClick = viewModel::confirmUpload,
                        enabled = !plan.isBlockedForCommit,
                    )
                }
            }

            Spacer(modifier = Modifier.height(PainkillerSpacing.lg))
        }
    }

    // ── Repo picker dialog ──────────────────────────────────────────────────
    if (showRepoDialog) {
        PickerDialog(
            title = "Pick repository",
            isLoading = state.isLoadingRepos,
            items = state.repositories,
            label = { it.fullName },
            onSelect = { repo ->
                viewModel.selectRepository(repo)
                showRepoDialog = false
            },
            onDismiss = { showRepoDialog = false },
        )
    }

    // ── Branch picker dialog ────────────────────────────────────────────────
    if (showBranchDialog) {
        PickerDialog(
            title = "Pick branch",
            isLoading = state.isLoadingBranches,
            items = state.branches,
            label = { it.name },
            onSelect = { branch ->
                viewModel.selectBranch(branch)
                showBranchDialog = false
            },
            onDismiss = { showBranchDialog = false },
        )
    }
}

// ─── Success screen ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessScreen(
    sha: String,
    url: String,
    path: String,
    onStartOver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Committed") },
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
                title = "Upload complete",
                body = "File committed to $path",
            )
            SectionCard(title = "Commit") {
                Column(verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs)) {
                    Text(
                        "SHA: ${sha.take(12)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (url.isNotBlank()) {
                        Text(
                            url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            PainkillerPrimaryActionButton(
                text = "Upload another file",
                onClick = onStartOver,
            )
        }
    }
}

// ─── Shared helpers ───────────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(PainkillerSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.sm),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
private fun <T> PickerDialog(
    title: String,
    isLoading: Boolean,
    items: List<T>,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }
            } else if (items.isEmpty()) {
                Text("No items found.")
            } else {
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(items) { item ->
                        Column {
                            Text(
                                text = label(item),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(item) }
                                    .padding(vertical = PainkillerSpacing.sm),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MiB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.1f KiB".format(bytes / 1024.0)
    else -> "$bytes B"
}
