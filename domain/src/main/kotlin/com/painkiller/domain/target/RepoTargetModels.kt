package com.painkiller.domain.target

import com.painkiller.domain.path.PathValidation
import kotlinx.serialization.Serializable

@Serializable
data class BranchTarget(
    val name: String
) {
    init {
        require(name.isNotBlank()) { "branch must not be blank" }
    }
}

@Serializable
data class TargetPath(
    val normalized: String
) {
    init {
        require(PathValidation.normalizeRepoPath(normalized) == normalized) {
            "target path must be normalized and safe"
        }
    }

    companion object {
        fun fromRaw(raw: String): TargetPathValidationResult {
            val normalized = PathValidation.normalizeRepoPath(raw)
                ?: return TargetPathValidationResult.Invalid(
                    message = "Target path is invalid: absolute paths and traversal are not allowed.",
                    rawInput = raw
                )

            return TargetPathValidationResult.Valid(TargetPath(normalized = normalized))
        }
    }
}

sealed interface TargetPathValidationResult {
    data class Valid(val targetPath: TargetPath) : TargetPathValidationResult
    data class Invalid(val message: String, val rawInput: String) : TargetPathValidationResult
}

@Serializable
data class RepoTarget(
    val owner: String,
    val repo: String,
    val branch: BranchTarget,
    val targetPath: TargetPath
) {
    init {
        require(owner.isNotBlank()) { "owner must not be blank" }
        require(repo.isNotBlank()) { "repo must not be blank" }
    }

    val fullName: String
        get() = "$owner/$repo"
}

@Serializable
data class PainkillerPreset(
    val id: String,
    val label: String,
    val repoTarget: RepoTarget,
    val updatedAtEpochMillis: Long
) {
    init {
        require(id.isNotBlank()) { "preset id must not be blank" }
        require(label.isNotBlank()) { "preset label must not be blank" }
        require(updatedAtEpochMillis >= 0L) { "updatedAtEpochMillis must be >= 0" }
    }
}
