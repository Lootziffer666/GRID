package com.painkiller.domain.conflict

object ConflictBranchFreshnessGuard {
    fun isStale(plannedBranchSha: String?, currentBranchSha: String?): Boolean {
        if (plannedBranchSha.isNullOrBlank() || currentBranchSha.isNullOrBlank()) return false
        return plannedBranchSha != currentBranchSha
    }
}
