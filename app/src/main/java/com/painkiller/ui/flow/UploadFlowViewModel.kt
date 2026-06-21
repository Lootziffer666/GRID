package com.painkiller.ui.flow

import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.painkiller.data.files.LoadedFile
import com.painkiller.data.files.SafFileReader
import com.painkiller.data.files.SafZipReader
import com.painkiller.data.github.GithubBranchListResult
import com.painkiller.data.github.GithubLfsUploadResult
import com.painkiller.data.github.GithubLfsRepository
import com.painkiller.data.github.GithubPullRequestListResult
import com.painkiller.data.github.GithubPullRequestMergeResult
import com.painkiller.data.github.GithubPullRequestRepository
import com.painkiller.data.github.GithubReleaseAssetUploadResult
import com.painkiller.data.github.GithubReleaseCreateResult
import com.painkiller.data.github.GithubReleaseListResult
import com.painkiller.data.github.GithubReleaseRepository
import com.painkiller.data.github.GithubRepoBranchRepository
import com.painkiller.data.github.GithubRepoListResult
import com.painkiller.data.github.MultiFileCommitRepository
import com.painkiller.data.github.SingleFileCommitRepository
import com.painkiller.data.settings.RepoTargetSettingsStore
import com.painkiller.domain.error.HumanReadableError
import com.painkiller.domain.error.PainkillerErrorMapper
import com.painkiller.domain.error.RecoveryHint
import com.painkiller.domain.error.RetrySafety
import com.painkiller.domain.conflict.ConflictPreset
import com.painkiller.domain.conflict.ConflictPresetPlanner
import com.painkiller.domain.conflict.ConflictDecision
import com.painkiller.domain.conflict.ConflictCommitBlockedFile
import com.painkiller.domain.conflict.ConflictCommitPlan
import com.painkiller.domain.conflict.ConflictCommitPlanner
import com.painkiller.domain.conflict.ConflictCommitResult
import com.painkiller.domain.conflict.ResolvedCommitCandidate
import com.painkiller.domain.conflict.ConflictBranchFreshnessGuard
import com.painkiller.domain.conflict.ConflictReviewPreview
import com.painkiller.domain.conflict.ConflictReviewPreviewPlanner
import com.painkiller.domain.conflict.ConflictReviewSession
import com.painkiller.domain.conflict.ConflictReviewSessionBuilder
import com.painkiller.domain.conflict.ConflictReviewSessionReducer
import com.painkiller.domain.conflict.ConflictWriteExecutor
import com.painkiller.domain.conflict.ConflictWritePlan
import com.painkiller.domain.conflict.ConflictWritePlanner
import com.painkiller.domain.conflict.ConflictWriteResult
import com.painkiller.domain.conflict.ConflictResolutionPlan
import com.painkiller.domain.conflict.ConflictSourceFile
import com.painkiller.domain.files.DefaultIgnoreRules
import com.painkiller.domain.files.FilePlan
import com.painkiller.domain.files.FilePlanBuildResult
import com.painkiller.domain.files.FilePlanBuilder
import com.painkiller.domain.files.PlannedFile
import com.painkiller.domain.files.SelectedSource
import com.painkiller.domain.files.SourceKind
import com.painkiller.domain.files.ZipIntakeIssue
import com.painkiller.domain.files.ZipIntakeIssueCode
import com.painkiller.domain.github.GithubBranchSummary
import com.painkiller.domain.github.GithubPullRequestSummary
import com.painkiller.domain.github.GithubReleaseSummary
import com.painkiller.domain.github.GithubRepositorySummary
import com.painkiller.domain.github.MultiFileCommitEntry
import com.painkiller.domain.github.MultiFileCommitInput
import com.painkiller.domain.github.MultiFileCommitResult
import com.painkiller.domain.github.SingleFileCommitInput
import com.painkiller.domain.github.SingleFileCommitResult
import com.painkiller.domain.github.UploadReleaseAssetRequest
import com.painkiller.domain.target.BranchTarget
import com.painkiller.domain.target.RepoTarget
import com.painkiller.domain.target.TargetPath
import com.painkiller.domain.target.TargetPathValidationResult
import com.painkiller.domain.upload.LargeFileRoutingDecision
import com.painkiller.domain.upload.LargeFileRoutingDecider
import com.painkiller.domain.upload.LargeFileRoutingInput
import com.painkiller.domain.upload.UploadPlan
import com.painkiller.domain.upload.UploadPlanBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

/**
 * Owns the end-to-end upload flow:
 *
 *   1. user picks a file / folder / ZIP via SAF — VM stores the source
 *   2. user picks owner/repo and branch from listings retrieved via
 *      [GithubRepoBranchRepository], plus a target path
 *   3. VM builds an [UploadPlan] via Gate 5 [UploadPlanBuilder]
 *   4. user confirms — VM submits via Gate 6 [SingleFileCommitRepository]
 *      or Gate 7 [MultiFileCommitRepository] depending on source kind
 *   5. VM emits success (with commit URL + paths) or a [HumanReadableError]
 */
class UploadFlowViewModel(
    private val safFileReader: SafFileReader,
    private val repoBranchRepository: GithubRepoBranchRepository,
    private val pullRequestRepository: GithubPullRequestRepository,
    private val releaseRepository: GithubReleaseRepository,
    private val singleFileCommitRepository: SingleFileCommitRepository,
    private val multiFileCommitRepository: MultiFileCommitRepository,
    private val lfsRepository: GithubLfsRepository,
    private val settingsStore: RepoTargetSettingsStore,
    private val conflictFileWriter: com.painkiller.domain.conflict.ConflictFileWriter,
) : ViewModel() {

    private val _state = MutableStateFlow(UploadFlowUiState())
    val state: StateFlow<UploadFlowUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = settingsStore.loadLastUsedTarget()
            if (saved != null) {
                _state.update {
                    it.copy(
                        ownerInput = saved.owner,
                        repoInput = saved.repo,
                        branchInput = saved.branch.name,
                        targetPathInput = saved.targetPath.normalized,
                    )
                }
            }
        }
    }

    // ─── source picking ──────────────────────────────────────────────────────

    fun onFolderSourceLoaded(source: SelectedSource) {
        _state.update {
            it.copy(
                loadedFolder = source,
                loadedMultiContent = null,
                loadedFile = null,
                loadedFilePlan = null,
                plan = null,
                errorMessage = null,
                zipIssues = emptyList(),
            )
        }
    }

    fun onZipSourceLoaded(result: SafZipReader.ZipReadResult) {
        _state.update {
            it.copy(
                loadedFolder = result.source,
                loadedMultiContent = result.contentByRelativePath,
                loadedFile = null,
                loadedFilePlan = null,
                plan = null,
                errorMessage = null,
                zipIssues = result.issues,
            )
        }
    }

    fun onMultiSourceUrisPicked(uris: List<Uri>) {
        _state.update {
            it.copy(
                loadedFile = null,
                loadedFolder = null,
                loadedMultiContent = null,
                loadedFilePlan = null,
                errorMessage = null,
                plan = null,
                zipIssues = emptyList(),
            )
        }
        viewModelScope.launch {
            val loaded = uris.mapNotNull { uri -> safFileReader.read(uri) }
            if (loaded.isEmpty()) {
                _state.update {
                    it.copy(errorMessage = "No readable files were selected.")
                }
                return@launch
            }
            val duplicateDisplayNames = loaded
                .groupBy { it.displayName.lowercase() }
                .filterValues { entries -> entries.size > 1 }
                .values
                .map { entries -> entries.first().displayName }
                .sorted()
            if (duplicateDisplayNames.isNotEmpty()) {
                val preview = duplicateDisplayNames.take(3).joinToString(", ")
                val suffix = if (duplicateDisplayNames.size > 3) " and more" else ""
                _state.update {
                    it.copy(
                        errorMessage = "Duplicate file names selected: $preview$suffix. " +
                            "Rename files or choose a folder/ZIP source.",
                    )
                }
                return@launch
            }
            val items = loaded.map { file ->
                file.sourceItem.copy(relativePath = file.displayName)
            }.sortedBy { it.displayName.lowercase() }
            val encoded = loaded.associate { file ->
                file.sourceItem.sourceId to (file.contentBase64 ?: "")
            }
            _state.update {
                it.copy(
                    loadedFolder = SelectedSource(
                        kind = SourceKind.MULTIPLE_FILES,
                        items = items,
                    ),
                    loadedMultiContent = encoded,
                    loadedFile = null,
                    loadedFilePlan = null,
                    plan = null,
                    errorMessage = null,
                    zipIssues = emptyList(),
                )
            }
        }
    }

    fun clearLoadedFolder() {
        _state.update {
            it.copy(
                loadedFolder = null,
                loadedMultiContent = null,
                loadedFilePlan = null,
                plan = null,
                zipIssues = emptyList(),
            )
        }
    }

    fun onSourceUriPicked(uri: Uri) {
        _state.update {
            it.copy(
                loadedFile = null,
                loadedFolder = null,
                loadedMultiContent = null,
                loadedFilePlan = null,
                errorMessage = null,
                plan = null,
                zipIssues = emptyList(),
            )
        }
        viewModelScope.launch {
            val loadedMetadata = safFileReader.readMetadata(uri)
            if (loadedMetadata == null) {
                _state.update {
                    it.copy(errorMessage = "Could not read the selected file. The link may have expired.")
                }
                return@launch
            }
            if (loadedMetadata.sizeBytes > MAX_NORMAL_COMMIT_BYTES) {
                _state.update {
                    it.copy(
                        loadedFile = loadedMetadata,
                        errorMessage = "This file is too large for a normal Git commit (>100 MiB). " +
                            "GRID blocked the upload.",
                    )
                }
                return@launch
            }
            val loaded = safFileReader.read(uri)
            if (loaded == null) {
                _state.update {
                    it.copy(errorMessage = "Could not read the selected file. The link may have expired.")
                }
                return@launch
            }
            _state.update { it.copy(loadedFile = loaded, errorMessage = null) }
        }
    }

    fun clearLoadedFile() {
        _state.update {
            it.copy(
                loadedFile = null,
                loadedFolder = null,
                loadedMultiContent = null,
                loadedFilePlan = null,
                plan = null,
                zipIssues = emptyList(),
            )
        }
    }

    // ─── target editing ──────────────────────────────────────────────────────

    fun onOwnerChanged(value: String) = _state.update { it.copy(ownerInput = value, errorMessage = null) }
    fun onRepoChanged(value: String) = _state.update { it.copy(repoInput = value, errorMessage = null) }
    fun onBranchChanged(value: String) = _state.update { it.copy(branchInput = value, errorMessage = null) }
    fun onTargetPathChanged(value: String) = _state.update { it.copy(targetPathInput = value, errorMessage = null) }
    fun onCommitMessageChanged(value: String) = _state.update { it.copy(commitMessageInput = value) }

    // ─── repo / branch listing ───────────────────────────────────────────────

    fun loadRepositoryList() {
        if (_state.value.isLoadingRepos) return
        _state.update { it.copy(isLoadingRepos = true, errorMessage = null) }
        viewModelScope.launch {
            when (val r = repoBranchRepository.listRepositories()) {
                is GithubRepoListResult.Success -> _state.update {
                    it.copy(repositories = r.repositories, isLoadingRepos = false)
                }
                is GithubRepoListResult.Failure -> _state.update {
                    it.copy(
                        isLoadingRepos = false,
                        errorMessage = PainkillerErrorMapper.mapRepoListing(r.reason).detail,
                    )
                }
            }
        }
    }

    fun loadBranchList() {
        val owner = _state.value.ownerInput.trim()
        val repo = _state.value.repoInput.trim()
        if (owner.isBlank() || repo.isBlank()) return
        if (_state.value.isLoadingBranches) return
        _state.update { it.copy(isLoadingBranches = true, errorMessage = null) }
        viewModelScope.launch {
            when (val r = repoBranchRepository.listBranches(owner, repo)) {
                is GithubBranchListResult.Success -> _state.update {
                    it.copy(branches = r.branches, isLoadingBranches = false)
                }
                is GithubBranchListResult.Failure -> _state.update {
                    it.copy(
                        isLoadingBranches = false,
                        errorMessage = PainkillerErrorMapper.mapBranchListing(r.reason).detail,
                    )
                }
            }
        }
    }

    fun loadPullRequestList() {
        val owner = _state.value.ownerInput.trim()
        val repo = _state.value.repoInput.trim()
        if (owner.isBlank() || repo.isBlank()) return
        if (_state.value.isLoadingPullRequests) return
        _state.update { it.copy(isLoadingPullRequests = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = pullRequestRepository.listOpenPullRequests(owner, repo)) {
                is GithubPullRequestListResult.Success -> _state.update {
                    it.copy(pullRequests = result.pullRequests, isLoadingPullRequests = false)
                }
                is GithubPullRequestListResult.Failure -> _state.update {
                    it.copy(isLoadingPullRequests = false, errorMessage = result.reason)
                }
            }
        }
    }

    fun selectRepository(summary: GithubRepositorySummary) {
        _state.update {
            it.copy(
                ownerInput = summary.fullName.substringBefore('/'),
                repoInput = summary.name,
                branchInput = summary.defaultBranch,
                branches = emptyList(),
            )
        }
    }

    fun selectBranch(summary: GithubBranchSummary) {
        _state.update { it.copy(branchInput = summary.name) }
    }

    fun selectPullRequest(summary: GithubPullRequestSummary) {
        _state.update {
            it.copy(
                branchInput = summary.head.ref,
                selectedPullRequest = summary,
                selectedPullRequestDetail = null,
            )
        }
        loadSelectedPullRequestDetail()
    }

    fun loadReleaseList() {
        val owner = _state.value.ownerInput.trim()
        val repo = _state.value.repoInput.trim()
        if (owner.isBlank() || repo.isBlank()) return
        if (_state.value.isLoadingReleases) return
        _state.update { it.copy(isLoadingReleases = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = releaseRepository.listReleases(owner, repo)) {
                is GithubReleaseListResult.Success -> _state.update {
                    it.copy(releases = result.releases, isLoadingReleases = false)
                }
                is GithubReleaseListResult.Failure -> _state.update {
                    it.copy(isLoadingReleases = false, errorMessage = result.reason)
                }
            }
        }
    }

    fun selectRelease(summary: GithubReleaseSummary) {
        _state.update { it.copy(selectedRelease = summary) }
    }

    fun clearSelectedRelease() {
        _state.update { it.copy(selectedRelease = null) }
    }

    fun onNewReleaseTagChanged(value: String) {
        _state.update { it.copy(newReleaseTagInput = value, errorMessage = null) }
    }

    fun onNewReleaseNameChanged(value: String) {
        _state.update { it.copy(newReleaseNameInput = value, errorMessage = null) }
    }

    fun createReleaseFromInputs() {
        val s = _state.value
        val owner = s.ownerInput.trim()
        val repo = s.repoInput.trim()
        val tag = s.newReleaseTagInput.trim()
        if (owner.isBlank() || repo.isBlank()) {
            _state.update { it.copy(errorMessage = "Owner and repo are required.") }
            return
        }
        if (tag.isBlank()) {
            _state.update { it.copy(errorMessage = "Release tag is required.") }
            return
        }
        if (s.isCreatingRelease) return
        _state.update { it.copy(isCreatingRelease = true, errorMessage = null) }
        viewModelScope.launch {
            when (
                val result = releaseRepository.createRelease(
                    owner = owner,
                    repo = repo,
                    request = com.painkiller.domain.github.CreateReleaseRequest(
                        tagName = tag,
                        name = s.newReleaseNameInput.trim().ifBlank { null },
                        targetCommitish = s.branchInput.trim().ifBlank { null },
                    ),
                )
            ) {
                is GithubReleaseCreateResult.Success -> _state.update {
                    it.copy(
                        isCreatingRelease = false,
                        selectedRelease = result.release,
                        newReleaseTagInput = "",
                        newReleaseNameInput = "",
                    )
                }
                is GithubReleaseCreateResult.Failure -> _state.update {
                    it.copy(isCreatingRelease = false, errorMessage = result.reason)
                }
            }
        }
    }

    fun uploadSelectedFileAsReleaseAsset() {
        val s = _state.value
        val release = s.selectedRelease
        val file = s.loadedFile
        if (release == null) {
            _state.update { it.copy(errorMessage = "Pick or create a release first.") }
            return
        }
        if (file == null) {
            _state.update { it.copy(errorMessage = "Pick a single file to upload as a release asset.") }
            return
        }
        if (s.isUploadingReleaseAsset) return
        _state.update { it.copy(isUploadingReleaseAsset = true, errorMessage = null) }
        viewModelScope.launch {
            val payload = try {
                safFileReader.createUploadPayload(file.sourceItem.sourceId, file.sizeBytes)
            } catch (e: Throwable) {
                null
            }
            if (payload == null) {
                _state.update {
                    it.copy(
                        isUploadingReleaseAsset = false,
                        errorMessage = "Could not open selected file stream for release upload.",
                    )
                }
                return@launch
            }
            val contentType = file.mimeType?.ifBlank { null } ?: "application/octet-stream"
            when (
                val result = releaseRepository.uploadReleaseAsset(
                    owner = s.ownerInput.trim(),
                    repo = s.repoInput.trim(),
                    releaseId = release.id,
                    request = UploadReleaseAssetRequest(
                        name = file.displayName,
                        contentType = contentType,
                        payload = payload,
                    ),
                )
            ) {
                is GithubReleaseAssetUploadResult.Success -> _state.update {
                    it.copy(
                        isUploadingReleaseAsset = false,
                        releaseAssetUploadMessage = "Uploaded ${result.asset.name} to ${release.tagName}.",
                    )
                }
                is GithubReleaseAssetUploadResult.Failure -> _state.update {
                    it.copy(isUploadingReleaseAsset = false, errorMessage = result.reason)
                }
            }
        }
    }

    fun loadSelectedPullRequestDetail() {
        val s = _state.value
        val selected = s.selectedPullRequest ?: return
        val owner = s.ownerInput.trim()
        val repo = s.repoInput.trim()
        if (owner.isBlank() || repo.isBlank()) return
        if (s.isLoadingPullRequestDetail) return
        _state.update { it.copy(isLoadingPullRequestDetail = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = pullRequestRepository.getPullRequest(owner, repo, selected.number)) {
                is com.painkiller.data.github.GithubPullRequestDetailResult.Success -> _state.update {
                    it.copy(
                        isLoadingPullRequestDetail = false,
                        selectedPullRequestDetail = result.pullRequest,
                    )
                }
                is com.painkiller.data.github.GithubPullRequestDetailResult.Failure -> _state.update {
                    it.copy(isLoadingPullRequestDetail = false, errorMessage = result.reason)
                }
            }
        }
    }

    fun mergeSelectedPullRequest(method: com.painkiller.data.github.PullRequestMergeMethod) {
        val s = _state.value
        val selected = s.selectedPullRequest ?: return
        val owner = s.ownerInput.trim()
        val repo = s.repoInput.trim()
        if (owner.isBlank() || repo.isBlank()) return
        if (s.isMergingPullRequest) return
        _state.update { it.copy(isMergingPullRequest = true, errorMessage = null) }
        viewModelScope.launch {
            when (
                val result = pullRequestRepository.mergePullRequest(
                    owner = owner,
                    repo = repo,
                    number = selected.number,
                    method = method,
                    expectedHeadSha = selectedPullRequestHeadSha(),
                )
            ) {
                is GithubPullRequestMergeResult.Success -> _state.update {
                    it.copy(
                        isMergingPullRequest = false,
                        pullRequestMergeMessage = result.response.message,
                    )
                }
                is GithubPullRequestMergeResult.Failure -> _state.update {
                    it.copy(
                        isMergingPullRequest = false,
                        errorMessage = result.reason,
                    )
                }
            }
        }
    }

    private fun selectedPullRequestHeadSha(): String? = _state.value.selectedPullRequestDetail?.head?.sha

    fun dismissPullRequestMessage() {
        _state.update { it.copy(pullRequestMergeMessage = null) }
    }

    fun dismissReleaseAssetMessage() {
        _state.update { it.copy(releaseAssetUploadMessage = null) }
    }

    fun onConflictPresetChanged(preset: ConflictPreset) {
        _state.update { it.copy(selectedConflictPreset = preset, conflictPlan = null, conflictMessage = null) }
    }

    fun buildConflictPreview() {
        val s = _state.value
        if (!s.hasSource) {
            _state.update { it.copy(conflictMessage = "Pick source files first. GRID did not change any files.") }
            return
        }
        viewModelScope.launch {
            val conflictSources = collectConflictSourceFiles(_state.value)
            if (conflictSources.isEmpty()) {
                _state.update {
                    it.copy(
                        conflictMessage = "No readable text files were found for collision preview. Nothing was changed.",
                        conflictPlan = null,
                    )
                }
                return@launch
            }
            val plan = ConflictPresetPlanner.buildPreviewPlan(
                files = conflictSources,
                preset = _state.value.selectedConflictPreset,
            )
            _state.update {
                it.copy(
                    conflictPlan = plan,
                    conflictMessage = plan.summary,
                )
            }
        }
    }

    fun clearConflictPreview() {
        _state.update {
            it.copy(
                conflictPlan = null,
                conflictMessage = null,
                conflictWritePlan = null,
                conflictWriteResult = null,
            )
        }
    }

    fun startConflictCardReview() {
        val s = _state.value
        if (!s.hasSource) {
            _state.update {
                it.copy(conflictReviewMessage = "No collision session available. Pick source files first.")
            }
            return
        }
        viewModelScope.launch {
            val conflictSources = collectConflictSourceFiles(_state.value)
            if (conflictSources.isEmpty()) {
                _state.update {
                    it.copy(conflictReviewMessage = "No readable text files were found for card review.")
                }
                return@launch
            }
            val session = ConflictReviewSessionBuilder.create(conflictSources)
            _state.update {
                it.copy(
                    conflictReviewSession = session,
                    conflictReviewPreview = null,
                    conflictReviewMessage = if (session.totalCollisions == 0)
                        "No collisions found for card review."
                    else
                        "Collision cards ready. Choose what should stay.",
                )
            }
        }
    }

    fun decideCurrentConflictCard(decision: ConflictDecision) {
        val session = _state.value.conflictReviewSession ?: return
        val updated = ConflictReviewSessionReducer.decide(session, decision)
        _state.update { it.copy(conflictReviewSession = updated, conflictReviewPreview = null) }
    }

    fun decideAndAdvanceConflictCard(decision: ConflictDecision) {
        val session = _state.value.conflictReviewSession ?: return
        val decided = ConflictReviewSessionReducer.decide(session, decision)
        val advanced = if (decided.currentIndex < decided.cards.lastIndex) {
            ConflictReviewSessionReducer.next(decided)
        } else {
            decided
        }
        _state.update { it.copy(conflictReviewSession = advanced, conflictReviewPreview = null) }
    }

    fun nextConflictCard() {
        val session = _state.value.conflictReviewSession ?: return
        _state.update { it.copy(conflictReviewSession = ConflictReviewSessionReducer.next(session)) }
    }

    fun previousConflictCard() {
        val session = _state.value.conflictReviewSession ?: return
        _state.update { it.copy(conflictReviewSession = ConflictReviewSessionReducer.previous(session)) }
    }

    fun buildConflictCardPreview() {
        val session = _state.value.conflictReviewSession ?: run {
            _state.update {
                it.copy(conflictReviewMessage = "No conflict session available. GRID did not change any files.")
            }
            return
        }
        val preview = ConflictReviewPreviewPlanner.buildPreview(session)
        _state.update {
            it.copy(
                conflictReviewPreview = preview,
                conflictReviewMessage = preview.summary,
            )
        }
    }

    fun closeConflictCardReview() {
        _state.update {
            it.copy(
                conflictReviewSession = null,
                conflictReviewPreview = null,
                conflictReviewMessage = null,
                conflictWritePlan = null,
                conflictWriteResult = null,
            )
        }
    }

    fun buildConflictWritePlanFromPresetPreview() {
        viewModelScope.launch {
            val sources = collectConflictSourceFiles(_state.value)
            val plan = ConflictWritePlanner.fromPresetPlan(
                previewPlan = _state.value.conflictPlan,
                sources = sources,
            )
            _state.update {
                it.copy(
                    conflictWritePlan = plan,
                    conflictWriteResult = null,
                    conflictWriteMessage = plan.summary,
                )
            }
        }
    }

    fun buildConflictWritePlanFromCardPreview() {
        viewModelScope.launch {
            val state = _state.value
            val sources = collectConflictSourceFiles(state)
            val plan = ConflictWritePlanner.fromCardPreview(
                preview = state.conflictReviewPreview,
                session = state.conflictReviewSession,
                sources = sources,
            )
            _state.update {
                it.copy(
                    conflictWritePlan = plan,
                    conflictWriteResult = null,
                    conflictWriteMessage = plan.summary,
                )
            }
        }
    }

    fun writeResolvedFiles(confirmed: Boolean) {
        val result = ConflictWriteExecutor.execute(
            plan = _state.value.conflictWritePlan,
            confirmed = confirmed,
            writer = conflictFileWriter,
        )
        _state.update {
            it.copy(
                conflictWriteResult = result,
                conflictWriteMessage = result.summary,
                conflictCommitPlan = null,
                conflictCommitResult = null,
            )
        }
    }

    fun buildConflictCommitPlan() {
        val s = _state.value
        val writeResult = s.conflictWriteResult ?: run {
            _state.update { it.copy(conflictCommitMessage = "No resolved files were written yet. Write the resolved preview first, then commit.") }
            return
        }
        if (writeResult.writtenFiles.isEmpty()) {
            _state.update { it.copy(conflictCommitMessage = "No resolved files were written yet. Write the resolved preview first, then commit.") }
            return
        }
        viewModelScope.launch {
            val sources = collectConflictSourceFiles(s).associateBy { it.path }
            val candidates = mutableListOf<ResolvedCommitCandidate>()
            for (path in writeResult.writtenFiles) {
                val src = sources[path] ?: continue
                val sourceId = src.sourceId ?: continue
                val loaded = safFileReader.read(Uri.parse(sourceId)) ?: continue
                val b64 = loaded.contentBase64 ?: continue
                val text = decodeBase64Text(b64) ?: continue
                candidates += ResolvedCommitCandidate(
                    repoPath = path,
                    sourcePath = path,
                    sourceId = sourceId,
                    contentBase64 = b64,
                    textContent = text,
                    sourceKind = src.sourceKind,
                )
            }
            val target = buildRepoTargetOrError()
            val plan = ConflictCommitPlanner.buildPlan(
                target = target,
                writtenFiles = candidates,
                blockedByWrite = writeResult.blockedFiles,
                failedByWrite = writeResult.failedFiles.map { it.path },
            )
            val baseSha = target?.let { resolveBranchSha(it.owner, it.repo, it.branch.name) }
            _state.update {
                it.copy(
                    conflictCommitPlan = plan,
                    commitMessageInput = it.commitMessageInput.ifBlank { plan.commitMessageSuggestion },
                    conflictCommitResult = null,
                    conflictCommitMessage = plan.summary,
                    conflictCommitBaseSha = baseSha,
                )
            }
        }
    }

    fun commitResolvedFiles(confirmed: Boolean) {
        val s = _state.value
        val plan = s.conflictCommitPlan ?: run {
            _state.update { it.copy(conflictCommitMessage = "Review what will be committed first.") }
            return
        }
        if (!confirmed) {
            _state.update { it.copy(conflictCommitMessage = "Commit cancelled. Nothing changed on GitHub.") }
            return
        }
        if (!plan.canCommit || plan.target == null) {
            _state.update { it.copy(conflictCommitMessage = "Commit is blocked for safety. Review blocked files first.") }
            return
        }
        val target = plan.target ?: run {
            _state.update { it.copy(conflictCommitMessage = "Commit target is missing. Review what will be committed again.") }
            return
        }
        _state.update { it.copy(isCommitting = true, humanError = null) }
        viewModelScope.launch {
            val currentSha = resolveBranchSha(target.owner, target.repo, target.branch.name)
            if (ConflictBranchFreshnessGuard.isStale(s.conflictCommitBaseSha, currentSha)) {
                _state.update {
                    it.copy(
                        isCommitting = false,
                        conflictCommitMessage = "The branch changed on GitHub after plan review. Refresh commit review before committing.",
                    )
                }
                return@launch
            }
            val message = s.commitMessageInput.ifBlank { plan.commitMessageSuggestion }
            val result = if (plan.candidates.size == 1) {
                val file = plan.candidates.first()
                when (val r = singleFileCommitRepository.commitSingleFile(
                    SingleFileCommitInput(target, file.repoPath, file.contentBase64, message),
                )) {
                    is SingleFileCommitResult.Success -> ConflictCommitResult(
                        committedFiles = listOf(r.committedPath),
                        blockedFiles = plan.blockedFiles,
                        failedReason = null,
                        commitSha = r.commitSha,
                        commitUrl = r.commitUrl,
                        didCreateCommit = true,
                    )
                    is SingleFileCommitResult.Failure -> ConflictCommitResult(
                        committedFiles = emptyList(),
                        blockedFiles = plan.blockedFiles,
                        failedReason = PainkillerErrorMapper.map(r).detail,
                        commitSha = null,
                        commitUrl = null,
                        didCreateCommit = false,
                    )
                }
            } else {
                val entries = plan.candidates.map { MultiFileCommitEntry(it.repoPath, it.contentBase64) }
                when (val r = multiFileCommitRepository.commitMultipleFiles(
                    MultiFileCommitInput(target, entries, message),
                )) {
                    is MultiFileCommitResult.Success -> ConflictCommitResult(
                        committedFiles = r.committedPaths,
                        blockedFiles = plan.blockedFiles,
                        failedReason = null,
                        commitSha = r.commitSha,
                        commitUrl = r.commitUrl,
                        didCreateCommit = true,
                    )
                    is MultiFileCommitResult.Failure -> ConflictCommitResult(
                        committedFiles = emptyList(),
                        blockedFiles = plan.blockedFiles,
                        failedReason = PainkillerErrorMapper.map(r).detail,
                        commitSha = null,
                        commitUrl = null,
                        didCreateCommit = false,
                    )
                }
            }
            _state.update {
                it.copy(
                    isCommitting = false,
                    conflictCommitResult = result,
                    conflictCommitMessage = if (result.didCreateCommit)
                        "Commit created. No push will happen automatically."
                    else
                        (result.failedReason ?: "Commit failed. Nothing changed on GitHub."),
                )
            }
        }
    }

    // ─── plan building ───────────────────────────────────────────────────────

    fun buildPlan() {
        val s = _state.value
        val target = buildRepoTargetOrError() ?: return
        if (s.isZipSource && s.hasZipUnsafeEntries) {
            _state.update {
                it.copy(errorMessage = "ZIP contains unsafe paths. Remove blocked entries and pick a safe ZIP.")
            }
            return
        }
        when {
            s.loadedFolder != null -> buildMultiFilePlan(s.loadedFolder, target)
            s.loadedFile != null -> buildFilePlan(s.loadedFile, target)
            else -> _state.update { it.copy(errorMessage = "Pick a file, folder, or ZIP first.") }
        }
    }

    private fun buildFilePlan(loaded: LoadedFile, target: RepoTarget) {
        val plannedFile = PlannedFile(
            sourceId = loaded.sourceItem.sourceId,
            sourceDisplayName = loaded.displayName,
            sourceRelativePath = loaded.displayName,
            repoPath = if (target.targetPath.normalized.isEmpty()) loaded.displayName
            else "${target.targetPath.normalized}/${loaded.displayName}",
            sizeBytes = loaded.sizeBytes,
            mimeType = loaded.mimeType,
            sizeDiagnosis = com.painkiller.domain.files.LargeFileDoctor.diagnose(loaded.sizeBytes),
        )
        val filePlan = FilePlan(
            sourceKind = SourceKind.SINGLE_FILE,
            targetPath = target.targetPath.normalized,
            includedFiles = listOf(plannedFile),
            ignoredFiles = emptyList(),
            issues = emptyList(),
            isBlockedForNormalCommit = plannedFile.sizeDiagnosis.isBlockedForNormalCommit,
        )
        applyPlan(UploadPlanBuilder.build(filePlan, target), target)
    }

    private fun buildMultiFilePlan(source: SelectedSource, target: RepoTarget) {
        when (val r = FilePlanBuilder.build(source, target.targetPath.normalized, DefaultIgnoreRules.rules)) {
            is FilePlanBuildResult.Success ->
                applyPlan(UploadPlanBuilder.build(r.plan, target), target, r.plan)
            is FilePlanBuildResult.ValidationError -> _state.update {
                it.copy(errorMessage = r.issues.firstOrNull()?.message ?: "Source has issues.")
            }
        }
    }

    private fun applyPlan(plan: UploadPlan, target: RepoTarget, filePlan: FilePlan? = null) {
        _state.update {
            it.copy(
                plan = plan,
                loadedFilePlan = filePlan,
                commitMessageInput = it.commitMessageInput.ifBlank { plan.suggestedCommitMessage },
            )
        }
        viewModelScope.launch { settingsStore.saveLastUsedTarget(target) }
    }

    // ─── confirmation / commit ───────────────────────────────────────────────

    fun confirmUpload() {
        val s = _state.value
        if (s.isCommitting) return
        val plan = s.plan ?: return
        val message = s.commitMessageInput.ifBlank { plan.suggestedCommitMessage }
        if (plan.isBlockedForCommit) {
            _state.update { it.copy(humanError = PainkillerErrorMapper.mapBlockedForCommit()) }
            return
        }
        _state.update { it.copy(isCommitting = true, humanError = null) }
        viewModelScope.launch {
            if (s.isMultiFileSource) {
                confirmMultiFileUpload(s, plan, message)
            } else {
                val loaded = s.loadedFile ?: run {
                    _state.update { it.copy(isCommitting = false) }
                    return@launch
                }
                confirmSingleFileUpload(loaded, plan, message)
            }
        }
    }

    private suspend fun confirmSingleFileUpload(loaded: LoadedFile, plan: UploadPlan, message: String) {
        val input = SingleFileCommitInput(
            target = plan.target,
            fileName = loaded.displayName,
            contentBase64 = loaded.contentBase64 ?: run {
                _state.update { it.copy(isCommitting = false, errorMessage = "Could not read selected file content.") }
                return
            },
            commitMessage = message,
        )
        handleSingleFileCommitResult(singleFileCommitRepository.commitSingleFile(input))
    }

    private suspend fun confirmMultiFileUpload(s: UploadFlowUiState, plan: UploadPlan, message: String) {
        val filePlan = s.loadedFilePlan ?: run {
            _state.update { it.copy(isCommitting = false, errorMessage = "File plan is not available.") }
            return
        }
        val entries = mutableListOf<MultiFileCommitEntry>()
        for (planned in filePlan.includedFiles) {
            if (planned.sizeDiagnosis.isBlockedForNormalCommit) continue
            val contentBase64 = when {
                s.isFolderSource -> safFileReader.read(Uri.parse(planned.sourceId))?.contentBase64
                s.isZipSource || s.isMultipleFileSource -> s.loadedMultiContent?.get(planned.sourceId)
                else -> null
            }
            if (contentBase64 == null) {
                _state.update {
                    it.copy(
                        isCommitting = false,
                        errorMessage = "Could not read file: ${planned.sourceDisplayName}. Try again.",
                    )
                }
                return
            }
            entries += MultiFileCommitEntry(repoPath = planned.repoPath, contentBase64 = contentBase64)
        }
        val input = MultiFileCommitInput(
            target = plan.target,
            entries = entries,
            commitMessage = message,
        )
        when (val result = multiFileCommitRepository.commitMultipleFiles(input)) {
            is MultiFileCommitResult.Success -> _state.update {
                it.copy(
                    isCommitting = false,
                    successCommitSha = result.commitSha,
                    successCommitUrl = result.commitUrl,
                    successCommittedPaths = result.committedPaths,
                )
            }
            is MultiFileCommitResult.Failure -> _state.update {
                it.copy(
                    isCommitting = false,
                    humanError = PainkillerErrorMapper.map(result),
                )
            }
        }
    }

    fun uploadSingleFileViaLfs() {
        val s = _state.value
        val file = s.loadedFile ?: run {
            _state.update { it.copy(errorMessage = "Pick a single file first.") }
            return
        }
        if (file.sizeBytes <= MAX_NORMAL_COMMIT_BYTES) {
            _state.update { it.copy(errorMessage = "This file is within normal commit size. LFS is optional and not required.") }
            return
        }
        val target = buildRepoTargetOrError() ?: return
        val commitMessage = s.commitMessageInput.ifBlank { "Track ${file.displayName} via Git LFS" }
        if (s.isUploadingLfs) return

        _state.update { it.copy(isUploadingLfs = true, errorMessage = null) }
        viewModelScope.launch {
            when (
                val result = lfsRepository.uploadSingleFileAndCommitPointer(
                    target = target,
                    fileName = file.displayName,
                    payload = safFileReader.createUploadPayload(file.sourceItem.sourceId, file.sizeBytes)
                        ?: run {
                            _state.update {
                                it.copy(isUploadingLfs = false, errorMessage = "Could not open selected file stream for Git LFS upload.")
                            }
                            return@launch
                        },
                    commitMessage = commitMessage,
                )
            ) {
                is GithubLfsUploadResult.Success -> _state.update {
                    it.copy(
                        isUploadingLfs = false,
                        successCommitSha = result.commitSha,
                        successCommitUrl = result.commitUrl,
                        successCommittedPaths = listOf(result.committedPath),
                    )
                }

                is GithubLfsUploadResult.Failure -> _state.update {
                    it.copy(
                        isUploadingLfs = false,
                        errorMessage = result.reason,
                    )
                }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(humanError = null, errorMessage = null) }
    }

    fun startOver() {
        _state.update {
            UploadFlowUiState(
                ownerInput = it.ownerInput,
                repoInput = it.repoInput,
                branchInput = it.branchInput,
                targetPathInput = it.targetPathInput,
                zipIssues = emptyList(),
            )
        }
    }

    private fun handleSingleFileCommitResult(result: SingleFileCommitResult) {
        when (result) {
            is SingleFileCommitResult.Success -> _state.update {
                it.copy(
                    isCommitting = false,
                    successCommitSha = result.commitSha,
                    successCommitUrl = result.commitUrl,
                    successCommittedPaths = listOf(result.committedPath),
                )
            }
            is SingleFileCommitResult.Failure -> _state.update {
                it.copy(
                    isCommitting = false,
                    humanError = PainkillerErrorMapper.map(result),
                )
            }
        }
    }

    private fun buildRepoTargetOrError(): RepoTarget? {
        val s = _state.value
        val owner = s.ownerInput.trim()
        val repo = s.repoInput.trim()
        val branch = s.branchInput.trim()
        if (owner.isEmpty() || repo.isEmpty() || branch.isEmpty()) {
            _state.update { it.copy(errorMessage = "Owner, repo, and branch are required.") }
            return null
        }
        val pathResult = TargetPath.fromRaw(s.targetPathInput)
        val path = when (pathResult) {
            is TargetPathValidationResult.Valid -> pathResult.targetPath
            is TargetPathValidationResult.Invalid -> {
                _state.update { it.copy(errorMessage = pathResult.message) }
                return null
            }
        }
        return RepoTarget(
            owner = owner,
            repo = repo,
            branch = BranchTarget(name = branch),
            targetPath = path,
        )
    }

    private suspend fun collectConflictSourceFiles(state: UploadFlowUiState): List<ConflictSourceFile> {
        val single = state.loadedFile
        if (single != null) {
            val content = decodeBase64Text(single.contentBase64) ?: return emptyList()
            return listOf(
                ConflictSourceFile(
                    path = single.displayName,
                    content = content,
                    sourceId = single.sourceItem.sourceId,
                    sourceKind = SourceKind.SINGLE_FILE,
                    writableBySaf = true,
                ),
            )
        }

        val source = state.loadedFolder ?: return emptyList()
        return when {
            state.isMultipleFileSource || state.isZipSource -> {
                source.items.mapNotNull { item ->
                    val encoded = state.loadedMultiContent?.get(item.sourceId) ?: return@mapNotNull null
                    val decoded = decodeBase64Text(encoded) ?: return@mapNotNull null
                    ConflictSourceFile(
                        path = item.relativePath ?: item.displayName,
                        content = decoded,
                        sourceId = if (state.isZipSource) null else item.sourceId,
                        sourceKind = source.kind,
                        writableBySaf = !state.isZipSource,
                    )
                }
            }

            state.isFolderSource -> {
                source.items.mapNotNull { item ->
                    val read = safFileReader.read(Uri.parse(item.sourceId)) ?: return@mapNotNull null
                    val decoded = decodeBase64Text(read.contentBase64) ?: return@mapNotNull null
                    ConflictSourceFile(
                        path = item.relativePath ?: item.displayName,
                        content = decoded,
                        sourceId = item.sourceId,
                        sourceKind = SourceKind.FOLDER,
                        writableBySaf = true,
                    )
                }
            }

            else -> emptyList()
        }
    }

    private fun decodeBase64Text(encoded: String?): String? {
        if (encoded.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            bytes.toString(StandardCharsets.UTF_8)
        } catch (_: Throwable) {
            null
        }
    }

    private suspend fun resolveBranchSha(owner: String, repo: String, branch: String): String? {
        return when (val result = repoBranchRepository.listBranches(owner, repo)) {
            is GithubBranchListResult.Success -> result.branches.firstOrNull { it.name == branch }?.commit?.sha
            is GithubBranchListResult.Failure -> null
        }
    }

    companion object {
        private const val MAX_NORMAL_COMMIT_BYTES = 100L * 1024L * 1024L

        fun factory(
            safFileReader: SafFileReader,
            repoBranchRepository: GithubRepoBranchRepository,
            releaseRepository: GithubReleaseRepository,
            singleFileCommitRepository: SingleFileCommitRepository,
            pullRequestRepository: GithubPullRequestRepository,
            multiFileCommitRepository: MultiFileCommitRepository,
            lfsRepository: GithubLfsRepository,
            settingsStore: RepoTargetSettingsStore,
            conflictFileWriter: com.painkiller.domain.conflict.ConflictFileWriter,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                    UploadFlowViewModel(
                        safFileReader = safFileReader,
                        repoBranchRepository = repoBranchRepository,
                        pullRequestRepository = pullRequestRepository,
                        releaseRepository = releaseRepository,
                        singleFileCommitRepository = singleFileCommitRepository,
                        multiFileCommitRepository = multiFileCommitRepository,
                        lfsRepository = lfsRepository,
                        settingsStore = settingsStore,
                        conflictFileWriter = conflictFileWriter,
                    ) as T
            }
    }
}

data class UploadFlowUiState(
    val loadedFile: LoadedFile? = null,
    val loadedFolder: SelectedSource? = null,
    val loadedMultiContent: Map<String, String>? = null,
    val loadedFilePlan: FilePlan? = null,
    val zipIssues: List<ZipIntakeIssue> = emptyList(),
    val ownerInput: String = "",
    val repoInput: String = "",
    val branchInput: String = "",
    val targetPathInput: String = "",
    val commitMessageInput: String = "",
    val repositories: List<GithubRepositorySummary> = emptyList(),
    val branches: List<GithubBranchSummary> = emptyList(),
    val pullRequests: List<GithubPullRequestSummary> = emptyList(),
    val selectedPullRequest: GithubPullRequestSummary? = null,
    val selectedPullRequestDetail: com.painkiller.domain.github.GithubPullRequestDetail? = null,
    val releases: List<GithubReleaseSummary> = emptyList(),
    val selectedRelease: GithubReleaseSummary? = null,
    val isLoadingRepos: Boolean = false,
    val isLoadingBranches: Boolean = false,
    val isLoadingPullRequests: Boolean = false,
    val isLoadingPullRequestDetail: Boolean = false,
    val isLoadingReleases: Boolean = false,
    val isCreatingRelease: Boolean = false,
    val isUploadingReleaseAsset: Boolean = false,
    val isMergingPullRequest: Boolean = false,
    val pullRequestMergeMessage: String? = null,
    val releaseAssetUploadMessage: String? = null,
    val selectedConflictPreset: ConflictPreset = ConflictPreset.KEEP_CURRENT,
    val conflictPlan: ConflictResolutionPlan? = null,
    val conflictMessage: String? = null,
    val conflictReviewSession: ConflictReviewSession? = null,
    val conflictReviewPreview: ConflictReviewPreview? = null,
    val conflictReviewMessage: String? = null,
    val conflictWritePlan: ConflictWritePlan? = null,
    val conflictWriteResult: ConflictWriteResult? = null,
    val conflictWriteMessage: String? = null,
    val conflictCommitPlan: ConflictCommitPlan? = null,
    val conflictCommitBaseSha: String? = null,
    val conflictCommitResult: ConflictCommitResult? = null,
    val conflictCommitMessage: String? = null,
    val newReleaseTagInput: String = "",
    val newReleaseNameInput: String = "",
    val plan: UploadPlan? = null,
    val isCommitting: Boolean = false,
    val isUploadingLfs: Boolean = false,
    val errorMessage: String? = null,
    val humanError: HumanReadableError? = null,
    val successCommitSha: String? = null,
    val successCommitUrl: String? = null,
    val successCommittedPaths: List<String>? = null,
) {
    val hasSucceeded: Boolean get() = successCommitSha != null
    val isFolderSource: Boolean get() = loadedFolder?.kind == SourceKind.FOLDER
    val isZipSource: Boolean get() = loadedFolder?.kind == SourceKind.ZIP
    val isMultipleFileSource: Boolean get() = loadedFolder?.kind == SourceKind.MULTIPLE_FILES
    val isMultiFileSource: Boolean get() = loadedFolder != null
    val zipCollisionCount: Int get() = zipIssues.count { it.code == ZipIntakeIssueCode.COLLISION }
    val hasZipUnsafeEntries: Boolean get() = zipIssues.any { it.code == ZipIntakeIssueCode.UNSAFE_PATH }
    val hasSource: Boolean get() = loadedFile != null || loadedFolder != null
    val sourceKind: SourceKind?
        get() = when {
            loadedFile != null -> SourceKind.SINGLE_FILE
            loadedFolder != null -> loadedFolder.kind
            else -> null
        }
    val isSingleLargeFileEligibleForLfs: Boolean get() = loadedFile?.sizeBytes?.let { it > (100L * 1024L * 1024L) } == true
    val hasAnyLargeFilesInPlan: Boolean
        get() = plan?.let { current ->
            current.blockedEntries.isNotEmpty() ||
                current.warningEntries.any { entry -> entry.sizeBytes?.let { it > (100L * 1024L * 1024L) } == true }
        } == true
    val routingDecision: LargeFileRoutingDecision?
        get() {
            val kind = sourceKind ?: return null
            val currentPlan = plan ?: return null
            return LargeFileRoutingDecider.decide(
                LargeFileRoutingInput(
                    sourceKind = kind,
                    hasBlockedEntries = currentPlan.isBlockedForCommit,
                    hasUnsafeZipEntries = isZipSource && hasZipUnsafeEntries,
                    hasSingleLargeFile = kind == SourceKind.SINGLE_FILE && isSingleLargeFileEligibleForLfs,
                    hasAnyLargeFiles = hasAnyLargeFilesInPlan,
                    hasReleaseSelected = selectedRelease != null,
                ),
            )
        }
    val retryHint: RecoveryHint? get() = humanError?.recoveryHint
    val retrySafety: RetrySafety? get() = humanError?.retrySafety
}
