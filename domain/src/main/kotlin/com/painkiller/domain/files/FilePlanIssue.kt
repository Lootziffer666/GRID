package com.painkiller.domain.files

enum class FilePlanIssueCode {
    EMPTY_SOURCE,
    INVALID_TARGET_PATH,
    INVALID_SOURCE_PATH,
    DUPLICATE_REPO_PATH
}

data class FilePlanIssue(
    val code: FilePlanIssueCode,
    val message: String,
    val relatedPath: String? = null
)
