package com.grid.shared.registry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Central registry holding all feature modules.
 *
 * Thread-safe: backed by [ConcurrentHashMap]. Feature modules register
 * themselves on startup; the crash boundary can disable them at runtime.
 *
 * Exposes [activeModules] as a [StateFlow] so Compose UI can reactively
 * observe changes when modules are registered or disabled.
 */
class ModuleRegistry {

    private val active = ConcurrentHashMap<FeatureId, FeatureEntry>()
    private val disabled = ConcurrentHashMap<FeatureId, String>()

    private val _activeModules = MutableStateFlow<List<FeatureEntry>>(emptyList())

    /**
     * Observable snapshot of currently active modules. Emits a new list
     * whenever a module is registered or disabled.
     */
    val activeModules: StateFlow<List<FeatureEntry>> = _activeModules.asStateFlow()

    private val _disabledModules = MutableStateFlow<Map<FeatureId, String>>(emptyMap())

    /**
     * Observable snapshot of currently disabled modules with their reasons.
     */
    val disabledModules: StateFlow<Map<FeatureId, String>> = _disabledModules.asStateFlow()

    /**
     * Register a feature module. If the module was previously disabled,
     * the disabled record is cleared.
     */
    fun register(entry: FeatureEntry) {
        disabled.remove(entry.id)
        active[entry.id] = entry
        emitSnapshots()
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
        emitSnapshots()
    }

    /**
     * List all currently active feature entries.
     */
    fun listActive(): List<FeatureEntry> = active.values.toList()

    /**
     * List all disabled module IDs with their disable reasons.
     */
    fun listDisabled(): Map<FeatureId, String> = disabled.toMap()

    private fun emitSnapshots() {
        _activeModules.value = active.values.toList()
        _disabledModules.value = disabled.toMap()
    }
}
