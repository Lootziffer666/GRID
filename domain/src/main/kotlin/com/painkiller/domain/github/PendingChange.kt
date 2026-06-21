package com.painkiller.domain.github

/**
 * Represents a single file-management operation that will be compiled into
 * tree entries for an atomic commit via the GitHub Git Data API.
 *
 * [sourcePath] is the original path in the tree (for MOVE, RENAME, DELETE).
 * [targetPath] is the destination path (for MOVE, RENAME, CREATE_FOLDER, UPLOAD).
 * [blobSha] is used by UPLOAD when the blob already exists on GitHub.
 * [newContentBase64] is used by UPLOAD when a new blob must be created.
 */
enum class PendingChangeType {
    MOVE,
    RENAME,
    DELETE,
    CREATE_FOLDER,
    UPLOAD,
}

data class PendingChange(
    val type: PendingChangeType,
    val sourcePath: String?,
    val targetPath: String?,
    val blobSha: String? = null,
    val newContentBase64: String? = null,
)
