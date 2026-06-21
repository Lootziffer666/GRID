package com.grid.shared.registry

import java.util.concurrent.ConcurrentHashMap

/**
 * Central registry holding all feature modules.
 *
 * Thread-safe: backed by [ConcurrentHashMap]. Feature modules register
 * themselves on startup; the crash boundary can disable them at runtime.
 */
class ModuleRegistry {

    private val active = ConcurrentHashMap<FeatureId, FeatureEntry>()
    private val disabled = ConcurrentHashMap<FeatureId, String>()

    /**
     * Register a feature module. If the module was previously disabled,
     * the disabled record is cleared.
     */
    fun register(entry: FeatureEntry) {
        disabled.remove(entry.id)
        active[entry.id] = entry
    }

    /**
     * Retrieve an active module by its [FeatureId]. Returns null if not
     * registered or currently disabled.
     */
    fun get(id: FeatureId): FeatureEntry? = active[id]

    /**
     * Disable a module, removing it from the active set and recording the reason.
     */
    fun disable(id: FeatureId, reason: String) {
        active.remove(id)
        disabled[id] = reason
    }

    /**
     * List all currently active feature entries.
     */
    fun listActive(): List<FeatureEntry> = active.values.toList()

    /**
     * List all disabled module IDs with their disable reasons.
     */
    fun listDisabled(): Map<FeatureId, String> = disabled.toMap()
}
