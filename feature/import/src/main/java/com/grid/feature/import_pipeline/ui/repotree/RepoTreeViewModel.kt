package com.grid.feature.import_pipeline.ui.repotree

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.grid.feature.import_pipeline.data.files.SafFileReader
import com.grid.feature.import_pipeline.data.files.SafZipReader
import com.grid.feature.import_pipeline.data.github.GithubBranchListResult
import com.grid.feature.import_pipeline.data.github.GithubRepoBranchRepository
import com.grid.feature.import_pipeline.data.github.GithubRepoListResult
import com.grid.feature.import_pipeline.data.github.RepoTreeLoadResult
import com.grid.feature.import_pipeline.data.github.RepoTreeRepository
import com.painkiller.domain.github.GithubBranchSummary
import com.painkiller.domain.github.GithubRepositorySummary
import com.painkiller.domain.github.PendingChange
import com.painkiller.domain.github.PendingChangeType
import com.painkiller.domain.github.RepoTreeResult
import com.painkiller.domain.github.TreeEntry
import com.painkiller.domain.path.PathValidation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the repo file manager screen.
 *
 * Manages:
 *   - repo/branch selection (via [GithubRepoBranchRepository])
 *   - recursive tree loading + breadcrumb navigation (via [RepoTreeRepository])
 *   - pending change accumulation (move, rename, delete, create folder, upload)
 *   - atomic commit of all pending changes
 */
class RepoTreeViewModel(
    private val repoTreeRepository: RepoTreeRepository,
    private val repoBranchRepository: GithubRepoBranchRepository,
    private val safFileReader: SafFileReader,
    private val safZipReader: SafZipReader,
) : ViewModel() {

    private val _state = MutableStateFlow(RepoTreeUiState())
    val state: StateFlow<RepoTreeUiState> = _state.asStateFlow()

    // ── Repo / branch selection ──────────────────────────────────────────────

    fun onOwnerChanged(value: String) {
        _state.update { it.copy(ownerInput = value, errorMessage = null) }
    }

    fun onRepoChanged(value: String) {
        _state.update { it.copy(repoInput = value, errorMessage = null) }
    }

    fun onBranchChanged(value: String) {
        _state.update { it.copy(branchInput = value, errorMessage = null) }
    }

    fun loadRepositoryList() {
        if (_state.value.isLoadingRepos) return
        _state.update { it.copy(isLoadingRepos = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repoBranchRepository.listRepositories()) {
                is GithubRepoListResult.Success -> _state.update {
                    it.copy(repositories = result.repositories, isLoadingRepos = false)
                }

                is GithubRepoListResult.Failure -> _state.update {
                    it.copy(isLoadingRepos = false, errorMessage = result.reason)
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
                treeEntries = emptyList(),
                currentPath = "",
                pendingChanges = emptyList(),
                commitResult = null,
            )
        }
    }

    fun loadBranchList() {
        val owner = _state.value.ownerInput.trim()
        val repo = _state.value.repoInput.trim()
        if (owner.isBlank() || repo.isBlank()) return
        if (_state.value.isLoadingBranches) return
        _state.update { it.copy(isLoadingBranches = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repoBranchRepository.listBranches(owner, repo)) {
                is GithubBranchListResult.Success -> _state.update {
                    it.copy(branches = result.branches, isLoadingBranches = false)
                }

                is GithubBranchListResult.Failure -> _state.update {
                    it.copy(isLoadingBranches = false, errorMessage = result.reason)
                }
            }
        }
    }

    fun selectBranch(summary: GithubBranchSummary) {
        _state.update {
            it.copy(
                branchInput = summary.name,
                treeEntries = emptyList(),
                currentPath = "",
                commitResult = null,
            )
        }
    }

    // ── Tree loading ─────────────────────────────────────────────────────────

    fun loadTree() {
        val owner = _state.value.ownerInput.trim()
        val repo = _state.value.repoInput.trim()
        val branch = _state.value.branchInput.trim()
        if (owner.isBlank() || repo.isBlank() || branch.isBlank()) {
            _state.update { it.copy(errorMessage = "Owner, repo, and branch are required.") }
            return
        }
        if (_state.value.isLoadingTree) return
        _state.update { it.copy(isLoadingTree = true, errorMessage = null, commitResult = null) }
        viewModelScope.launch {
            when (val result = repoTreeRepository.loadTree(owner, repo, branch)) {
                is RepoTreeLoadResult.Success -> _state.update {
                    it.copy(
                        treeEntries = result.entries,
                        treeTruncated = result.truncated,
                        isLoadingTree = false,
                        currentPath = "",
                    )
                }

                is RepoTreeLoadResult.AuthError -> _state.update {
                    it.copy(isLoadingTree = false, errorMessage = result.message)
                }

                is RepoTreeLoadResult.NotFound -> _state.update {
                    it.copy(isLoadingTree = false, errorMessage = result.message)
                }

                is RepoTreeLoadResult.NetworkError -> _state.update {
                    it.copy(isLoadingTree = false, errorMessage = result.message)
                }
            }
        }
    }

    // ── Breadcrumb navigation ────────────────────────────────────────────────

    fun navigateToFolder(path: String) {
        _state.update { it.copy(currentPath = path) }
    }

    fun navigateUp() {
        val current = _state.value.currentPath
        if (current.isEmpty()) return
        val parent = current.substringBeforeLast('/', "")
        _state.update { it.copy(currentPath = parent) }
    }

    // ── Pending changes ──────────────────────────────────────────────────────

    fun addPendingChange(change: PendingChange) {
        _state.update {
            it.copy(pendingChanges = it.pendingChanges + change, commitResult = null)
        }
    }

    fun removePendingChange(index: Int) {
        _state.update {
            val updated = it.pendingChanges.toMutableList()
            if (index in updated.indices) updated.removeAt(index)
            it.copy(pendingChanges = updated)
        }
    }

    fun addNewFolder(folderName: String) {
        if (!PathValidation.isSafeRepoPath(folderName) || folderName.isBlank()) {
            _state.update { it.copy(errorMessage = "Invalid folder name: '$folderName'.") }
            return
        }
        val current = _state.value.currentPath
        val fullPath = if (current.isEmpty()) folderName else "$current/$folderName"
        addPendingChange(
            PendingChange(
                type = PendingChangeType.CREATE_FOLDER,
                sourcePath = null,
                targetPath = fullPath,
            ),
        )
    }

    fun deleteFile(path: String) {
        addPendingChange(
            PendingChange(
                type = PendingChangeType.DELETE,
                sourcePath = path,
                targetPath = null,
            ),
        )
    }

    fun renameFile(oldPath: String, newName: String) {
        if (!PathValidation.isSafeRepoPath(newName) || newName.isBlank()) {
            _state.update { it.copy(errorMessage = "Invalid name: '$newName'.") }
            return
        }
        val parentDir = oldPath.substringBeforeLast('/', "")
        val newPath = if (parentDir.isEmpty()) newName else "$parentDir/$newName"
        addPendingChange(
            PendingChange(
                type = PendingChangeType.RENAME,
                sourcePath = oldPath,
                targetPath = newPath,
            ),
        )
    }

    fun moveFile(oldPath: String, newFolder: String) {
        if (!PathValidation.isSafeRepoPath(newFolder)) {
            _state.update { it.copy(errorMessage = "Invalid target folder: '$newFolder'.") }
            return
        }
        val fileName = oldPath.substringAfterLast('/')
        val newPath = if (newFolder.isEmpty()) fileName else "$newFolder/$fileName"
        addPendingChange(
            PendingChange(
                type = PendingChangeType.MOVE,
                sourcePath = oldPath,
                targetPath = newPath,
            ),
        )
    }

    fun onFileUriPicked(uri: Uri) {
        viewModelScope.launch {
            val loaded = safFileReader.read(uri) ?: run {
                _state.update { it.copy(errorMessage = "Could not read the selected file.") }
                return@launch
            }
            val current = _state.value.currentPath
            val targetPath = if (current.isEmpty()) loaded.displayName
            else "$current/${loaded.displayName}"
            addPendingChange(
                PendingChange(
                    type = PendingChangeType.UPLOAD,
                    sourcePath = null,
                    targetPath = targetPath,
                    newContentBase64 = loaded.contentBase64,
                ),
            )
        }
    }

    fun onZipUriPicked(uri: Uri) {
        viewModelScope.launch {
            val result = safZipReader.read(uri)
            val current = _state.value.currentPath
            result.source.items.forEach { item ->
                val content = result.contentByRelativePath[item.relativePath] ?: return@forEach
                val targetPath = if (current.isEmpty()) item.relativePath
                else "$current/${item.relativePath}"
                addPendingChange(
                    PendingChange(
                        type = PendingChangeType.UPLOAD,
                        sourcePath = null,
                        targetPath = targetPath,
                        newContentBase64 = content,
                    ),
                )
            }
        }
    }

    fun clearPendingChanges() {
        _state.update { it.copy(pendingChanges = emptyList()) }
    }

    // ── Commit ───────────────────────────────────────────────────────────────

    fun commitChanges() {
        val s = _state.value
        val owner = s.ownerInput.trim()
        val repo = s.repoInput.trim()
        val branch = s.branchInput.trim()
        if (owner.isBlank() || repo.isBlank() || branch.isBlank()) {
            _state.update { it.copy(errorMessage = "Owner, repo, and branch are required.") }
            return
        }
        if (s.pendingChanges.isEmpty()) {
            _state.update { it.copy(errorMessage = "No pending changes to commit.") }
            return
        }
        if (s.isCommitting) return
        _state.update { it.copy(isCommitting = true, errorMessage = null, commitResult = null) }
        viewModelScope.launch {
            val result = repoTreeRepository.commitChanges(
                owner = owner,
                repo = repo,
                branch = branch,
                changes = s.pendingChanges,
            )
            when (result) {
                is RepoTreeResult.Success -> {
                    _state.update {
                        it.copy(
                            isCommitting = false,
                            commitResult = CommitResultInfo(
                                sha = result.commitSha,
                                url = result.commitUrl,
                                summary = result.summary,
                            ),
                            pendingChanges = emptyList(),
                        )
                    }
                    loadTree()
                }

                is RepoTreeResult.Failure -> _state.update {
                    it.copy(isCommitting = false, errorMessage = result.message)
                }
            }
        }
    }

    fun dismissCommitResult() {
        _state.update { it.copy(commitResult = null) }
    }

    companion object {
        fun factory(
            repoTreeRepository: RepoTreeRepository,
            repoBranchRepository: GithubRepoBranchRepository,
            safFileReader: SafFileReader,
            safZipReader: SafZipReader,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                    RepoTreeViewModel(
                        repoTreeRepository = repoTreeRepository,
                        repoBranchRepository = repoBranchRepository,
                        safFileReader = safFileReader,
                        safZipReader = safZipReader,
                    ) as T
            }
    }
}

data class CommitResultInfo(
    val sha: String,
    val url: String?,
    val summary: String,
)

data class RepoTreeUiState(
    val ownerInput: String = "",
    val repoInput: String = "",
    val branchInput: String = "",
    val repositories: List<GithubRepositorySummary> = emptyList(),
    val branches: List<GithubBranchSummary> = emptyList(),
    val treeEntries: List<TreeEntry> = emptyList(),
    val treeTruncated: Boolean = false,
    val currentPath: String = "",
    val pendingChanges: List<PendingChange> = emptyList(),
    val isLoadingRepos: Boolean = false,
    val isLoadingBranches: Boolean = false,
    val isLoadingTree: Boolean = false,
    val isCommitting: Boolean = false,
    val errorMessage: String? = null,
    val commitResult: CommitResultInfo? = null,
) {
    val hasTree: Boolean get() = treeEntries.isNotEmpty()

    /** Entries visible at the current directory level. */
    val currentLevelEntries: List<TreeEntry>
        get() {
            val prefix = if (currentPath.isEmpty()) "" else "$currentPath/"
            return treeEntries.filter { entry ->
                if (!entry.path.startsWith(prefix) && currentPath.isNotEmpty()) return@filter false
                if (currentPath.isEmpty() && entry.path.contains('/')) return@filter false
                if (currentPath.isNotEmpty()) {
                    val relative = entry.path.removePrefix(prefix)
                    !relative.contains('/')
                } else {
                    true
                }
            }
        }

    /** Breadcrumb path segments for navigation. */
    val breadcrumbs: List<String>
        get() = if (currentPath.isEmpty()) emptyList()
        else currentPath.split('/')

    val hasPendingChanges: Boolean get() = pendingChanges.isNotEmpty()
}
