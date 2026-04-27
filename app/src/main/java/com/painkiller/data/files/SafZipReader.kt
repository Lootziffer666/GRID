package com.painkiller.data.files

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.painkiller.domain.files.SelectedSource
import com.painkiller.domain.files.SelectedSourceItem
import com.painkiller.domain.files.SourceKind
import com.painkiller.domain.path.PathValidation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipInputStream

/**
 * Reads a Storage Access Framework ZIP into a [ZipReadResult] of metadata
 * plus per-entry Base64 content. The ZIP is opened via [android.content.ContentResolver]
 * and walked with a single [ZipInputStream] pass. Entries are read into memory
 * and Base64-encoded immediately so the SAF stream can be released; nothing
 * persists on disk.
 *
 * ## ZIP-Slip prevention
 * Every entry name is normalized via [PathValidation.normalizeRepoPath] before
 * any byte is kept. Entries whose names contain `..`, absolute roots, or
 * Windows drive prefixes are silently skipped here — the orchestrator's
 * defence-in-depth check ([com.painkiller.domain.github.MultiFileCommitOrchestrator])
 * remains the second wall.
 *
 * ## Bounds
 * Traversal stops at [MAX_FILES] entries to keep upload payloads predictable.
 * Per-file size limits are enforced later by `LargeFileDoctor` during planning.
 *
 * All I/O runs on [Dispatchers.IO].
 */
class SafZipReader(appContext: Context) {

    private val context = appContext.applicationContext

    data class ZipReadResult(
        val source: SelectedSource,
        val contentByRelativePath: Map<String, String>,
    )

    suspend fun read(zipUri: Uri): ZipReadResult = withContext(Dispatchers.IO) {
        val items = mutableListOf<SelectedSourceItem>()
        val content = mutableMapOf<String, String>()

        context.contentResolver.openInputStream(zipUri)?.use { stream ->
            ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null && items.size < MAX_FILES) {
                    if (!entry.isDirectory) {
                        val normalized = PathValidation.normalizeRepoPath(entry.name ?: "")
                        if (!normalized.isNullOrBlank()) {
                            val bytes = zip.readBytes()
                            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                            items += SelectedSourceItem(
                                sourceId = normalized,
                                displayName = normalized.substringAfterLast('/'),
                                relativePath = normalized,
                                sizeBytes = entry.size.takeIf { it > 0L } ?: bytes.size.toLong(),
                                mimeType = null,
                            )
                            content[normalized] = base64
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        ZipReadResult(
            source = SelectedSource(SourceKind.ZIP, items),
            contentByRelativePath = content,
        )
    }

    private companion object {
        const val MAX_FILES = 500
    }
}
