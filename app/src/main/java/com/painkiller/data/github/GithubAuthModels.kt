package com.painkiller.data.github

import com.painkiller.domain.github.GithubBranchSummary
import com.painkiller.domain.github.GithubRepositorySummary

sealed interface GithubAuthState {
    data object Unauthenticated : GithubAuthState
    data class Authenticated(val tokenPreview: String) : GithubAuthState
}

sealed interface GithubAuthResult {
    data class Success(val state: GithubAuthState.Authenticated) : GithubAuthResult
    data class Failure(val reason: String) : GithubAuthResult
}

sealed interface GithubRepoListResult {
    data class Success(val repositories: List<GithubRepositorySummary>) : GithubRepoListResult
    data class Failure(val reason: String) : GithubRepoListResult
}

sealed interface GithubBranchListResult {
    data class Success(val branches: List<GithubBranchSummary>) : GithubBranchListResult
    data class Failure(val reason: String) : GithubBranchListResult
}
