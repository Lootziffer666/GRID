package com.painkiller.domain.files

import com.painkiller.domain.path.PathValidation

enum class ZipIntakeIssueCode {
    UNSAFE_PATH,
    COLLISION,
}

data class ZipIntakeIssue(
    val code: ZipIntakeIssueCode,
    val message: String,
    val relatedPath: String,
)

data class ZipIntakeEntry(
    val entryName: String,
    val sizeBytes: Long,
    val contentBase64: String,
)

data class ZipIntakeResult(
    val source: SelectedSource,
    val contentByRelativePath: Map<String, String>,
    val issues: List<ZipIntakeIssue>,
) {
    val hasUnsafeEntries: Boolean get() = issues.any { it.code == ZipIntakeIssueCode.UNSAFE_PATH }
}

/**
 * Pure ZIP intake normalization for Android adapters.
 *
 * - normalizes paths via [PathValidation]
 * - strips a single synthetic root folder (`archive-root/...`)
 * - records collisions deterministically (first entry wins)
 * - records unsafe entries (blocked before upload)
 */
object ZipIntakePlanner {

    private data class NormalizedEntry(
        val normalizedPath: String,
        val sizeBytes: Long,
        val contentBase64: String,
    )

    fun build(entries: List<ZipIntakeEntry>): ZipIntakeResult {
        val issues = mutableListOf<ZipIntakeIssue>()
        val safeEntries = mutableListOf<NormalizedEntry>()

        for (entry in entries) {
            val normalized = PathValidation.normalizeRepoPath(entry.entryName)
            if (normalized.isNullOrBlank()) {
                issues += ZipIntakeIssue(
                    code = ZipIntakeIssueCode.UNSAFE_PATH,
                    message = "Unsafe ZIP path blocked before upload.",
                    relatedPath = entry.entryName,
                )
                continue
            }
            safeEntries += NormalizedEntry(
                normalizedPath = normalized,
                sizeBytes = entry.sizeBytes,
                contentBase64 = entry.contentBase64,
            )
        }

        val rootNormalized = normalizeRootFolder(safeEntries)
        val deduped = linkedMapOf<String, NormalizedEntry>()
        for (entry in rootNormalized) {
            if (deduped.containsKey(entry.normalizedPath)) {
                issues += ZipIntakeIssue(
                    code = ZipIntakeIssueCode.COLLISION,
                    message = "ZIP path collision detected after normalization.",
                    relatedPath = entry.normalizedPath,
                )
            } else {
                deduped[entry.normalizedPath] = entry
            }
        }

        val ordered = deduped.values.toList().sortedBy { it.normalizedPath }
        val items = ordered.map { safe ->
            SelectedSourceItem(
                sourceId = safe.normalizedPath,
                displayName = safe.normalizedPath.substringAfterLast('/'),
                relativePath = safe.normalizedPath,
                sizeBytes = safe.sizeBytes,
                mimeType = null,
            )
        }

        return ZipIntakeResult(
            source = SelectedSource(kind = SourceKind.ZIP, items = items),
            contentByRelativePath = ordered.associate { it.normalizedPath to it.contentBase64 },
            issues = issues,
        )
    }

    private fun normalizeRootFolder(entries: List<NormalizedEntry>): List<NormalizedEntry> {
        if (entries.isEmpty()) return entries
        val topLevel = entries.map { it.normalizedPath.substringBefore('/') }.distinct()
        val hasRootFiles = entries.any { !it.normalizedPath.contains('/') }
        val shouldStripSingleRootFolder = topLevel.size == 1 && !hasRootFiles
        return entries.map { entry ->
            if (!shouldStripSingleRootFolder) return@map entry
            val stripped = entry.normalizedPath.substringAfter('/', missingDelimiterValue = entry.normalizedPath)
            entry.copy(normalizedPath = stripped)
        }
    }
}
