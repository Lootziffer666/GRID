package com.grid.shared.registry

import kotlinx.serialization.Serializable

/**
 * Unique identifier for a feature module.
 */
@JvmInline
@Serializable
value class FeatureId(val value: String)

/**
 * A user-initiated intent directed at a feature module.
 */
data class UserIntent(
    val action: String,
    val extras: Map<String, String> = emptyMap(),
)

/**
 * Result of executing a feature operation.
 */
sealed class Outcome {
    data class Success(val message: String = "") : Outcome()
    data class Error(val reason: String, val cause: Throwable? = null) : Outcome()
    data object NavigateBack : Outcome()
}

/**
 * Contract that every feature module implements to register with the core shell.
 *
 * Feature modules provide this entry point so the Module Registry can discover,
 * invoke, and if necessary disable them at runtime.
 */
interface FeatureEntry {
    val id: FeatureId
    val displayName: String

    /**
     * Execute a user intent within this feature module.
     */
    suspend fun execute(intent: UserIntent): Outcome

    /**
     * Route identifier for navigation. Feature modules that have a UI
     * surface return their start route here. Returns null if the feature
     * has no navigation target.
     */
    fun navigationRoute(): String? = null
}
