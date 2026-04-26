package com.painkiller.domain.files

import com.painkiller.domain.path.PathValidation

object FilePlanBuilder {

    fun build(
        selectedSource: SelectedSource,
        targetPathRaw: String,
        ignoreRules: List<IgnoreRule> = DefaultIgnoreRules.rules
    ): FilePlanBuildResult {
        val issues = mutableListOf<FilePlanIssue>()

        if (selectedSource.items.isEmpty()) {
            issues += FilePlanIssue(
                code = FilePlanIssueCode.EMPTY_SOURCE,
                message = "At least one source file is required."
            )
            return FilePlanBuildResult.ValidationError(issues)
        }

        val normalizedTargetPath = PathValidation.normalizeRepoPath(targetPathRaw)
            ?: return FilePlanBuildResult.ValidationError(
                listOf(
                    FilePlanIssue(
                        code = FilePlanIssueCode.INVALID_TARGET_PATH,
                        message = "Target path is unsafe or invalid.",
                        relatedPath = targetPathRaw
                    )
                )
            )

        val sortedItems = selectedSource.items.sortedWith(
            compareBy<SelectedSourceItem>({ it.relativePath ?: it.displayName }, { it.sourceId })
        )

        val includedFiles = mutableListOf<PlannedFile>()
        val ignoredFiles = mutableListOf<PlannedFile>()
        val seenRepoPaths = linkedSetOf<String>()

        for (item in sortedItems) {
            val sourceRelative = (item.relativePath ?: item.displayName).trim()
            val normalizedRelative = PathValidation.normalizeRepoPath(sourceRelative)
            if (normalizedRelative == null || normalizedRelative.isBlank()) {
                issues += FilePlanIssue(
                    code = FilePlanIssueCode.INVALID_SOURCE_PATH,
                    message = "Source path is unsafe or invalid.",
                    relatedPath = sourceRelative
                )
                continue
            }

            val rawRepoPath = if (normalizedTargetPath.isBlank()) {
                normalizedRelative
            } else {
                "$normalizedTargetPath/$normalizedRelative"
            }

            val normalizedRepoPath = PathValidation.normalizeRepoPath(rawRepoPath)
            if (normalizedRepoPath == null || normalizedRepoPath.isBlank()) {
                issues += FilePlanIssue(
                    code = FilePlanIssueCode.INVALID_SOURCE_PATH,
                    message = "Planned repository path is unsafe.",
                    relatedPath = rawRepoPath
                )
                continue
            }

            if (!seenRepoPaths.add(normalizedRepoPath)) {
                issues += FilePlanIssue(
                    code = FilePlanIssueCode.DUPLICATE_REPO_PATH,
                    message = "Duplicate normalized repository path detected.",
                    relatedPath = normalizedRepoPath
                )
                continue
            }

            val ignoreRule = ignoreRules.firstOrNull { rule -> rule.matches(normalizedRepoPath) }
            val planned = PlannedFile(
                sourceId = item.sourceId,
                sourceDisplayName = item.displayName,
                sourceRelativePath = normalizedRelative,
                repoPath = normalizedRepoPath,
                sizeBytes = item.sizeBytes,
                mimeType = item.mimeType,
                ignoredByRule = ignoreRule
            )

            if (ignoreRule == null) {
                includedFiles += planned
            } else {
                ignoredFiles += planned
            }
        }

        val plan = FilePlan(
            sourceKind = selectedSource.kind,
            targetPath = normalizedTargetPath,
            includedFiles = includedFiles,
            ignoredFiles = ignoredFiles,
            issues = issues
        )

        return FilePlanBuildResult.Success(plan)
    }
}
