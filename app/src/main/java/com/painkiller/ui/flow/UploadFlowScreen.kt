package com.painkiller.ui.flow

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.painkiller.data.files.SafFolderReader
import com.painkiller.data.files.SafZipReader
import com.painkiller.data.github.PullRequestMergeMethod
import com.painkiller.domain.error.RetrySafety
import com.painkiller.domain.upload.LargeFileRoute
import com.painkiller.domain.upload.LargeFileRouteAvailability
import com.painkiller.domain.conflict.ConflictDecision
import com.painkiller.domain.conflict.ConflictPreset
import com.painkiller.domain.github.GithubBranchSummary
import com.painkiller.domain.github.GithubPullRequestSummary
import com.painkiller.domain.github.GithubReleaseSummary
import com.painkiller.domain.github.GithubRepositorySummary
import com.painkiller.ui.components.PainkillerErrorBanner
import kotlinx.coroutines.launch
import com.painkiller.ui.components.PainkillerInfoCard
import com.painkiller.ui.components.PainkillerPrimaryActionButton
import com.painkiller.ui.theme.PainkillerSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadFlowScreen(
    viewModel: UploadFlowViewModel,
    safFolderReader: SafFolderReader,
    safZipReader: SafZipReader,
    darkModeEnabled: Boolean,
    onToggleDarkMode: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    var isLoadingFolder by remember { mutableStateOf(false) }
    var isLoadingZip by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        uri?.let { viewModel.onSourceUriPicked(it) }
    }
    val multipleLauncher = rememberLauncherForActivityResult(OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onMultiSourceUrisPicked(uris)
        }
    }
    val folderLauncher = rememberLauncherForActivityResult(OpenDocumentTree()) { treeUri ->
        treeUri?.let { uri ->
            scope.launch {
                isLoadingFolder = true
                val source = safFolderReader.read(uri)
                viewModel.onFolderSourceLoaded(source)
                isLoadingFolder = false
            }
        }
    }
    val zipLauncher = rememberLauncherForActivityResult(OpenDocument()) { zipUri ->
        zipUri?.let { uri ->
            scope.launch {
                isLoadingZip = true
                val result = safZipReader.read(uri)
                viewModel.onZipSourceLoaded(result)
                isLoadingZip = false
            }
        }
    }
    var showRepoDialog by remember { mutableStateOf(false) }
    var showBranchDialog by remember { mutableStateOf(false) }
    var showPullRequestDialog by remember { mutableStateOf(false) }
    var showReleaseDialog by remember { mutableStateOf(false) }
    var pendingMergeMethod by remember { mutableStateOf<PullRequestMergeMethod?>(null) }

    if (state.hasSucceeded) {
        SuccessScreen(
            sha = state.successCommitSha ?: "",
            url = state.successCommitUrl ?: "",
            paths = state.successCommittedPaths ?: emptyList(),
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
                    TextButton(onClick = onToggleDarkMode) {
                        Text(if (darkModeEnabled) "Light mode" else "Dark mode")
                    }
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
            // ── Source section ───────────────────────────────────────────────
            SectionCard(title = "Source") {
                when {
                    state.loadedFile != null -> {
                        Column(verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs)) {
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
                            if (state.isSingleLargeFileEligibleForLfs) {
                                Text(
                                    text = "This file is too large for a normal Git commit. " +
                                        "Review routing options below to choose Git LFS or Release Asset.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        }
                    }
                    state.loadedFolder != null -> {
                        Column(verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = when {
                                            state.isZipSource -> "ZIP — ${state.loadedFolder!!.items.size} safe file(s)"
                                            state.isMultipleFileSource -> "Multiple files — ${state.loadedFolder!!.items.size} file(s)"
                                            else -> "Folder — ${state.loadedFolder!!.items.size} file(s)"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                TextButton(onClick = viewModel::clearLoadedFolder) { Text("Clear") }
                            }
                            if (state.isZipSource && state.zipIssues.isNotEmpty()) {
                                val collisionLabel = if (state.zipCollisionCount > 0) {
                                    "Collisions: ${state.zipCollisionCount}. First normalized path is kept."
                                } else null
                                val unsafeCount = state.zipIssues.count {
                                    it.code == com.painkiller.domain.files.ZipIntakeIssueCode.UNSAFE_PATH
                                }
                                if (unsafeCount > 0) {
                                    Text(
                                        text = "Blocked unsafe ZIP paths: $unsafeCount. Review ZIP and retry.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                                collisionLabel?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                            }
                        }
                    }
                    isLoadingFolder || isLoadingZip -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) { CircularProgressIndicator() }
                    }
                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs),
                        ) {
                            TextButton(
                                onClick = { launcher.launch(arrayOf("*/*")) },
                                modifier = Modifier.weight(1f),
                            ) { Text("Pick file") }
                            TextButton(
                                onClick = { multipleLauncher.launch(arrayOf("*/*")) },
                                modifier = Modifier.weight(1f),
                            ) { Text("Pick files") }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs),
                        ) {
                            TextButton(
                                onClick = { folderLauncher.launch(null) },
                                modifier = Modifier.weight(1f),
                            ) { Text("Auswählen (Explorer)") }
                            TextButton(
                                onClick = { zipLauncher.launch(arrayOf("application/zip")) },
                                modifier = Modifier.weight(1f),
                            ) { Text("Pick ZIP") }
                        }
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
                    TextButton(
                        onClick = {
                            viewModel.loadPullRequestList()
                            showPullRequestDialog = true
                        },
                        enabled = state.ownerInput.isNotBlank() && state.repoInput.isNotBlank(),
                    ) {
                        Text(
                            if (state.isLoadingPullRequests) "Loading pull requests…"
                            else "Pick open PR",
                        )
                    }
                    TextButton(
                        onClick = {
                            viewModel.loadReleaseList()
                            showReleaseDialog = true
                        },
                        enabled = state.ownerInput.isNotBlank() && state.repoInput.isNotBlank(),
                    ) {
                        Text(
                            if (state.isLoadingReleases) "Loading releases…"
                            else "Pick release (optional)",
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
            state.pullRequestMergeMessage?.let { message ->
                PainkillerInfoCard(
                    title = "Pull request",
                    body = message,
                )
                TextButton(onClick = viewModel::dismissPullRequestMessage) { Text("Dismiss PR message") }
            }
            state.releaseAssetUploadMessage?.let { message ->
                PainkillerInfoCard(
                    title = "Release asset",
                    body = message,
                )
                TextButton(onClick = viewModel::dismissReleaseAssetMessage) { Text("Dismiss release message") }
            }
            state.conflictMessage?.let { message ->
                PainkillerInfoCard(
                    title = "Codex cleanup preset",
                    body = message,
                )
            }
            state.conflictReviewMessage?.let { message ->
                PainkillerInfoCard(
                    title = "Collision card review",
                    body = message,
                )
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
            state.selectedPullRequest?.let { selected ->
                SectionCard(title = "Selected pull request") {
                    Column(verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs)) {
                        Text(formatPullRequestLabel(selected), style = MaterialTheme.typography.bodyMedium)
                        val detail = state.selectedPullRequestDetail
                        if (state.isLoadingPullRequestDetail) {
                            CircularProgressIndicator()
                        } else if (detail != null) {
                            Text(
                                "Mergeable: ${detail.mergeable?.toString() ?: "unknown"} · state: ${detail.mergeableState ?: "unknown"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs)) {
                                TextButton(onClick = { pendingMergeMethod = PullRequestMergeMethod.MERGE }) {
                                    Text("Merge")
                                }
                                TextButton(onClick = { pendingMergeMethod = PullRequestMergeMethod.SQUASH }) {
                                    Text("Squash")
                                }
                                TextButton(onClick = { pendingMergeMethod = PullRequestMergeMethod.REBASE }) {
                                    Text("Rebase")
                                }
                            }
                        } else {
                            TextButton(onClick = viewModel::loadSelectedPullRequestDetail) {
                                Text("Refresh mergeability")
                            }
                        }
                    }
                }
            }
            state.selectedRelease?.let { release ->
                SectionCard(title = "Selected release") {
                    Column(verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs)) {
                        Text(
                            text = "${release.tagName} ${release.name?.let { "· $it" } ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = viewModel::clearSelectedRelease) {
                            Text("Clear selected release")
                        }
                        Text(
                            text = "Release asset upload currently supports single-file source only.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Painkiller streams this file as a Release Asset upload. This does not create a normal repo commit.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
            SectionCard(title = "Create release (optional)") {
                Column(verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs)) {
                    OutlinedTextField(
                        value = state.newReleaseTagInput,
                        onValueChange = viewModel::onNewReleaseTagChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Tag (e.g. v1.0.0)") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.newReleaseNameInput,
                        onValueChange = viewModel::onNewReleaseNameChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Release name (optional)") },
                        singleLine = true,
                    )
                    PainkillerPrimaryActionButton(
                        text = if (state.isCreatingRelease) "Creating release…" else "Create release",
                        onClick = viewModel::createReleaseFromInputs,
                        enabled = !state.isCreatingRelease &&
                            state.ownerInput.isNotBlank() &&
                            state.repoInput.isNotBlank() &&
                            state.newReleaseTagInput.isNotBlank(),
                    )
                }
            }

            SectionCard(title = "Codex collision cleanup (MVP)") {
                Column(verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs)) {
                    Text(
                        text = "Use a preset to resolve repeated collision markers in preview. Nothing is written yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Default cleanup: keep current version for all.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs)) {
                        TextButton(onClick = { viewModel.onConflictPresetChanged(ConflictPreset.KEEP_CURRENT) }) {
                            Text(if (state.selectedConflictPreset == ConflictPreset.KEEP_CURRENT) "Keep current ✓" else "Keep current")
                        }
                        TextButton(onClick = { viewModel.onConflictPresetChanged(ConflictPreset.KEEP_INCOMING) }) {
                            Text(if (state.selectedConflictPreset == ConflictPreset.KEEP_INCOMING) "Keep incoming ✓" else "Keep incoming")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs)) {
                        TextButton(onClick = { viewModel.onConflictPresetChanged(ConflictPreset.KEEP_BOTH) }) {
                            Text(if (state.selectedConflictPreset == ConflictPreset.KEEP_BOTH) "Keep both ✓" else "Keep both")
                        }
                        TextButton(onClick = { viewModel.onConflictPresetChanged(ConflictPreset.REVIEW_MANUALLY) }) {
                            Text(if (state.selectedConflictPreset == ConflictPreset.REVIEW_MANUALLY) "Manual ✓" else "Manual review")
                        }
                    }
                    PainkillerPrimaryActionButton(
                        text = "Build collision preview",
                        onClick = viewModel::buildConflictPreview,
                        enabled = state.hasSource,
                    )
                    state.conflictPlan?.let { conflictPlan ->
                        Text(
                            text = "${conflictPlan.filesWithCollisions} file(s) with collisions · " +
                                "${conflictPlan.totalCollisionBlocks} collision block(s) · " +
                                "${conflictPlan.malformedFiles} malformed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        conflictPlan.previews.take(1).forEach { preview ->
                            Text(
                                text = "Preview: ${preview.path}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = preview.resolvedContent?.take(400)
                                    ?: (preview.unresolvedReason ?: "Needs manual review."),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        PainkillerPrimaryActionButton(
                            text = "Write resolved files (disabled in Gate 29)",
                            onClick = {},
                            enabled = false,
                        )
                        TextButton(onClick = viewModel::clearConflictPreview) {
                            Text("Cancel preview")
                        }
                    }
                }
            }

            SectionCard(title = "Collision cards review (Gate 30)") {
                Column(verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs)) {
                    Text(
                        text = "Review collisions one by one with visible controls. Nothing is written until final confirmation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PainkillerPrimaryActionButton(
                        text = "Start collision cards",
                        onClick = viewModel::startConflictCardReview,
                        enabled = state.hasSource,
                    )
                    state.conflictReviewSession?.let { session ->
                        val card = session.currentCard
                        if (card != null) {
                            Text(
                                text = "Collision ${card.cardIndex} of ${card.totalCards}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = card.filePath,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Both versions changed this same place. Choose what should stay.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "Swipe right = Keep current · Swipe left = Keep incoming",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(card.ref, card.selectedDecision) {
                                        var totalDrag = 0f
                                        detectHorizontalDragGestures(
                                            onHorizontalDrag = { change, dragAmount ->
                                                totalDrag += dragAmount
                                                change.consume()
                                            },
                                            onDragEnd = {
                                                when {
                                                    totalDrag >= 120f -> {
                                                        viewModel.decideAndAdvanceConflictCard(
                                                            ConflictDecision.KEEP_CURRENT,
                                                        )
                                                    }

                                                    totalDrag <= -120f -> {
                                                        viewModel.decideAndAdvanceConflictCard(
                                                            ConflictDecision.KEEP_INCOMING,
                                                        )
                                                    }
                                                }
                                            },
                                        )
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.padding(PainkillerSpacing.sm),
                                    verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs),
                                ) {
                                    Text(
                                        text = "Current version",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                    Text(card.currentTextPreview, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = "Incoming version",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                    Text(card.incomingTextPreview, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs)) {
                                TextButton(onClick = { viewModel.decideCurrentConflictCard(ConflictDecision.KEEP_CURRENT) }) {
                                    Text("Keep current")
                                }
                                TextButton(onClick = { viewModel.decideCurrentConflictCard(ConflictDecision.KEEP_INCOMING) }) {
                                    Text("Keep incoming")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs)) {
                                TextButton(onClick = { viewModel.decideCurrentConflictCard(ConflictDecision.KEEP_BOTH) }) {
                                    Text("Keep both")
                                }
                                TextButton(onClick = { viewModel.decideCurrentConflictCard(ConflictDecision.REVIEW_MANUALLY) }) {
                                    Text("Review later")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs)) {
                                TextButton(onClick = viewModel::previousConflictCard) { Text("Previous") }
                                TextButton(onClick = viewModel::nextConflictCard) { Text("Next") }
                                TextButton(onClick = viewModel::buildConflictCardPreview) { Text("Summary / preview") }
                            }
                            Text(
                                text = "Resolved ${session.resolvedCount} · Manual ${session.manualCount} · Malformed ${session.malformedFiles.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        state.conflictReviewPreview?.let { preview ->
                            Text(
                                text = "Preview summary: ${preview.summary}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "Keep current ${preview.keepCurrentCount} · Keep incoming ${preview.keepIncomingCount} · " +
                                    "Keep both ${preview.keepBothCount} · Manual ${preview.manualCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            PainkillerPrimaryActionButton(
                                text = "Write resolved files (still disabled)",
                                onClick = {},
                                enabled = false,
                            )
                        }
                        TextButton(onClick = viewModel::closeConflictCardReview) {
                            Text("Close card review")
                        }
                    }
                }
            }

            if (state.plan == null) {
                PainkillerPrimaryActionButton(
                    text = "Review upload",
                    onClick = viewModel::buildPlan,
                    enabled = state.hasSource && !state.isCommitting && !isLoadingFolder && !isLoadingZip,
                )
            } else {
                val plan = state.plan!!
                state.routingDecision?.let { decision ->
                    SectionCard(title = "Large-file routing") {
                        Column(verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.sm)) {
                            Text(
                                text = decision.summary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            decision.options.forEach { option ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(PainkillerSpacing.sm),
                                        verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs),
                                    ) {
                                        Text(
                                            text = option.title +
                                                if (option.recommended) " (Recommended)" else "",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(option.explanation, style = MaterialTheme.typography.bodySmall)
                                        Text("GitHub result: ${option.githubEffect}", style = MaterialTheme.typography.bodySmall)
                                        Text("Repo change: ${option.repoChange}", style = MaterialTheme.typography.bodySmall)
                                        option.reason?.let {
                                            Text(
                                                text = "Why unavailable: $it",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                        Text(
                                            text = option.safetyNote,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.tertiary,
                                        )
                                        when (option.route) {
                                            LargeFileRoute.GIT_LFS_SINGLE_FILE -> {
                                                PainkillerPrimaryActionButton(
                                                    text = if (state.isUploadingLfs) "Uploading with Git LFS…" else option.actionLabel,
                                                    onClick = viewModel::uploadSingleFileViaLfs,
                                                    enabled = option.executable &&
                                                        option.availability == LargeFileRouteAvailability.AVAILABLE &&
                                                        !state.isUploadingLfs &&
                                                        !state.isCommitting,
                                                )
                                            }

                                            LargeFileRoute.RELEASE_ASSET_SINGLE_FILE -> {
                                                PainkillerPrimaryActionButton(
                                                    text = if (state.isUploadingReleaseAsset) "Uploading asset…" else option.actionLabel,
                                                    onClick = viewModel::uploadSelectedFileAsReleaseAsset,
                                                    enabled = option.executable &&
                                                        option.availability == LargeFileRouteAvailability.AVAILABLE &&
                                                        !state.isUploadingReleaseAsset,
                                                )
                                            }

                                            else -> Unit
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
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
                        Text(
                            "Safe ${plan.safeEntries.size}  ·  Warnings ${plan.warningEntries.size}  ·  " +
                                "Blocked ${plan.blockedEntries.size}  ·  Ignored ${plan.ignoredEntries.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (plan.warningEntries.isNotEmpty()) {
                            Text(
                                "Warning entries can still be committed. Review size/path warnings before confirm.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        if (plan.blockedEntries.isNotEmpty()) {
                            Text(
                                "Blocked entries must be removed or replaced before upload.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        if (state.isZipSource && state.zipCollisionCount > 0) {
                            Text(
                                "ZIP collisions detected: ${state.zipCollisionCount}. Conflicting paths were normalized and deduplicated.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        if (state.isZipSource && state.hasZipUnsafeEntries) {
                            Text(
                                "ZIP contains unsafe paths and is blocked before upload.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
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
                        text = if (plan.isBlockedForCommit)
                            "Blocked — resolve large files first"
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

    if (showPullRequestDialog) {
        PickerDialog(
            title = "Pick open pull request",
            isLoading = state.isLoadingPullRequests,
            items = state.pullRequests,
            label = { formatPullRequestLabel(it) },
            onSelect = { pr ->
                viewModel.selectPullRequest(pr)
                showPullRequestDialog = false
            },
            onDismiss = { showPullRequestDialog = false },
        )
    }

    if (showReleaseDialog) {
        PickerDialog(
            title = "Pick release",
            isLoading = state.isLoadingReleases,
            items = state.releases,
            label = { formatReleaseLabel(it) },
            onSelect = { release ->
                viewModel.selectRelease(release)
                showReleaseDialog = false
            },
            onDismiss = { showReleaseDialog = false },
        )
    }

    pendingMergeMethod?.let { method ->
        AlertDialog(
            onDismissRequest = { pendingMergeMethod = null },
            title = { Text("Confirm PR merge") },
            text = {
                Text(
                    "Run ${method.name.lowercase()} merge for selected pull request? " +
                        "This writes to the target branch.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.mergeSelectedPullRequest(method)
                        pendingMergeMethod = null
                    },
                ) { Text("Merge now") }
            },
            dismissButton = {
                TextButton(onClick = { pendingMergeMethod = null }) { Text("Cancel") }
            },
        )
    }
}

// ─── Success screen ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessScreen(
    sha: String,
    url: String,
    paths: List<String>,
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
                body = when {
                    paths.size == 1 -> "Committed to ${paths[0]}"
                    paths.isEmpty() -> "Commit successful"
                    else -> "${paths.size} files committed"
                },
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
                    if (paths.size > 1) {
                        HorizontalDivider()
                        for (path in paths.take(20)) {
                            Text(
                                text = path,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (paths.size > 20) {
                            Text(
                                text = "… and ${paths.size - 20} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            PainkillerPrimaryActionButton(
                text = "Upload another",
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

private fun formatPullRequestLabel(summary: GithubPullRequestSummary): String {
    val draftTag = if (summary.draft) " [draft]" else ""
    return "#${summary.number} ${summary.title}$draftTag → ${summary.head.ref}"
}

private fun formatReleaseLabel(summary: GithubReleaseSummary): String {
    val draftTag = if (summary.draft) " [draft]" else ""
    val preTag = if (summary.prerelease) " [pre-release]" else ""
    return "${summary.tagName}$draftTag$preTag ${summary.name?.let { "· $it" } ?: ""}".trim()
}
