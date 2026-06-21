package com.painkiller.domain.conflict

object ConflictPresetPlanner {

    fun buildPreviewPlan(
        files: List<ConflictSourceFile>,
        preset: ConflictPreset,
    ): ConflictResolutionPlan {
        val previews = files.map { source ->
            when (val parsed = ConflictMarkerParser.parse(source.path, source.content)) {
                is ConflictParseResult.NoMarkers -> ConflictResolutionPreview(
                    path = source.path,
                    originalContent = source.content,
                    resolvedContent = source.content,
                    blockCount = 0,
                    malformed = false,
                    unresolvedReason = "No conflict markers found.",
                )

                is ConflictParseResult.Malformed -> ConflictResolutionPreview(
                    path = source.path,
                    originalContent = source.content,
                    resolvedContent = null,
                    blockCount = 0,
                    malformed = true,
                    unresolvedReason = parsed.file.parseIssues.firstOrNull()
                        ?: "Malformed conflict markers. Needs manual review.",
                )

                is ConflictParseResult.Success -> resolveParsedFile(parsed, preset)
            }
        }

        val filesWithCollisions = previews.count { it.blockCount > 0 }
        val totalBlocks = previews.sumOf { it.blockCount }
        val malformedCount = previews.count { it.malformed }
        val unresolved = previews.count { it.resolvedContent == null }

        val summary = when {
            filesWithCollisions == 0 ->
                "No conflict markers were found. GRID did not change any files."

            preset == ConflictPreset.REVIEW_MANUALLY ->
                "$totalBlocks collisions found. Manual review selected; nothing is resolved automatically."

            else ->
                "$totalBlocks collisions found. GRID built a preview only. Nothing is written yet."
        }

        return ConflictResolutionPlan(
            preset = preset,
            previews = previews,
            totalFiles = files.size,
            filesWithCollisions = filesWithCollisions,
            totalCollisionBlocks = totalBlocks,
            malformedFiles = malformedCount,
            unresolvedFiles = unresolved,
            summary = summary,
            writeAllowed = false,
        )
    }

    private fun resolveParsedFile(
        parsed: ConflictParseResult.Success,
        preset: ConflictPreset,
    ): ConflictResolutionPreview {
        if (preset == ConflictPreset.REVIEW_MANUALLY) {
            return ConflictResolutionPreview(
                path = parsed.file.path,
                originalContent = parsed.file.originalContent,
                resolvedContent = null,
                blockCount = parsed.file.blocks.size,
                malformed = false,
                unresolvedReason = "Needs manual review by choice.",
            )
        }

        val resolved = buildString {
            parsed.chunks.forEach { chunk ->
                when (chunk) {
                    is ConflictChunk.PlainText -> append(chunk.text)
                    is ConflictChunk.BlockChunk -> append(
                        resolveBlock(
                            block = chunk.block,
                            preset = preset,
                        ),
                    )
                }
            }
        }

        return ConflictResolutionPreview(
            path = parsed.file.path,
            originalContent = parsed.file.originalContent,
            resolvedContent = resolved,
            blockCount = parsed.file.blocks.size,
            malformed = false,
            unresolvedReason = null,
        )
    }

    private fun resolveBlock(block: ConflictBlock, preset: ConflictPreset): String = when (preset) {
        ConflictPreset.KEEP_CURRENT -> block.currentText
        ConflictPreset.KEEP_INCOMING -> block.incomingText
        ConflictPreset.KEEP_BOTH -> joinBoth(block.currentText, block.incomingText)
        ConflictPreset.REVIEW_MANUALLY -> ""
    }

    private fun joinBoth(current: String, incoming: String): String {
        if (current.isEmpty()) return incoming
        if (incoming.isEmpty()) return current
        val needsSeparator = !current.endsWith("\n") && !incoming.startsWith("\n")
        return if (needsSeparator) "$current\n$incoming" else current + incoming
    }
}
