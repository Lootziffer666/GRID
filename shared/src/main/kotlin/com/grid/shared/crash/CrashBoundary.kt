package com.grid.shared.crash

import com.grid.shared.registry.FeatureId
import com.grid.shared.registry.ModuleRegistry
import com.grid.shared.registry.Outcome
import com.grid.shared.registry.UserIntent

/**
 * Wraps every feature module invocation in [runCatching].
 *
 * If a module throws, it is automatically disabled via [ModuleRegistry.disable]
 * and the failure is returned as [Result.failure]. The rest of the application
 * continues to operate normally.
 */
class CrashBoundary(private val registry: ModuleRegistry) {

    /**
     * Execute a [UserIntent] against the module identified by [id].
     *
     * - If the module is not registered/active, returns [Result.failure] with
     *   [IllegalStateException].
     * - If the module throws, it is disabled and [Result.failure] is returned.
     * - On success, returns [Result.success] with the [Outcome].
     */
    suspend fun executeFeature(id: FeatureId, intent: UserIntent): Result<Outcome> {
        val entry = registry.get(id)
            ?: return Result.failure(
                IllegalStateException("Module ${id.value} is not registered or has been disabled")
            )

        return runCatching {
            entry.execute(intent)
        }.onFailure { throwable ->
            registry.disable(id, "Crash: ${throwable.message ?: throwable::class.simpleName}")
        }
    }
}
