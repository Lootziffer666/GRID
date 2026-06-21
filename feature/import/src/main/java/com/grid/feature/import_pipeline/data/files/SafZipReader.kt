package com.grid.feature.import_pipeline.data.files

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.painkiller.domain.files.ZipIntakeEntry
import com.painkiller.domain.files.ZipIntakeIssue
import com.painkiller.domain.files.ZipIntakePlanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipInputStream

/**
 * Reads a Storage Access Framework ZIP into normalized file-like entries.
 *
 * Adapter responsibilities:
 * - stream entries from SAF/ZipInputStream
 * - encode entry bytes to Base64
 * - delegate path safety, root normalization, and collision handling to
 *   [ZipIntakePlanner] (pure domain)
 */
class SafZipReader(appContext: Context) {

    private val context = appContext.applicationContext

    data class ZipReadResult(
        val source: com.painkiller.domain.files.SelectedSource,
        val contentByRelativePath: Map<String, String>,
        val issues: List<ZipIntakeIssue>,
    ) {
        val hasUnsafeEntries: Boolean get() = issues.any { it.code == com.painkiller.domain.files.ZipIntakeIssueCode.UNSAFE_PATH }
    }

    suspend fun read(zipUri: Uri): ZipReadResult = withContext(Dispatchers.IO) {
        val entries = mutableListOf<ZipIntakeEntry>()

        context.contentResolver.openInputStream(zipUri)?.use { stream ->
            ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null && entries.size < MAX_FILES) {
                    if (!entry.isDirectory) {
                        val bytes = zip.readBytes()
                        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        entries += ZipIntakeEntry(
                            entryName = entry.name ?: "",
                            sizeBytes = entry.size.takeIf { it > 0L } ?: bytes.size.toLong(),
                            contentBase64 = base64,
                        )
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        val planned = ZipIntakePlanner.build(entries)
        ZipReadResult(
            source = planned.source,
            contentByRelativePath = planned.contentByRelativePath,
            issues = planned.issues,
        )
    }

    private companion object {
        const val MAX_FILES = 500
    }
}
