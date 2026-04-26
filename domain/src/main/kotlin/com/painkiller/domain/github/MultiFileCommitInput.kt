package com.painkiller.domain.github

import com.painkiller.domain.target.RepoTarget

/**
 * Gate 7 input for a multi-file atomic commit.
 *
 * [target] identifies the repository, branch, and default target folder.
 * [entries] is the list of files to commit. May be empty; the orchestrator
 * will then inject a `.gitkeep` at the target path so the folder is visible
 * on GitHub. All entry paths must be pre-validated and must not be blocked
 * by Gate 2 size diagnosis.
 * [commitMessage] must be non-blank.
 */
data class MultiFileCommitInput(
    val target: RepoTarget,
    val entries: List<MultiFileCommitEntry>,
    val commitMessage: String,
)
