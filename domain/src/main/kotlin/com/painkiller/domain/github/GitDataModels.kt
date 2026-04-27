package com.painkiller.domain.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Gate 0 spike. These are the request/response shapes for the GitHub Git Data API
// flow Painkiller will use in later gates: get ref, get commit, create blob,
// create tree, create commit, update ref. Nothing here issues network calls.

@Serializable
data class GitRef(
    @SerialName("ref") val ref: String,
    @SerialName("node_id") val nodeId: String? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("object") val obj: GitRefObject,
)

@Serializable
data class GitRefObject(
    @SerialName("sha") val sha: String,
    @SerialName("type") val type: String,
    @SerialName("url") val url: String? = null,
)

@Serializable
data class GitCommit(
    @SerialName("sha") val sha: String,
    @SerialName("message") val message: String,
    @SerialName("tree") val tree: GitTreeRef,
    @SerialName("parents") val parents: List<GitTreeRef> = emptyList(),
    @SerialName("url") val url: String? = null,
)

@Serializable
data class GitTreeRef(
    @SerialName("sha") val sha: String,
    @SerialName("url") val url: String? = null,
)

@Serializable
data class CreateBlobRequest(
    @SerialName("content") val content: String,
    @SerialName("encoding") val encoding: String = "base64",
)

@Serializable
data class CreateBlobResponse(
    @SerialName("sha") val sha: String,
    @SerialName("url") val url: String? = null,
)

@Serializable
data class CreateTreeRequest(
    @SerialName("base_tree") val baseTree: String?,
    @SerialName("tree") val tree: List<TreeEntry>,
)

@Serializable
data class TreeEntry(
    @SerialName("path") val path: String,
    @SerialName("mode") val mode: String = MODE_FILE,
    @SerialName("type") val type: String = TYPE_BLOB,
    @SerialName("sha") val sha: String? = null,
    @SerialName("content") val content: String? = null,
) {
    companion object {
        const val MODE_FILE = "100644"
        const val MODE_EXECUTABLE = "100755"
        const val MODE_SUBDIR = "040000"
        const val MODE_SUBMODULE = "160000"
        const val MODE_SYMLINK = "120000"

        const val TYPE_BLOB = "blob"
        const val TYPE_TREE = "tree"
        const val TYPE_COMMIT = "commit"
    }
}

@Serializable
data class CreateTreeResponse(
    @SerialName("sha") val sha: String,
    @SerialName("url") val url: String? = null,
    @SerialName("tree") val tree: List<TreeEntry> = emptyList(),
    @SerialName("truncated") val truncated: Boolean = false,
)

@Serializable
data class CreateCommitRequest(
    @SerialName("message") val message: String,
    @SerialName("tree") val tree: String,
    @SerialName("parents") val parents: List<String>,
)

@Serializable
data class CreateCommitResponse(
    @SerialName("sha") val sha: String,
    @SerialName("message") val message: String,
    @SerialName("url") val url: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
)

@Serializable
data class UpdateRefRequest(
    @SerialName("sha") val sha: String,
    @SerialName("force") val force: Boolean = false,
)

@Serializable
data class GithubRepositorySummary(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("private") val private: Boolean,
    @SerialName("default_branch") val defaultBranch: String,
    @SerialName("html_url") val htmlUrl: String? = null,
)

@Serializable
data class GithubBranchSummary(
    @SerialName("name") val name: String,
    @SerialName("commit") val commit: GitTreeRef,
    @SerialName("protected") val protected: Boolean = false,
)

@Serializable
data class GithubPullRequestSummary(
    @SerialName("id") val id: Long,
    @SerialName("number") val number: Long,
    @SerialName("title") val title: String,
    @SerialName("state") val state: String,
    @SerialName("draft") val draft: Boolean = false,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("head") val head: GithubPullRequestHead,
)

@Serializable
data class GithubPullRequestHead(
    @SerialName("ref") val ref: String,
    @SerialName("sha") val sha: String,
    @SerialName("label") val label: String? = null,
)

@Serializable
data class GithubPullRequestDetail(
    @SerialName("id") val id: Long,
    @SerialName("number") val number: Long,
    @SerialName("title") val title: String,
    @SerialName("state") val state: String,
    @SerialName("draft") val draft: Boolean = false,
    @SerialName("mergeable") val mergeable: Boolean? = null,
    @SerialName("mergeable_state") val mergeableState: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("head") val head: GithubPullRequestHead,
)

@Serializable
data class MergePullRequestRequest(
    @SerialName("commit_title") val commitTitle: String? = null,
    @SerialName("merge_method") val mergeMethod: String = "merge",
    @SerialName("sha") val sha: String? = null,
)

@Serializable
data class MergePullRequestResponse(
    @SerialName("sha") val sha: String? = null,
    @SerialName("merged") val merged: Boolean,
    @SerialName("message") val message: String,
)
