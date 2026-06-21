package com.grid.app

import android.app.Application
import android.util.Log
import com.grid.app.di.GridContainer
import com.grid.shared.registry.FeatureEntry
import java.util.ServiceLoader

class GridApplication : Application() {

    /**
     * Lazily initialised DI container for the shell layer. Held by the
     * [Application] so it survives configuration changes and is shared
     * by every Activity / ViewModel.
     *
     * Feature modules access the [ModuleRegistry] and [CrashBoundary]
     * through this container to register themselves.
     */
    val container: GridContainer by lazy { GridContainer() }

    override fun onCreate() {
        super.onCreate()
        discoverAndRegisterFeatureModules()
    }

    /**
     * Uses [ServiceLoader] to discover all [FeatureEntry] implementations
     * declared in META-INF/services files across all installed modules.
     *
     * Each discovered entry is registered with the [ModuleRegistry]. This
     * allows dynamic feature modules to self-declare their entry point
     * without the :app module needing a compile-time dependency on them.
     */
    private fun discoverAndRegisterFeatureModules() {
        val loader = ServiceLoader.load(FeatureEntry::class.java)
        for (entry in loader) {
            try {
                container.moduleRegistry.register(entry)
                Log.i(TAG, "Registered feature module: ${entry.displayName} (${entry.id.value})")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register feature module: ${entry.id.value}", e)
            }
        }
        val count = container.moduleRegistry.listActive().size
        Log.i(TAG, "Module discovery complete: $count module(s) registered")
    }

    companion object {
        private const val TAG = "GridApplication"
    }
}
