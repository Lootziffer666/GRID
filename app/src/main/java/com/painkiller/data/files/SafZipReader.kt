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

    private data class RawZipEntry(
        val normalizedPath: String,
        val displayName: String,
        val sizeBytes: Long,
        val contentBase64: String,
    )

    data class ZipReadResult(
        val source: SelectedSource,
        val contentByRelativePath: Map<String, String>,
    )

    suspend fun read(zipUri: Uri): ZipReadResult = withContext(Dispatchers.IO) {
        val rawEntries = mutableListOf<RawZipEntry>()

        context.contentResolver.openInputStream(zipUri)?.use { stream ->
            ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null && rawEntries.size < MAX_FILES) {
                    if (!entry.isDirectory) {
                        val normalized = PathValidation.normalizeRepoPath(entry.name ?: "")
                        if (!normalized.isNullOrBlank()) {
                            val bytes = zip.readBytes()
                            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                            rawEntries += RawZipEntry(
                                normalizedPath = normalized,
                                displayName = normalized.substringAfterLast('/'),
                                sizeBytes = entry.size.takeIf { it > 0L } ?: bytes.size.toLong(),
                                contentBase64 = base64,
                            )
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        val normalizedEntries = normalizeRootForZip(rawEntries)
            .distinctBy { it.normalizedPath }
        val items = normalizedEntries.map { entry ->
            SelectedSourceItem(
                sourceId = entry.normalizedPath,
                displayName = entry.displayName,
                relativePath = entry.normalizedPath,
                sizeBytes = entry.sizeBytes,
                mimeType = null,
            )
        }
        val content = normalizedEntries.associate { it.normalizedPath to it.contentBase64 }

        ZipReadResult(
            source = SelectedSource(SourceKind.ZIP, items),
            contentByRelativePath = content,
        )
    }

    private fun normalizeRootForZip(entries: List<RawZipEntry>): List<RawZipEntry> {
        if (entries.isEmpty()) return entries
        val topLevel = entries.map { it.normalizedPath.substringBefore('/') }.distinct()
        val hasRootFiles = entries.any { !it.normalizedPath.contains('/') }
        val shouldStripSingleRootFolder = topLevel.size == 1 && !hasRootFiles
        return entries.map { entry ->
            if (!shouldStripSingleRootFolder) return@map entry
            val stripped = entry.normalizedPath.substringAfter('/', missingDelimiterValue = entry.normalizedPath)
            entry.copy(normalizedPath = stripped)
        }.sortedBy { it.normalizedPath }
    }

    private companion object {
        const val MAX_FILES = 500
    }
}
