package com.painkiller.ui.flow

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.painkiller.data.files.LoadedFile
import com.painkiller.data.files.SafFileReader
import com.painkiller.data.files.SafZipReader
import com.painkiller.data.github.GithubBranchListResult
import com.painkiller.data.github.GithubPullRequestListResult
import com.painkiller.data.github.GithubPullRequestMergeResult
import com.painkiller.data.github.GithubPullRequestRepository
import com.painkiller.data.github.GithubRepoBranchRepository
import com.painkiller.data.github.GithubRepoListResult
import com.painkiller.data.github.MultiFileCommitRepository
import com.painkiller.data.github.SingleFileCommitRepository
import com.painkiller.data.settings.RepoTargetSettingsStore
import com.painkiller.domain.error.HumanReadableError
import com.painkiller.domain.error.PainkillerErrorMapper
import com.painkiller.domain.error.RecoveryHint
import com.painkiller.domain.error.RetrySafety
import com.painkiller.domain.files.DefaultIgnoreRules
import com.painkiller.domain.files.FilePlan
import com.painkiller.domain.files.FilePlanBuildResult
import com.painkiller.domain.files.FilePlanBuilder
import com.painkiller.domain.files.PlannedFile
import com.painkiller.domain.files.SelectedSource
import com.painkiller.domain.files.SourceKind
import com.painkiller.domain.github.GithubBranchSummary
import com.painkiller.domain.github.GithubPullRequestSummary
import com.painkiller.domain.github.GithubRepositorySummary
import com.painkiller.domain.github.MultiFileCommitEntry
import com.painkiller.domain.github.MultiFileCommitInput
import com.painkiller.domain.github.MultiFileCommitResult
import com.painkiller.domain.github.SingleFileCommitInput
import com.painkiller.domain.github.SingleFileCommitResult
import com.painkiller.domain.target.BranchTarget
import com.painkiller.domain.target.RepoTarget
import com.painkiller.domain.target.TargetPath
import com.painkiller.domain.target.TargetPathValidationResult
import com.painkiller.domain.upload.UploadPlan
import com.painkiller.domain.upload.UploadPlanBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private val singleFileCommitRepository: SingleFileCommitRepository,
    private val multiFileCommitRepository: MultiFileCommitRepository,
    private val settingsStore: RepoTargetSettingsStore,
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
                file.sourceItem.sourceId to file.contentBase64
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
            )
        }
        viewModelScope.launch {
            val loaded = safFileReader.read(uri)
            if (loaded == null) {
                _state.update {
                    it.copy(errorMessage = "Could not read the selected file. The link may have expired.")
                }
                return@launch
            }
            if (loaded.sizeBytes > MAX_NORMAL_COMMIT_BYTES) {
                _state.update {
                    it.copy(
                        loadedFile = loaded,
                        errorMessage = "This file is too large for a normal Git commit (>100 MiB). " +
                            "Painkiller blocked the upload.",
                    )
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

    // ─── plan building ───────────────────────────────────────────────────────

    fun buildPlan() {
        val s = _state.value
        val target = buildRepoTargetOrError() ?: return
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
            contentBase64 = loaded.contentBase64,
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

    companion object {
        private const val MAX_NORMAL_COMMIT_BYTES = 100L * 1024L * 1024L

        fun factory(
            safFileReader: SafFileReader,
            repoBranchRepository: GithubRepoBranchRepository,
            singleFileCommitRepository: SingleFileCommitRepository,
            pullRequestRepository: GithubPullRequestRepository,
            multiFileCommitRepository: MultiFileCommitRepository,
            settingsStore: RepoTargetSettingsStore,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                    UploadFlowViewModel(
                        safFileReader = safFileReader,
                        repoBranchRepository = repoBranchRepository,
                        pullRequestRepository = pullRequestRepository,
                        singleFileCommitRepository = singleFileCommitRepository,
                        multiFileCommitRepository = multiFileCommitRepository,
                        settingsStore = settingsStore,
                    ) as T
            }
    }
}

data class UploadFlowUiState(
    val loadedFile: LoadedFile? = null,
    val loadedFolder: SelectedSource? = null,
    val loadedMultiContent: Map<String, String>? = null,
    val loadedFilePlan: FilePlan? = null,
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
    val isLoadingRepos: Boolean = false,
    val isLoadingBranches: Boolean = false,
    val isLoadingPullRequests: Boolean = false,
    val isLoadingPullRequestDetail: Boolean = false,
    val isMergingPullRequest: Boolean = false,
    val pullRequestMergeMessage: String? = null,
    val plan: UploadPlan? = null,
    val isCommitting: Boolean = false,
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
    val hasSource: Boolean get() = loadedFile != null || loadedFolder != null
    val retryHint: RecoveryHint? get() = humanError?.recoveryHint
    val retrySafety: RetrySafety? get() = humanError?.retrySafety
}
