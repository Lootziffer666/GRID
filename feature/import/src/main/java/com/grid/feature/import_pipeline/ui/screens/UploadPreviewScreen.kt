package com.grid.feature.import_pipeline.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.painkiller.domain.model.DiagnosticSeverity
import com.painkiller.domain.upload.UploadPlan
import com.painkiller.domain.upload.UploadPlanEntry
import com.grid.app.ui.components.PainkillerPrimaryActionButton
import com.grid.app.ui.components.PainkillerSeverityBadge
import com.grid.app.ui.components.PainkillerWarningCard
import com.grid.feature.import_pipeline.ui.preview.Gate5PreviewSample
import com.grid.app.ui.theme.PainkillerSpacing
import com.grid.app.ui.theme.PainkillerTheme

/**
 * Gate 5 — upload diagnosis preview screen.
 *
 * Shows the [UploadPlan] grouped by severity, the target repository, and a
 * suggested commit message. The Confirm button is disabled when:
 *   - [UploadPlan.isBlockedForCommit] is true (blocked files present), or
 *   - [onConfirm] is null (commit wiring not yet available — Gates 6 / 7).
 *
 * No GitHub write is initiated here. All data is read from [plan], which
 * is a pure, stateless value computed by [com.painkiller.domain.upload.UploadPlanBuilder].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadPreviewScreen(
    plan: UploadPlan,
    onConfirm: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Upload Preview") },
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
            TargetCard(plan = plan)

            if (plan.isBlockedForCommit) {
                PainkillerWarningCard(
                    severity = DiagnosticSeverity.BLOCKED,
                    title = "Upload blocked",
                    body = "One or more files are too large for a normal Git commit. " +
                        "Remove or replace the blocked files before confirming.",
                )
            }

            EntryGroup(
                title = "Safe to commit",
                entries = plan.safeEntries,
                severity = DiagnosticSeverity.SAFE,
            )
            EntryGroup(
                title = "Warning — large but allowed",
                entries = plan.warningEntries,
                severity = DiagnosticSeverity.WARNING,
            )
            EntryGroup(
                title = "Blocked — too large",
                entries = plan.blockedEntries,
                severity = DiagnosticSeverity.BLOCKED,
            )
            EntryGroup(
                title = "Deferred — requires later workflow",
                entries = plan.deferredEntries,
                severity = DiagnosticSeverity.DEFERRED,
            )

            if (plan.ignoredEntries.isNotEmpty()) {
                Text(
                    text = "${plan.ignoredEntries.size} file(s) ignored by rule",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            CommitMessageCard(message = plan.suggestedCommitMessage)

            PainkillerPrimaryActionButton(
                text = if (plan.isBlockedForCommit) "Blocked — resolve issues first"
                else if (onConfirm == null) "Confirm (commit wiring pending)"
                else "Confirm upload",
                onClick = { onConfirm?.invoke() },
                enabled = !plan.isBlockedForCommit && onConfirm != null,
            )
        }
    }
}

@Composable
private fun TargetCard(plan: UploadPlan, modifier: Modifier = Modifier) {
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
            verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs),
        ) {
            Text("Target", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "${plan.target.owner}/${plan.target.repo}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Branch: ${plan.target.branch.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (plan.target.targetPath.normalized.isNotEmpty()) {
                Text(
                    text = "Path: ${plan.target.targetPath.normalized}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EntryGroup(
    title: String,
    entries: List<UploadPlanEntry>,
    severity: DiagnosticSeverity,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs),
        ) {
            PainkillerSeverityBadge(severity = severity)
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Column {
                entries.forEachIndexed { index, entry ->
                    EntryRow(entry = entry)
                    if (index < entries.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: UploadPlanEntry, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PainkillerSpacing.md, vertical = PainkillerSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = entry.repoPath.substringAfterLast('/'),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        val sizeLabel = entry.sizeBytes?.let { formatBytes(it) } ?: "?"
        Text(
            text = sizeLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CommitMessageCard(message: String, modifier: Modifier = Modifier) {
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
            verticalArrangement = Arrangement.spacedBy(PainkillerSpacing.xs),
        ) {
            Text("Commit message", style = MaterialTheme.typography.titleSmall)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MiB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.1f KiB".format(bytes / 1024.0)
    else -> "$bytes B"
}

@Preview(showBackground = true)
@Composable
private fun UploadPreviewScreenPreview() {
    PainkillerTheme {
        UploadPreviewScreen(plan = Gate5PreviewSample.plan)
    }
}
