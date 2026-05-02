package com.painkiller.domain.conflict

import java.nio.charset.StandardCharsets

data class ResolvedConflictFile(
    val path: String,
    val sourceId: String?,
    val resolvedContent: String?,
    val blockedReason: String?,
) {
    val isEligible: Boolean get() = blockedReason == null && !resolvedContent.isNullOrEmpty() && !sourceId.isNullOrBlank()
    val bytes: Long get() = (resolvedContent?.toByteArray(StandardCharsets.UTF_8)?.size ?: 0).toLong()
}

data class ConflictWritePlan(
    val filesToWrite: List<ResolvedConflictFile>,
    val blockedFiles: List<ResolvedConflictFile>,
    val totalBytes: Long,
    val requiresConfirmation: Boolean,
    val summary: String,
) {
    val hasEligibleFiles: Boolean get() = filesToWrite.isNotEmpty()
}

data class ConflictWriteFailure(
    val path: String,
    val reason: String,
)

data class ConflictWriteResult(
    val writtenFiles: List<String>,
    val blockedFiles: List<String>,
    val failedFiles: List<ConflictWriteFailure>,
    val didChangeFiles: Boolean,
    val summary: String,
)

sealed interface ConflictFileWriteOutcome {
    data object Success : ConflictFileWriteOutcome
    data class Failure(val reason: String) : ConflictFileWriteOutcome
}

fun interface ConflictFileWriter {
    fun writeText(sourceId: String, content: String): ConflictFileWriteOutcome
}

object ConflictWritePlanner {

    fun fromPresetPlan(
        previewPlan: ConflictResolutionPlan?,
        sources: List<ConflictSourceFile>,
    ): ConflictWritePlan {
        if (previewPlan == null) {
            return emptyPlan("Preview is required before writing resolved files.")
        }
        val sourceByPath = sources.associateBy { it.path }
        val entries = previewPlan.previews.map { preview ->
            val source = sourceByPath[preview.path]
            val blockedReason = when {
                preview.resolvedContent == null -> preview.unresolvedReason ?: "Manual review needed."
                preview.blockCount == 0 -> "No collision blocks were found in this file."
                source == null -> "Selected source metadata is missing."
                source.sourceKind == com.painkiller.domain.files.SourceKind.ZIP ->
                    "Blocked for safety: ZIP entries cannot be written back in this gate."
                !source.writableBySaf || source.sourceId.isNullOrBlank() ->
                    "Android did not provide writable file access for this item."
                else -> null
            }
            ResolvedConflictFile(
                path = preview.path,
                sourceId = source?.sourceId,
                resolvedContent = preview.resolvedContent,
                blockedReason = blockedReason,
            )
        }
        return buildPlan(entries)
    }

    fun fromCardPreview(
        preview: ConflictReviewPreview?,
        session: ConflictReviewSession?,
        sources: List<ConflictSourceFile>,
    ): ConflictWritePlan {
        if (preview == null || session == null) {
            return emptyPlan("Preview is required before writing resolved files.")
        }
        val sourceByPath = sources.associateBy { it.path }
        val blockCountByPath = session.files.associate { it.path to it.blocks.size }

        val entries = preview.files.map { file ->
            val source = sourceByPath[file.path]
            val blockedReason = when {
                file.resolvedContent == null -> file.unresolvedReason ?: "Manual review needed."
                (blockCountByPath[file.path] ?: 0) == 0 -> "No collision blocks were found in this file."
                source == null -> "Selected source metadata is missing."
                source.sourceKind == com.painkiller.domain.files.SourceKind.ZIP ->
                    "Blocked for safety: ZIP entries cannot be written back in this gate."
                !source.writableBySaf || source.sourceId.isNullOrBlank() ->
                    "Android did not provide writable file access for this item."
                else -> null
            }
            ResolvedConflictFile(
                path = file.path,
                sourceId = source?.sourceId,
                resolvedContent = file.resolvedContent,
                blockedReason = blockedReason,
            )
        }
        return buildPlan(entries)
    }

    private fun buildPlan(entries: List<ResolvedConflictFile>): ConflictWritePlan {
        val writeable = entries.filter { it.isEligible }
        val blocked = entries.filterNot { it.isEligible }
        val totalBytes = writeable.sumOf { it.bytes }
        val summary = when {
            entries.isEmpty() -> "No conflict files were available for write-back."
            writeable.isEmpty() -> "No files are eligible for write-back. Painkiller did not write any files."
            blocked.isEmpty() -> "${writeable.size} file(s) ready to write. Nothing will be committed or pushed."
            else -> "${writeable.size} file(s) ready, ${blocked.size} blocked for safety. Nothing will be committed or pushed."
        }
        return ConflictWritePlan(
            filesToWrite = writeable,
            blockedFiles = blocked,
            totalBytes = totalBytes,
            requiresConfirmation = true,
            summary = summary,
        )
    }

    private fun emptyPlan(summary: String): ConflictWritePlan = ConflictWritePlan(
        filesToWrite = emptyList(),
        blockedFiles = emptyList(),
        totalBytes = 0,
        requiresConfirmation = true,
        summary = summary,
    )
}

object ConflictWriteExecutor {
    fun execute(
        plan: ConflictWritePlan?,
        confirmed: Boolean,
        writer: ConflictFileWriter,
    ): ConflictWriteResult {
        if (plan == null) {
            return ConflictWriteResult(
                writtenFiles = emptyList(),
                blockedFiles = emptyList(),
                failedFiles = emptyList(),
                didChangeFiles = false,
                summary = "Preview is required before writing resolved files.",
            )
        }
        if (!confirmed) {
            return ConflictWriteResult(
                writtenFiles = emptyList(),
                blockedFiles = plan.blockedFiles.map { it.path },
                failedFiles = emptyList(),
                didChangeFiles = false,
                summary = "Write cancelled before confirmation. No files were changed.",
            )
        }
        if (plan.filesToWrite.isEmpty()) {
            return ConflictWriteResult(
                writtenFiles = emptyList(),
                blockedFiles = plan.blockedFiles.map { it.path },
                failedFiles = emptyList(),
                didChangeFiles = false,
                summary = "No files are eligible for write-back. Painkiller did not write any files.",
            )
        }

        val written = mutableListOf<String>()
        val failed = mutableListOf<ConflictWriteFailure>()

        plan.filesToWrite.forEach { file ->
            val sourceId = file.sourceId
            val content = file.resolvedContent
            if (sourceId.isNullOrBlank() || content == null) {
                failed += ConflictWriteFailure(file.path, "Missing writable source metadata.")
                return@forEach
            }
            when (val outcome = writer.writeText(sourceId, content)) {
                is ConflictFileWriteOutcome.Success -> written += file.path
                is ConflictFileWriteOutcome.Failure -> failed += ConflictWriteFailure(file.path, outcome.reason)
            }
        }

        val summary = when {
            failed.isEmpty() -> "Painkiller wrote ${written.size} file(s). No commit was created and nothing was pushed."
            written.isEmpty() -> "Painkiller could not write resolved files. No commit was created and nothing was pushed."
            else -> "Painkiller wrote ${written.size} file(s), but ${failed.size} file(s) failed. No commit was created and nothing was pushed."
        }

        return ConflictWriteResult(
            writtenFiles = written,
            blockedFiles = plan.blockedFiles.map { it.path },
            failedFiles = failed,
            didChangeFiles = written.isNotEmpty(),
            summary = summary,
        )
    }
}
