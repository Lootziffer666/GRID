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

class SafFileReader(
    appContext: Context,
) {
    private val resolver: ContentResolver = appContext.applicationContext.contentResolver

    suspend fun read(uri: Uri): LoadedFile? = withContext(Dispatchers.IO) {
        val metadata = readMetadataInternal(uri) ?: return@withContext null
        val bytes = try {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: SecurityException) {
            null
        } catch (e: IOException) {
            null
        } ?: return@withContext null

        metadata.toLoadedFile(
            contentBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
            sizeBytes = metadata.sizeBytes ?: bytes.size.toLong(),
        )
    }

    suspend fun readMetadata(uri: Uri): LoadedFile? = withContext(Dispatchers.IO) {
        val metadata = readMetadataInternal(uri) ?: return@withContext null
        metadata.toLoadedFile(
            contentBase64 = null,
            sizeBytes = metadata.sizeBytes ?: 0L,
        )
    }

    fun createUploadPayload(sourceId: String, sizeBytes: Long): SafUriUploadPayload? {
        val uri = runCatching { Uri.parse(sourceId) }.getOrNull() ?: return null
        return SafUriUploadPayload(resolver = resolver, uri = uri, sizeBytes = sizeBytes)
    }

    private fun readMetadataInternal(uri: Uri): FileMetadata? {
        val (displayName, sizeBytes) = queryMetadata(uri) ?: return null
        val mimeType = resolver.getType(uri)
        return FileMetadata(uri, displayName, sizeBytes, mimeType)
    }

    private fun queryMetadata(uri: Uri): Pair<String, Long?>? {
        return try {
            resolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null,
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

    private data class FileMetadata(
        val uri: Uri,
        val displayName: String,
        val sizeBytes: Long?,
        val mimeType: String?,
    ) {
        fun toLoadedFile(contentBase64: String?, sizeBytes: Long): LoadedFile {
            return LoadedFile(
                displayName = displayName,
                sizeBytes = sizeBytes,
                mimeType = mimeType,
                contentBase64 = contentBase64,
                sourceItem = SelectedSourceItem(
                    sourceId = uri.toString(),
                    displayName = displayName,
                    relativePath = displayName,
                    sizeBytes = sizeBytes,
                    mimeType = mimeType,
                ),
            )
        }
    }
}

data class LoadedFile(
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String?,
    val contentBase64: String?,
    val sourceItem: SelectedSourceItem,
) {
    fun sourceKind(): SourceKind = SourceKind.SINGLE_FILE
}
