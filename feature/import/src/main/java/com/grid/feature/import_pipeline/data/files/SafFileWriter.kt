package com.grid.feature.import_pipeline.data.files

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.painkiller.domain.conflict.ConflictFileWriteOutcome
import com.painkiller.domain.conflict.ConflictFileWriter
import java.io.IOException

class SafFileWriter(appContext: Context) : ConflictFileWriter {

    private val resolver: ContentResolver = appContext.applicationContext.contentResolver

    override fun writeText(sourceId: String, content: String): ConflictFileWriteOutcome {
        val uri = runCatching { Uri.parse(sourceId) }.getOrNull()
            ?: return ConflictFileWriteOutcome.Failure("Invalid file reference.")

        return try {
            resolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: return ConflictFileWriteOutcome.Failure("Android did not provide a writable stream.")
            ConflictFileWriteOutcome.Success
        } catch (_: SecurityException) {
            ConflictFileWriteOutcome.Failure(
                "Android did not allow GRID to write this file. Select it again with write permission.",
            )
        } catch (_: IOException) {
            ConflictFileWriteOutcome.Failure("Write failed while saving resolved content.")
        }
    }
}
