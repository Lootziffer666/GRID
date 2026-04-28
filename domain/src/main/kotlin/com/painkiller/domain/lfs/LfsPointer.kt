package com.painkiller.domain.lfs

import java.security.MessageDigest

object LfsPointer {
    const val VERSION = "https://git-lfs.github.com/spec/v1"

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun buildPlan(bytes: ByteArray): LfsUploadPlan {
        val oid = LfsObjectId(sha256Hex(bytes))
        val size = bytes.size.toLong()
        val pointer = buildString {
            append("version ")
            append(VERSION)
            append('\n')
            append("oid sha256:")
            append(oid.value)
            append('\n')
            append("size ")
            append(size)
            append('\n')
        }
        return LfsUploadPlan(
            oid = oid,
            sizeBytes = size,
            pointerText = pointer,
        )
    }
}
