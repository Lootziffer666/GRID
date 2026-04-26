package com.painkiller.domain.github

/**
 * Owner/repo/branch coordinates for a future upload. Pure value type.
 *
 * Validation here is intentionally narrow:
 *   - owner and repo cannot be blank
 *   - branch cannot be blank
 *   - whitespace-only segments are rejected
 *
 * Real GitHub naming rules are validated at the API boundary in later gates.
 */
data class RepoCoordinates(
    val owner: String,
    val repo: String,
    val branch: String,
) {
    init {
        require(owner.isNotBlank()) { "owner must not be blank" }
        require(repo.isNotBlank()) { "repo must not be blank" }
        require(branch.isNotBlank()) { "branch must not be blank" }
    }

    /** The fully qualified ref string (e.g. `heads/main`) used by the Git Data API. */
    val refPath: String
        get() = "heads/$branch"

    val fullName: String
        get() = "$owner/$repo"
}
