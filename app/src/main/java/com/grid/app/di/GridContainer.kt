package com.grid.app.di

import com.grid.shared.config.ConfigLoader
import com.grid.shared.config.TemplateRegistry
import com.grid.shared.crash.CrashBoundary
import com.grid.shared.registry.ModuleRegistry

/**
 * Slim DI container for the app shell.
 *
 * Contains only infrastructure-level singletons: the module registry,
 * crash boundary, and config loader. No feature-level dependencies
 * (GitHub APIs, SAF readers, etc.) live here -- those belong in their
 * respective feature modules.
 */
class GridContainer {

    val moduleRegistry: ModuleRegistry by lazy { ModuleRegistry() }

    val crashBoundary: CrashBoundary by lazy { CrashBoundary(moduleRegistry) }

    val templateRegistry: TemplateRegistry by lazy { TemplateRegistry() }

    val configLoader: ConfigLoader by lazy { ConfigLoader(templateRegistry) }
}
