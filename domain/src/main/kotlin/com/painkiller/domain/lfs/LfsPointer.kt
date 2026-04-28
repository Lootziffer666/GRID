package com.painkiller.domain.lfs

import java.io.InputStream
import java.security.MessageDigest

data class StreamDigestResult(
    val sha256Hex: String,
    val sizeBytes: Long,
)

object LfsPointer {
    const val VERSION = "https://git-lfs.github.com/spec/v1"

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun digestStream(input: InputStream): StreamDigestResult {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            digest.update(buffer, 0, read)
            total += read
        }
        val sha = digest.digest().joinToString("") { "%02x".format(it) }
        return StreamDigestResult(sha256Hex = sha, sizeBytes = total)
    }

    fun buildPlan(bytes: ByteArray): LfsUploadPlan {
        return buildPlan(
            oidHex = sha256Hex(bytes),
            sizeBytes = bytes.size.toLong(),
        )
    }

    fun buildPlanFromStream(input: InputStream): LfsUploadPlan {
        val digestResult = digestStream(input)
        return buildPlan(
            oidHex = digestResult.sha256Hex,
            sizeBytes = digestResult.sizeBytes,
        )
    }

    private fun buildPlan(oidHex: String, sizeBytes: Long): LfsUploadPlan {
        val oid = LfsObjectId(oidHex)
        val pointer = buildString {
            append("version ")
            append(VERSION)
            append('\n')
            append("oid sha256:")
            append(oid.value)
            append('\n')
            append("size ")
            append(sizeBytes)
            append('\n')
        }
        return LfsUploadPlan(
            oid = oid,
            sizeBytes = sizeBytes,
            pointerText = pointer,
        )
    }
}
