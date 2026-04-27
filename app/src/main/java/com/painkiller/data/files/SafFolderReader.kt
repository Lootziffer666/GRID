package com.painkiller.data.files

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.painkiller.domain.files.SelectedSource
import com.painkiller.domain.files.SelectedSourceItem
import com.painkiller.domain.files.SourceKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Traverses a Storage Access Framework document tree and returns a
 * [SelectedSource] of type [SourceKind.FOLDER] ready for [com.painkiller.domain.files.FilePlanBuilder].
 *
 * Traversal is bounded by [MAX_DEPTH] and [MAX_FILES] to prevent
 * accidental submission of enormous trees. Ignored folders ([IGNORED_FOLDERS])
 * are skipped entirely during traversal so their contents never reach
 * the planner or the upload quota.
 *
 * All I/O runs on [Dispatchers.IO]. The result contains only metadata
 * (URI, display name, relative path, size, MIME type) — file content
 * is read individually at commit time (Gate 14).
 */
class SafFolderReader(appContext: Context) {

    private val context = appContext.applicationContext

    suspend fun read(treeUri: Uri): SelectedSource = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext SelectedSource(SourceKind.FOLDER, emptyList())
        val items = mutableListOf<SelectedSourceItem>()
        traverse(root, prefix = "", items = items, depth = 0)
        SelectedSource(SourceKind.FOLDER, items)
    }

    private fun traverse(
        dir: DocumentFile,
        prefix: String,
        items: MutableList<SelectedSourceItem>,
        depth: Int,
    ) {
        if (depth > MAX_DEPTH || items.size >= MAX_FILES) return
        for (child in dir.listFiles()) {
            if (items.size >= MAX_FILES) break
            val name = child.name ?: continue
            val path = if (prefix.isEmpty()) name else "$prefix/$name"
            if (child.isDirectory) {
                if (name !in IGNORED_FOLDERS) traverse(child, path, items, depth + 1)
            } else {
                items += SelectedSourceItem(
                    sourceId = child.uri.toString(),
                    displayName = name,
                    relativePath = path,
                    sizeBytes = child.length().takeIf { it > 0L },
                    mimeType = child.type,
                )
            }
        }
    }

    private companion object {
        const val MAX_DEPTH = 10
        const val MAX_FILES = 500
        val IGNORED_FOLDERS = setOf(".git", ".gradle", "build", "node_modules", ".idea")
    }
}
