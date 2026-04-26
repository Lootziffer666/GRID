package com.painkiller.app.data.github

/**
 * Painkiller's interface to the GitHub Git Data API.
 *
 * Gate 0 ships only the contract. A real implementation backed by
 * Retrofit/OkHttp lands in Gate 6, where the safety contract
 * (no force pushes, ref update only after commit creation, expected-SHA
 * guards) is enforced.
 *
 * Method order matches the upload flow defined in `instructions.md`
 * § "Confirmed GitHub Commit Strategy":
 * 1. read current ref
 * 2. read base commit/tree as needed
 * 3. create blobs
 * 4. create tree
 * 5. create commit
 * 6. update ref — only after the complete commit exists
 */
interface GitHubGitDataApi {

    suspend fun getRef(
        owner: String,
        repo: String,
        ref: String,
    ): GitHubRefResponse

    suspend fun createBlob(
        owner: String,
        repo: String,
        request: GitHubBlobRequest,
    ): GitHubBlobResponse

    suspend fun createTree(
        owner: String,
        repo: String,
        request: GitHubCreateTreeRequest,
    ): GitHubCreateTreeResponse

    suspend fun createCommit(
        owner: String,
        repo: String,
        request: GitHubCreateCommitRequest,
    ): GitHubCreateCommitResponse

    /**
     * Updates a branch ref to point at [request].sha.
     *
     * Implementations must:
     * - never force unless [request].force is explicitly true (which
     *   Painkiller never sets in v0).
     * - surface SHA mismatch as a typed error so the upload pipeline
     *   can stop without overwriting.
     */
    suspend fun updateRef(
        owner: String,
        repo: String,
        ref: String,
        request: GitHubUpdateRefRequest,
    ): GitHubRefResponse

    suspend fun listRepositories(): List<GitHubRepositorySummary>

    suspend fun listBranches(
        owner: String,
        repo: String,
    ): List<GitHubBranchSummary>
}
