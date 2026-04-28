package com.painkiller.domain.upload

import com.painkiller.domain.files.SourceKind

enum class LargeFileRoute {
    NORMAL_COMMIT,
    GIT_LFS_SINGLE_FILE,
    RELEASE_ASSET_SINGLE_FILE,
    BLOCKED,
    UNSUPPORTED_FOR_SOURCE,
    DEFERRED,
}

enum class LargeFileRouteAvailability {
    AVAILABLE,
    UNAVAILABLE,
}

data class LargeFileRouteOption(
    val route: LargeFileRoute,
    val title: String,
    val explanation: String,
    val githubEffect: String,
    val repoChange: String,
    val actionLabel: String,
    val availability: LargeFileRouteAvailability,
    val executable: Boolean,
    val recommended: Boolean,
    val reason: String?,
    val safetyNote: String,
)

data class LargeFileRoutingInput(
    val sourceKind: SourceKind,
    val hasBlockedEntries: Boolean,
    val hasUnsafeZipEntries: Boolean,
    val hasSingleLargeFile: Boolean,
    val hasAnyLargeFiles: Boolean,
    val hasReleaseSelected: Boolean,
)

data class LargeFileRoutingDecision(
    val summary: String,
    val options: List<LargeFileRouteOption>,
)

object LargeFileRoutingDecider {

    fun decide(input: LargeFileRoutingInput): LargeFileRoutingDecision {
        if (input.hasUnsafeZipEntries) {
            return LargeFileRoutingDecision(
                summary = "Blocked for safety: this ZIP contains unsafe paths and cannot be uploaded.",
                options = listOf(
                    blockedOption(
                        reason = "Unsafe ZIP paths were blocked before upload.",
                        safetyNote = "No route can bypass unsafe ZIP validation.",
                    ),
                    normalCommitOption(
                        executable = false,
                        recommended = false,
                        reason = "Unsafe ZIP entries must be removed first.",
                    ),
                    unsupportedLfsOption(input.sourceKind),
                    unsupportedReleaseOption(input.sourceKind),
                ),
            )
        }

        val normalExecutable = !input.hasBlockedEntries
        val normalRecommended = normalExecutable && !input.hasSingleLargeFile

        val lfsOption = when {
            input.sourceKind == SourceKind.SINGLE_FILE && input.hasSingleLargeFile ->
                lfsAvailableOption(recommended = true)
            input.sourceKind == SourceKind.SINGLE_FILE ->
                lfsUnavailableSmallSingleOption()
            else -> unsupportedLfsOption(input.sourceKind)
        }

        val releaseOption = when {
            input.sourceKind != SourceKind.SINGLE_FILE -> unsupportedReleaseOption(input.sourceKind)
            !input.hasReleaseSelected -> releaseNeedsReleaseSelectionOption(input.hasSingleLargeFile)
            else -> releaseAvailableOption(recommended = false)
        }

        val summary = when {
            input.sourceKind == SourceKind.SINGLE_FILE && input.hasSingleLargeFile ->
                "This file is too large for a normal repo commit. Choose Git LFS or Release Asset."
            input.hasBlockedEntries && input.hasAnyLargeFiles ->
                "Large files block the normal repo commit for this source."
            normalExecutable ->
                "Normal repo commit is ready. Alternative large-file routes are shown for clarity."
            else ->
                "Normal repo commit is currently blocked."
        }

        val options = buildList {
            add(
                normalCommitOption(
                    executable = normalExecutable,
                    recommended = normalRecommended,
                    reason = if (normalExecutable) null else "GitHub blocks normal files above 100 MiB.",
                ),
            )
            add(lfsOption)
            add(releaseOption)
            if (!normalExecutable) {
                add(
                    blockedOption(
                        reason = "Painkiller blocked this route because GitHub would reject it.",
                        safetyNote = "No commit is sent while blocked entries remain.",
                    ),
                )
            }
        }

        return LargeFileRoutingDecision(summary = summary, options = options)
    }

    private fun normalCommitOption(
        executable: Boolean,
        recommended: Boolean,
        reason: String?,
    ) = LargeFileRouteOption(
        route = LargeFileRoute.NORMAL_COMMIT,
        title = "Put into repo normally",
        explanation = "Normal repo commit — best for small text/code files.",
        githubEffect = "File content is committed to normal Git history.",
        repoChange = if (executable) "Yes — creates one repo commit." else "No — blocked until issues are resolved.",
        actionLabel = "Confirm normal commit",
        availability = if (executable) LargeFileRouteAvailability.AVAILABLE else LargeFileRouteAvailability.UNAVAILABLE,
        executable = executable,
        recommended = recommended,
        reason = reason,
        safetyNote = "Painkiller never forces branch updates; normal commits stay SHA-guarded.",
    )

    private fun lfsAvailableOption(recommended: Boolean) = LargeFileRouteOption(
        route = LargeFileRoute.GIT_LFS_SINGLE_FILE,
        title = "Store large file with Git LFS",
        explanation = "Git LFS stores the large file outside normal Git history and commits a small pointer.",
        githubEffect = "Large object uploads to LFS storage first, then a pointer file is committed.",
        repoChange = "Yes — commits a small pointer file in the repo.",
        actionLabel = "Store with Git LFS",
        availability = LargeFileRouteAvailability.AVAILABLE,
        executable = true,
        recommended = recommended,
        reason = null,
        safetyNote = "If LFS upload fails, pointer commit is blocked and the repo is unchanged.",
    )

    private fun lfsUnavailableSmallSingleOption() = LargeFileRouteOption(
        route = LargeFileRoute.DEFERRED,
        title = "Store large file with Git LFS",
        explanation = "Git LFS is available for one selected large file when normal commit is blocked.",
        githubEffect = "Would upload to LFS and commit a pointer file.",
        repoChange = "Would create a pointer commit.",
        actionLabel = "Store with Git LFS",
        availability = LargeFileRouteAvailability.UNAVAILABLE,
        executable = false,
        recommended = false,
        reason = "This file size is normal; keep the simpler normal repo commit.",
        safetyNote = "Painkiller keeps large-file routes visible but does not force them.",
    )

    private fun unsupportedLfsOption(sourceKind: SourceKind) = LargeFileRouteOption(
        route = LargeFileRoute.UNSUPPORTED_FOR_SOURCE,
        title = "Store large file with Git LFS",
        explanation = "This build supports Git LFS only for one selected file.",
        githubEffect = "Multi-file, folder, and ZIP-to-LFS routing is not implemented yet.",
        repoChange = "No — not executable for this source type.",
        actionLabel = "Store with Git LFS",
        availability = LargeFileRouteAvailability.UNAVAILABLE,
        executable = false,
        recommended = false,
        reason = "Not available for ${sourceKindLabel(sourceKind)} yet.",
        safetyNote = "Unsupported routes are shown for clarity and kept disabled.",
    )

    private fun releaseAvailableOption(recommended: Boolean) = LargeFileRouteOption(
        route = LargeFileRoute.RELEASE_ASSET_SINGLE_FILE,
        title = "Publish as Release Asset",
        explanation = "Release Asset attaches the file to a GitHub Release download, not as a repo file.",
        githubEffect = "File is uploaded to the selected release as a downloadable asset.",
        repoChange = "No repo commit is created by this action.",
        actionLabel = "Publish as Release Asset",
        availability = LargeFileRouteAvailability.AVAILABLE,
        executable = true,
        recommended = recommended,
        reason = null,
        safetyNote = "Release upload is explicit and never runs automatically.",
    )

    private fun releaseNeedsReleaseSelectionOption(forLargeFile: Boolean) = LargeFileRouteOption(
        route = LargeFileRoute.RELEASE_ASSET_SINGLE_FILE,
        title = "Publish as Release Asset",
        explanation = "Release Asset attaches the file to a GitHub Release download, not as a repo file.",
        githubEffect = "Choose or create a release before this action can run.",
        repoChange = "No repo commit is created by this action.",
        actionLabel = "Publish as Release Asset",
        availability = LargeFileRouteAvailability.UNAVAILABLE,
        executable = false,
        recommended = false,
        reason = if (forLargeFile) "Pick or create a release to enable this path."
        else "Optional route; select a release if you want download-only publishing.",
        safetyNote = "Painkiller requires explicit release selection before upload.",
    )

    private fun unsupportedReleaseOption(sourceKind: SourceKind) = LargeFileRouteOption(
        route = LargeFileRoute.UNSUPPORTED_FOR_SOURCE,
        title = "Publish as Release Asset",
        explanation = "This build supports Release Asset upload only for one selected file.",
        githubEffect = "Batch upload from multi-file, folder, or ZIP sources is not implemented.",
        repoChange = "No — not executable for this source type.",
        actionLabel = "Publish as Release Asset",
        availability = LargeFileRouteAvailability.UNAVAILABLE,
        executable = false,
        recommended = false,
        reason = "Not available for ${sourceKindLabel(sourceKind)} yet.",
        safetyNote = "Unsupported routes are shown for clarity and kept disabled.",
    )

    private fun blockedOption(reason: String, safetyNote: String) = LargeFileRouteOption(
        route = LargeFileRoute.BLOCKED,
        title = "Blocked for safety",
        explanation = "Painkiller blocked this path to avoid a rejected or unsafe upload.",
        githubEffect = "No upload is sent while this block is active.",
        repoChange = "No — repo stays unchanged.",
        actionLabel = "Blocked",
        availability = LargeFileRouteAvailability.UNAVAILABLE,
        executable = false,
        recommended = false,
        reason = reason,
        safetyNote = safetyNote,
    )

    private fun sourceKindLabel(kind: SourceKind): String = when (kind) {
        SourceKind.SINGLE_FILE -> "single-file"
        SourceKind.MULTIPLE_FILES -> "multi-file"
        SourceKind.FOLDER -> "folder"
        SourceKind.ZIP -> "ZIP"
    }
}
