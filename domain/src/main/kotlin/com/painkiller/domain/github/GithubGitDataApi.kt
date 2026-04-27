package com.painkiller.domain.github

// Gate 0 spike interface for the Git Data API surface Painkiller will use
// in later gates. No implementation. No network. Each method maps to one
// GitHub REST endpoint and is intentionally narrow.
//
// Reference (for later gates):
//   GET    /repos/{owner}/{repo}/git/ref/{ref}
//   GET    /repos/{owner}/{repo}/git/commits/{sha}
//   POST   /repos/{owner}/{repo}/git/blobs
//   POST   /repos/{owner}/{repo}/git/trees
//   POST   /repos/{owner}/{repo}/git/commits
//   PATCH  /repos/{owner}/{repo}/git/refs/{ref}
interface GithubGitDataApi {

    suspend fun getRef(owner: String, repo: String, ref: String): GitRef

    suspend fun getCommit(owner: String, repo: String, commitSha: String): GitCommit

    suspend fun createBlob(
        owner: String,
        repo: String,
        request: CreateBlobRequest,
    ): CreateBlobResponse

    suspend fun createTree(
        owner: String,
        repo: String,
        request: CreateTreeRequest,
    ): CreateTreeResponse

    suspend fun createCommit(
        owner: String,
        repo: String,
        request: CreateCommitRequest,
    ): CreateCommitResponse

    /**
     * Updates a branch ref. The branch must currently point at [expectedSha]
     * for this to be safe. Implementations must NOT use force=true unless
     * an explicit, audited, future feature requires it. Painkiller's contract
     * is: a branch never points to an incomplete state.
     */
    suspend fun updateRef(
        owner: String,
        repo: String,
        ref: String,
        request: UpdateRefRequest,
        expectedSha: String,
    ): GitRef
}

interface GithubRepositoryApi {
    suspend fun listRepositories(): List<GithubRepositorySummary>
    suspend fun listBranches(owner: String, repo: String): List<GithubBranchSummary>
}

interface GithubPullRequestApi {
    suspend fun listPullRequests(
        owner: String,
        repo: String,
        state: String = "open",
    ): List<GithubPullRequestSummary>
}
