package com.painkiller.data.files

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.painkiller.domain.files.SelectedSourceItem
import com.painkiller.domain.files.SourceKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Reads a single SAF [Uri] into a [LoadedFile] containing display name, MIME
 * type, byte size, and Base64-encoded content. The file is fully read into
 * memory — Painkiller's blocked-size threshold (>100 MiB) is enforced
 * upstream by `LargeFileDoctor`, so this reader can safely buffer the bytes.
 *
 * All disk I/O hops to [Dispatchers.IO]. ContentResolver lookups can be
 * slow on cold caches, so callers should not invoke this from the main
 * thread.
 */
class SafFileReader(
    appContext: Context,
) {
    private val resolver: ContentResolver = appContext.applicationContext.contentResolver

    /**
     * Reads [uri] and returns its metadata + Base64-encoded content. Returns
     * null if the URI is no longer readable (e.g. permission revoked, file
     * deleted, content provider gone).
     */
    suspend fun read(uri: Uri): LoadedFile? = withContext(Dispatchers.IO) {
        val (displayName, sizeBytes) = queryMetadata(uri) ?: return@withContext null
        val mimeType = resolver.getType(uri)
        val bytes = try {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: SecurityException) {
            null
        } catch (e: IOException) {
            null
        } ?: return@withContext null

        LoadedFile(
            displayName = displayName,
            sizeBytes = sizeBytes ?: bytes.size.toLong(),
            mimeType = mimeType,
            contentBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
            sourceItem = SelectedSourceItem(
                sourceId = uri.toString(),
                displayName = displayName,
                relativePath = displayName,
                sizeBytes = sizeBytes ?: bytes.size.toLong(),
                mimeType = mimeType,
            ),
        )
    }

    private fun queryMetadata(uri: Uri): Pair<String, Long?>? {
        return try {
            resolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null, null, null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null
                if (name.isNullOrBlank()) null else name to size
            }
        } catch (e: SecurityException) {
            null
        }
    }
}

/**
 * Result of reading one file via SAF.
 */
data class LoadedFile(
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String?,
    /** Base64-encoded file content, suitable for `CreateBlobRequest`. */
    val contentBase64: String,
    /** [SelectedSourceItem] view of this file for `:domain` planning. */
    val sourceItem: SelectedSourceItem,
) {
    fun sourceKind(): SourceKind = SourceKind.SINGLE_FILE
}
