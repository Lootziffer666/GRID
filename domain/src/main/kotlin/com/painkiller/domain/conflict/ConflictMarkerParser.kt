package com.painkiller.domain.conflict

object ConflictMarkerParser {

    fun parse(path: String, content: String): ConflictParseResult {
        val lines = splitPreservingLineEndings(content)
        if (lines.isEmpty()) {
            return ConflictParseResult.NoMarkers(path = path, originalContent = content)
        }

        val chunks = mutableListOf<ConflictChunk>()
        val issues = mutableListOf<String>()
        var index = 0
        var blockIndex = 0

        while (index < lines.size) {
            val line = lines[index]
            if (!line.startsWith("<<<<<<<")) {
                chunks += ConflictChunk.PlainText(line)
                index += 1
                continue
            }

            val currentLabel = line.removePrefix("<<<<<<<").trim().ifBlank { null }
            index += 1
            val currentBuilder = StringBuilder()
            var sawSeparator = false

            while (index < lines.size) {
                val candidate = lines[index]
                if (candidate.startsWith("<<<<<<<")) {
                    issues += "Nested conflict marker found before separator."
                    return malformed(path, content, issues)
                }
                if (candidate.startsWith(">>>>>>>")) {
                    issues += "End marker found before separator."
                    return malformed(path, content, issues)
                }
                if (candidate.startsWith("=======")) {
                    sawSeparator = true
                    index += 1
                    break
                }
                currentBuilder.append(candidate)
                index += 1
            }

            if (!sawSeparator) {
                issues += "Missing separator marker (=======)."
                return malformed(path, content, issues)
            }

            val incomingBuilder = StringBuilder()
            var incomingLabel: String? = null
            var sawEnd = false

            while (index < lines.size) {
                val candidate = lines[index]
                if (candidate.startsWith("<<<<<<<")) {
                    issues += "Nested conflict marker found in incoming block."
                    return malformed(path, content, issues)
                }
                if (candidate.startsWith("=======")) {
                    issues += "Unexpected separator marker inside incoming block."
                    return malformed(path, content, issues)
                }
                if (candidate.startsWith(">>>>>>>")) {
                    incomingLabel = candidate.removePrefix(">>>>>>>").trim().ifBlank { null }
                    index += 1
                    sawEnd = true
                    break
                }
                incomingBuilder.append(candidate)
                index += 1
            }

            if (!sawEnd) {
                issues += "Missing end marker (>>>>>>>)."
                return malformed(path, content, issues)
            }

            val block = ConflictBlock(
                index = blockIndex,
                currentText = currentBuilder.toString(),
                incomingText = incomingBuilder.toString(),
                currentLabel = currentLabel,
                incomingLabel = incomingLabel,
            )
            chunks += ConflictChunk.BlockChunk(block)
            blockIndex += 1
        }

        val blocks = chunks.mapNotNull { chunk -> (chunk as? ConflictChunk.BlockChunk)?.block }
        if (blocks.isEmpty()) {
            return ConflictParseResult.NoMarkers(path = path, originalContent = content)
        }

        return ConflictParseResult.Success(
            file = ConflictFile(path = path, originalContent = content, blocks = blocks),
            chunks = chunks,
        )
    }

    private fun malformed(path: String, content: String, issues: List<String>): ConflictParseResult {
        return ConflictParseResult.Malformed(
            file = ConflictFile(
                path = path,
                originalContent = content,
                blocks = emptyList(),
                parseIssues = issues,
            ),
        )
    }

    private fun splitPreservingLineEndings(content: String): List<String> {
        if (content.isEmpty()) return emptyList()
        return Regex("(?<=\\n)").split(content)
    }
}
