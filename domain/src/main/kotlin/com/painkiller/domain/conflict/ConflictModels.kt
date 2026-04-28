package com.painkiller.domain.conflict

data class ConflictSourceFile(
    val path: String,
    val content: String,
)

data class ConflictBlock(
    val index: Int,
    val currentText: String,
    val incomingText: String,
    val currentLabel: String?,
    val incomingLabel: String?,
)

data class ConflictFile(
    val path: String,
    val originalContent: String,
    val blocks: List<ConflictBlock>,
    val parseIssues: List<String> = emptyList(),
)

enum class ConflictPreset {
    KEEP_CURRENT,
    KEEP_INCOMING,
    KEEP_BOTH,
    REVIEW_MANUALLY,
}

data class ConflictResolutionPreview(
    val path: String,
    val originalContent: String,
    val resolvedContent: String?,
    val blockCount: Int,
    val malformed: Boolean,
    val unresolvedReason: String?,
)

data class ConflictResolutionPlan(
    val preset: ConflictPreset,
    val previews: List<ConflictResolutionPreview>,
    val totalFiles: Int,
    val filesWithCollisions: Int,
    val totalCollisionBlocks: Int,
    val malformedFiles: Int,
    val unresolvedFiles: Int,
    val summary: String,
    val writeAllowed: Boolean,
)

sealed interface ConflictParseResult {
    data class Success(
        val file: ConflictFile,
        val chunks: List<ConflictChunk>,
    ) : ConflictParseResult

    data class NoMarkers(
        val path: String,
        val originalContent: String,
    ) : ConflictParseResult

    data class Malformed(
        val file: ConflictFile,
    ) : ConflictParseResult
}

sealed interface ConflictChunk {
    data class PlainText(val text: String) : ConflictChunk
    data class BlockChunk(val block: ConflictBlock) : ConflictChunk
}
