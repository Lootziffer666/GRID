package com.painkiller.domain.conflict

data class ConflictBlockRef(
    val filePath: String,
    val blockIndex: Int,
)

enum class ConflictDecision {
    KEEP_CURRENT,
    KEEP_INCOMING,
    KEEP_BOTH,
    REVIEW_MANUALLY,
}

data class ConflictCardUiModel(
    val ref: ConflictBlockRef,
    val filePath: String,
    val cardIndex: Int,
    val totalCards: Int,
    val currentTextPreview: String,
    val incomingTextPreview: String,
    val currentLabel: String?,
    val incomingLabel: String?,
    val selectedDecision: ConflictDecision,
    val safetyText: String,
)

data class ConflictReviewFile(
    val path: String,
    val chunks: List<ConflictChunk>,
    val blocks: List<ConflictBlock>,
)

data class ConflictReviewSession(
    val files: List<ConflictReviewFile>,
    val cards: List<ConflictCardUiModel>,
    val currentIndex: Int,
    val decisions: Map<ConflictBlockRef, ConflictDecision>,
    val malformedFiles: List<ConflictFile>,
    val filesWithoutCollisions: List<String>,
) {
    val totalCollisions: Int get() = cards.size
    val unresolvedCount: Int get() = decisions.values.count { it == ConflictDecision.REVIEW_MANUALLY }
    val keepCurrentCount: Int get() = decisions.values.count { it == ConflictDecision.KEEP_CURRENT }
    val keepIncomingCount: Int get() = decisions.values.count { it == ConflictDecision.KEEP_INCOMING }
    val keepBothCount: Int get() = decisions.values.count { it == ConflictDecision.KEEP_BOTH }
    val resolvedCount: Int get() = totalCollisions - unresolvedCount
    val manualCount: Int get() = unresolvedCount
    val canBuildResolvedPreview: Boolean get() = malformedFiles.isEmpty() && unresolvedCount == 0

    val currentCard: ConflictCardUiModel?
        get() = cards.getOrNull(currentIndex)
}

data class ConflictReviewPreviewFile(
    val path: String,
    val resolvedContent: String?,
    val unresolvedReason: String?,
)

data class ConflictReviewPreview(
    val files: List<ConflictReviewPreviewFile>,
    val totalCollisions: Int,
    val keepCurrentCount: Int,
    val keepIncomingCount: Int,
    val keepBothCount: Int,
    val manualCount: Int,
    val malformedCount: Int,
    val filesAffected: Int,
    val writeAllowed: Boolean,
    val summary: String,
)

object ConflictReviewSessionBuilder {

    fun create(files: List<ConflictSourceFile>): ConflictReviewSession {
        val reviewFiles = mutableListOf<ConflictReviewFile>()
        val cards = mutableListOf<ConflictCardUiModel>()
        val decisions = linkedMapOf<ConflictBlockRef, ConflictDecision>()
        val malformedFiles = mutableListOf<ConflictFile>()
        val filesWithoutCollisions = mutableListOf<String>()

        files.forEach { source ->
            when (val parsed = ConflictMarkerParser.parse(source.path, source.content)) {
                is ConflictParseResult.Success -> {
                    val file = ConflictReviewFile(
                        path = parsed.file.path,
                        chunks = parsed.chunks,
                        blocks = parsed.file.blocks,
                    )
                    reviewFiles += file
                    parsed.file.blocks.forEach { block ->
                        val ref = ConflictBlockRef(filePath = parsed.file.path, blockIndex = block.index)
                        decisions[ref] = ConflictDecision.REVIEW_MANUALLY
                        cards += ConflictCardUiModel(
                            ref = ref,
                            filePath = parsed.file.path,
                            cardIndex = cards.size + 1,
                            totalCards = 0,
                            currentTextPreview = block.currentText.take(PREVIEW_LIMIT),
                            incomingTextPreview = block.incomingText.take(PREVIEW_LIMIT),
                            currentLabel = block.currentLabel,
                            incomingLabel = block.incomingLabel,
                            selectedDecision = ConflictDecision.REVIEW_MANUALLY,
                            safetyText = "Nothing is written until preview and final confirmation.",
                        )
                    }
                }

                is ConflictParseResult.Malformed -> malformedFiles += parsed.file
                is ConflictParseResult.NoMarkers -> filesWithoutCollisions += source.path
            }
        }

        val totalCards = cards.size
        val normalizedCards = cards.mapIndexed { idx, card ->
            card.copy(totalCards = totalCards, cardIndex = idx + 1)
        }

        return ConflictReviewSession(
            files = reviewFiles,
            cards = normalizedCards,
            currentIndex = 0,
            decisions = decisions,
            malformedFiles = malformedFiles,
            filesWithoutCollisions = filesWithoutCollisions,
        )
    }

    private const val PREVIEW_LIMIT = 220
}

object ConflictReviewSessionReducer {

    fun decide(session: ConflictReviewSession, decision: ConflictDecision): ConflictReviewSession {
        val card = session.currentCard ?: return session
        val updated = session.decisions.toMutableMap().apply {
            put(card.ref, decision)
        }
        val updatedCards = session.cards.map { existing ->
            if (existing.ref == card.ref) existing.copy(selectedDecision = decision) else existing
        }
        return session.copy(decisions = updated, cards = updatedCards)
    }

    fun next(session: ConflictReviewSession): ConflictReviewSession {
        if (session.cards.isEmpty()) return session
        val nextIndex = (session.currentIndex + 1).coerceAtMost(session.cards.lastIndex)
        return session.copy(currentIndex = nextIndex)
    }

    fun previous(session: ConflictReviewSession): ConflictReviewSession {
        if (session.cards.isEmpty()) return session
        val prevIndex = (session.currentIndex - 1).coerceAtLeast(0)
        return session.copy(currentIndex = prevIndex)
    }
}

object ConflictReviewPreviewPlanner {

    fun buildPreview(session: ConflictReviewSession): ConflictReviewPreview {
        val files = session.files.map { file ->
            if (session.malformedFiles.any { it.path == file.path }) {
                return@map ConflictReviewPreviewFile(
                    path = file.path,
                    resolvedContent = null,
                    unresolvedReason = "Malformed collisions. Needs manual review.",
                )
            }

            val unresolved = file.blocks.firstOrNull { block ->
                val ref = ConflictBlockRef(filePath = file.path, blockIndex = block.index)
                session.decisions[ref] == null || session.decisions[ref] == ConflictDecision.REVIEW_MANUALLY
            }
            if (unresolved != null) {
                return@map ConflictReviewPreviewFile(
                    path = file.path,
                    resolvedContent = null,
                    unresolvedReason = "Unresolved collisions remain.",
                )
            }

            val resolved = buildString {
                file.chunks.forEach { chunk ->
                    when (chunk) {
                        is ConflictChunk.PlainText -> append(chunk.text)
                        is ConflictChunk.BlockChunk -> {
                            val ref = ConflictBlockRef(filePath = file.path, blockIndex = chunk.block.index)
                            val decision = session.decisions[ref] ?: ConflictDecision.REVIEW_MANUALLY
                            append(resolveByDecision(chunk.block, decision))
                        }
                    }
                }
            }
            ConflictReviewPreviewFile(path = file.path, resolvedContent = resolved, unresolvedReason = null)
        }

        val manualCount = session.manualCount
        val malformedCount = session.malformedFiles.size
        val writeAllowed = false

        val summary = when {
            session.totalCollisions == 0 -> "No collisions available for card review."
            malformedCount > 0 -> "Malformed collisions detected. Painkiller did not write any files."
            manualCount > 0 -> "$manualCount collisions still need a decision. Painkiller did not write any files."
            else -> "Collision preview generated from card decisions. Nothing is written yet."
        }

        return ConflictReviewPreview(
            files = files,
            totalCollisions = session.totalCollisions,
            keepCurrentCount = session.keepCurrentCount,
            keepIncomingCount = session.keepIncomingCount,
            keepBothCount = session.keepBothCount,
            manualCount = manualCount,
            malformedCount = malformedCount,
            filesAffected = files.count { it.resolvedContent != null },
            writeAllowed = writeAllowed,
            summary = summary,
        )
    }

    private fun resolveByDecision(block: ConflictBlock, decision: ConflictDecision): String = when (decision) {
        ConflictDecision.KEEP_CURRENT -> block.currentText
        ConflictDecision.KEEP_INCOMING -> block.incomingText
        ConflictDecision.KEEP_BOTH -> {
            val current = block.currentText
            val incoming = block.incomingText
            if (current.isEmpty()) incoming
            else if (incoming.isEmpty()) current
            else if (!current.endsWith("\n") && !incoming.startsWith("\n")) "$current\n$incoming"
            else current + incoming
        }

        ConflictDecision.REVIEW_MANUALLY -> ""
    }
}
