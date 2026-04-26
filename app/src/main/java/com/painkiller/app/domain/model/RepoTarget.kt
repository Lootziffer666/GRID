package com.painkiller.app.domain.model

/**
 * Painkiller upload target — the (repo, branch, path) triple.
 *
 * Gate 0 declares the type so other layers can refer to it. Validation
 * and DataStore-backed presets land in Gate 4. The domain stays
 * deliberately tiny here.
 */
data class RepoTarget(
    val ownerLogin: String,
    val repoName: String,
)

data class BranchTarget(
    val name: String,
    val isProtected: Boolean = false,
)

/**
 * Repository-relative path. Always normalized to GitHub's `/`-separated
 * form. Use `PathValidation.normalizeRepoPath` to construct safely.
 */
@JvmInline
value class TargetPath(val value: String)
