package com.painkiller.domain.files

/**
 * Source selection captured from Android SAF (or compatible adapters).
 *
 * `items` represent file-like entries that can become Git tree files.
 */
data class SelectedSource(
    val kind: SourceKind,
    val items: List<SelectedSourceItem>
)

/**
 * Metadata needed for deterministic planning in the pure domain layer.
 */
data class SelectedSourceItem(
    val sourceId: String,
    val displayName: String,
    val relativePath: String? = null,
    val sizeBytes: Long? = null,
    val mimeType: String? = null
)
