package com.grid.app

import android.app.Application
import com.grid.app.di.GridContainer

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
}
