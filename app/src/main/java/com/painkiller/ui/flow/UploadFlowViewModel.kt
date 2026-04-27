package com.painkiller.ui.flow

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.painkiller.data.files.LoadedFile
import com.painkiller.data.files.SafFileReader
import com.painkiller.data.github.GithubBranchListResult
import com.painkiller.data.github.GithubRepoBranchRepository
import com.painkiller.data.github.GithubRepoListResult
import com.painkiller.data.github.SingleFileCommitRepository
import com.painkiller.data.settings.RepoTargetSettingsStore
import com.painkiller.domain.error.HumanReadableError
import com.painkiller.domain.error.PainkillerErrorMapper
import com.painkiller.domain.error.RecoveryHint
import com.painkiller.domain.error.RetrySafety
import com.painkiller.domain.files.FilePlan
import com.painkiller.domain.files.PlannedFile
import com.painkiller.domain.files.SourceKind
import com.painkiller.domain.github.GithubBranchSummary
import com.painkiller.domain.github.GithubRepositorySummary
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
 * Owns the end-to-end single-file upload flow:
 *
 *   1. user picks a file via SAF — VM reads it via [SafFileReader]
 *   2. user picks owner/repo and branch from listings retrieved via
 *      [GithubRepoBranchRepository], plus a target path
 *   3. VM builds an [UploadPlan] via Gate 5 [UploadPlanBuilder]
 *   4. user confirms — VM submits via Gate 6 [SingleFileCommitRepository]
 *   5. VM emits success (with commit URL) or a [HumanReadableError]
 *
 * Multi-file / folder / ZIP flows are deferred to a future UI gate; the
 * domain orchestration for those already exists in
 * [com.painkiller.data.github.MultiFileCommitRepository].
 */
class UploadFlowViewModel(
    private val safFileReader: SafFileReader,
    private val repoBranchRepository: GithubRepoBranchRepository,
    private val singleFileCommitRepository: SingleFileCommitRepository,
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

    fun onSourceUriPicked(uri: Uri) {
        _state.update { it.copy(loadedFile = null, errorMessage = null, plan = null) }
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
        _state.update { it.copy(loadedFile = null, plan = null) }
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

    // ─── plan building ───────────────────────────────────────────────────────

    fun buildPlan() {
        val s = _state.value
        val loaded = s.loadedFile ?: run {
            _state.update { it.copy(errorMessage = "Pick a file first.") }
            return
        }
        val target = buildRepoTargetOrError() ?: return
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
        val plan = UploadPlanBuilder.build(filePlan, target)
        _state.update {
            it.copy(
                plan = plan,
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
        val loaded = s.loadedFile ?: return
        val message = s.commitMessageInput.ifBlank { plan.suggestedCommitMessage }
        if (plan.isBlockedForCommit) {
            val err = PainkillerErrorMapper.mapBlockedForCommit()
            _state.update { it.copy(humanError = err) }
            return
        }
        _state.update { it.copy(isCommitting = true, humanError = null) }
        viewModelScope.launch {
            val input = SingleFileCommitInput(
                target = plan.target,
                fileName = loaded.displayName,
                contentBase64 = loaded.contentBase64,
                commitMessage = message,
            )
            val result = singleFileCommitRepository.commitSingleFile(input)
            handleCommitResult(result)
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

    private fun handleCommitResult(result: SingleFileCommitResult) {
        when (result) {
            is SingleFileCommitResult.Success -> _state.update {
                it.copy(
                    isCommitting = false,
                    successCommitSha = result.commitSha,
                    successCommitUrl = result.commitUrl,
                    successCommittedPath = result.committedPath,
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
            settingsStore: RepoTargetSettingsStore,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                    UploadFlowViewModel(
                        safFileReader = safFileReader,
                        repoBranchRepository = repoBranchRepository,
                        singleFileCommitRepository = singleFileCommitRepository,
                        settingsStore = settingsStore,
                    ) as T
            }
    }
}

data class UploadFlowUiState(
    val loadedFile: LoadedFile? = null,
    val ownerInput: String = "",
    val repoInput: String = "",
    val branchInput: String = "",
    val targetPathInput: String = "",
    val commitMessageInput: String = "",
    val repositories: List<GithubRepositorySummary> = emptyList(),
    val branches: List<GithubBranchSummary> = emptyList(),
    val isLoadingRepos: Boolean = false,
    val isLoadingBranches: Boolean = false,
    val plan: UploadPlan? = null,
    val isCommitting: Boolean = false,
    val errorMessage: String? = null,
    val humanError: HumanReadableError? = null,
    val successCommitSha: String? = null,
    val successCommitUrl: String? = null,
    val successCommittedPath: String? = null,
) {
    val hasSucceeded: Boolean get() = successCommitSha != null
    val retryHint: RecoveryHint? get() = humanError?.recoveryHint
    val retrySafety: RetrySafety? get() = humanError?.retrySafety
}
