package com.painkiller.app.data.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GitHub Git Data API DTOs — Gate 0 model spike.
 *
 * Field names mirror the wire format from the GitHub REST API
 * (`/repos/{owner}/{repo}/git/...`) so the same DTOs can be reused when
 * a real HTTP client is wired up in Gate 6.
 *
 * Nothing here performs network I/O. Production wiring (auth headers,
 * If-Match guards, retry policy, error classification) belongs to
 * later gates.
 */

@Serializable
data class GitHubBlobRequest(
    val content: String,
    val encoding: String = "base64",
)

@Serializable
data class GitHubBlobResponse(
    val sha: String,
    val url: String? = null,
)

@Serializable
data class GitHubTreeEntryRequest(
    val path: String,
    /**
     * Standard GitHub tree modes:
     * - "100644" file (blob)
     * - "100755" executable
     * - "040000" directory
     * - "160000" submodule
     * - "120000" symlink
     */
    val mode: String,
    /** "blob", "tree", or "commit". */
    val type: String,
    val sha: String? = null,
    val content: String? = null,
)

@Serializable
data class GitHubCreateTreeRequest(
    @SerialName("base_tree") val baseTree: String? = null,
    val tree: List<GitHubTreeEntryRequest>,
)

@Serializable
data class GitHubCreateTreeResponse(
    val sha: String,
    val url: String? = null,
)

@Serializable
data class GitHubCreateCommitRequest(
    val message: String,
    val tree: String,
    val parents: List<String>,
)

@Serializable
data class GitHubCreateCommitResponse(
    val sha: String,
    val url: String? = null,
)

@Serializable
data class GitHubUpdateRefRequest(
    val sha: String,
    /**
     * Painkiller never force-updates a branch. The default `false`
     * matches GitHub's default safe behavior.
     */
    val force: Boolean = false,
)

@Serializable
data class GitHubRefResponse(
    val ref: String,
    @SerialName("node_id") val nodeId: String? = null,
    val url: String? = null,
    @SerialName("object") val obj: GitHubRefObject,
)

@Serializable
data class GitHubRefObject(
    val sha: String,
    val type: String,
    val url: String? = null,
)

@Serializable
data class GitHubRepositorySummary(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("default_branch") val defaultBranch: String? = null,
    val private: Boolean = false,
)

@Serializable
data class GitHubBranchSummary(
    val name: String,
    val protected: Boolean = false,
    val commit: GitHubBranchCommit? = null,
)

@Serializable
data class GitHubBranchCommit(
    val sha: String,
    val url: String? = null,
)
