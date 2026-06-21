package com.painkiller.domain.github

import com.painkiller.domain.path.PathValidation

/**
 * Compiles a list of [PendingChange] into [TreeEntry] modifications suitable
 * for passing to a GitHub createTree request.
 *
 * The compiler resolves blob SHAs from the existing tree for MOVE and RENAME
 * operations, injects `.gitkeep` for folder creation, and validates all paths
 * for safety (ZIP-Slip prevention).
 *
 * Throws [IllegalArgumentException] if any path is unsafe or a required blob
 * SHA cannot be resolved from the existing tree.
 */
object TreeChangeCompiler {

    private const val GITKEEP_NAME = ".gitkeep"

    /**
     * Compiles [changes] against [existingTree] into a list of [TreeEntry]
     * entries that represent the delta to apply via createTree.
     *
     * @throws IllegalArgumentException if a path fails validation or a
     *   source blob cannot be found in the existing tree.
     */
    fun compile(
        changes: List<PendingChange>,
        existingTree: List<TreeEntry>,
    ): List<TreeEntry> {
        val treeIndex = existingTree.associateBy { it.path }
        val result = mutableListOf<TreeEntry>()

        for (change in changes) {
            when (change.type) {
                PendingChangeType.MOVE, PendingChangeType.RENAME -> {
                    val source = requireSafePath(change.sourcePath, "sourcePath")
                    val target = requireSafePath(change.targetPath, "targetPath")
                    val existing = treeIndex[source]
                        ?: throw IllegalArgumentException(
                            "Source path not found in existing tree: \"$source\""
                        )
                    // Delete old path
                    result += TreeEntry(
                        path = source,
                        mode = TreeEntry.MODE_FILE,
                        type = TreeEntry.TYPE_BLOB,
                        sha = null,
                    )
                    // Add at new path with original blob SHA
                    result += TreeEntry(
                        path = target,
                        mode = existing.mode,
                        type = TreeEntry.TYPE_BLOB,
                        sha = existing.sha,
                    )
                }

                PendingChangeType.DELETE -> {
                    val source = requireSafePath(change.sourcePath, "sourcePath")
                    result += TreeEntry(
                        path = source,
                        mode = TreeEntry.MODE_FILE,
                        type = TreeEntry.TYPE_BLOB,
                        sha = null,
                    )
                }

                PendingChangeType.CREATE_FOLDER -> {
                    val target = requireSafePath(change.targetPath, "targetPath")
                    val gitkeepPath = "$target/$GITKEEP_NAME"
                    result += TreeEntry(
                        path = gitkeepPath,
                        mode = TreeEntry.MODE_FILE,
                        type = TreeEntry.TYPE_BLOB,
                        sha = null,
                        content = "",
                    )
                }

                PendingChangeType.UPLOAD -> {
                    val target = requireSafePath(change.targetPath, "targetPath")
                    if (change.blobSha != null) {
                        result += TreeEntry(
                            path = target,
                            mode = TreeEntry.MODE_FILE,
                            type = TreeEntry.TYPE_BLOB,
                            sha = change.blobSha,
                        )
                    } else if (change.newContentBase64 != null) {
                        // Will need a blob to be created; mark with content placeholder.
                        // The orchestrator handles actual blob creation; here we record
                        // the entry with a sentinel that the orchestrator replaces.
                        result += TreeEntry(
                            path = target,
                            mode = TreeEntry.MODE_FILE,
                            type = TreeEntry.TYPE_BLOB,
                            sha = null,
                            content = change.newContentBase64,
                        )
                    } else {
                        throw IllegalArgumentException(
                            "UPLOAD change at \"$target\" must have either blobSha or newContentBase64."
                        )
                    }
                }
            }
        }

        return result
    }

    private fun requireSafePath(path: String?, label: String): String {
        requireNotNull(path) { "$label must not be null." }
        require(path.isNotBlank()) { "$label must not be blank." }
        val normalized = PathValidation.normalizeRepoPath(path)
        require(normalized != null && normalized == path) {
            "Unsafe $label: \"$path\""
        }
        return path
    }
}
