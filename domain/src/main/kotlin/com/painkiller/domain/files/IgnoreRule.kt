package com.painkiller.domain.files

/**
 * Simple path-based ignore rule.
 */
data class IgnoreRule(
    val name: String,
    val folderName: String
) {
    fun matches(repoPath: String): Boolean {
        val normalized = repoPath.trim('/')
        if (normalized == folderName || normalized.startsWith("$folderName/")) return true
        return normalized.split('/').any { it == folderName }
    }
}
