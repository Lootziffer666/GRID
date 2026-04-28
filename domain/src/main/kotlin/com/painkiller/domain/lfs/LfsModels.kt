package com.painkiller.domain.lfs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@JvmInline
value class LfsObjectId(val value: String) {
    init {
        require(value.matches(Regex("^[a-f0-9]{64}$"))) { "LFS oid must be lowercase sha256 hex" }
    }
}

data class LfsUploadPlan(
    val oid: LfsObjectId,
    val sizeBytes: Long,
    val pointerText: String,
)

@Serializable
data class LfsBatchRequest(
    val operation: String,
    val transfers: List<String> = listOf("basic"),
    val objects: List<LfsBatchObjectRequest>,
    val ref: LfsBatchRef? = null,
)

@Serializable
data class LfsBatchRef(
    val name: String,
)

@Serializable
data class LfsBatchObjectRequest(
    val oid: String,
    val size: Long,
)

@Serializable
data class LfsBatchResponse(
    val transfer: String? = null,
    val objects: List<LfsBatchObjectResponse>,
)

@Serializable
data class LfsBatchObjectResponse(
    val oid: String,
    val size: Long,
    val actions: LfsObjectActions? = null,
    val error: LfsObjectError? = null,
)

@Serializable
data class LfsObjectActions(
    val upload: LfsObjectAction? = null,
    val verify: LfsObjectAction? = null,
)

@Serializable
data class LfsObjectAction(
    val href: String,
    val header: Map<String, String> = emptyMap(),
    @SerialName("expires_in")
    val expiresIn: Long? = null,
)

@Serializable
data class LfsObjectError(
    val code: Int,
    val message: String,
)
