package com.grid.feature.import_pipeline.data.files

import android.content.ContentResolver
import android.net.Uri
import com.painkiller.domain.github.UploadPayload
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

class SafUriUploadPayload(
    private val resolver: ContentResolver,
    private val uri: Uri,
    override val sizeBytes: Long,
) : UploadPayload {
    override fun openStream(): InputStream {
        return resolver.openInputStream(uri) ?: throw IOException("Could not open source stream.")
    }
}

class ByteArrayUploadPayload(
    private val bytes: ByteArray,
) : UploadPayload {
    override val sizeBytes: Long = bytes.size.toLong()

    override fun openStream(): InputStream = ByteArrayInputStream(bytes)
}
