package com.painkiller.domain.github

import java.io.InputStream

/**
 * Streamable upload source used by large upload paths (LFS and release assets)
 * to avoid eager ByteArray materialization.
 */
interface UploadPayload {
    val sizeBytes: Long
    fun openStream(): InputStream
}
