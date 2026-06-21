package com.grid.shared.config

import kotlinx.serialization.json.Json
import java.io.File

/**
 * Loads [WorkflowTemplate] objects from embedded classpath resources and/or
 * an external file-system path.
 *
 * Supports hot-swap: call [loadFromExternal] to replace all templates at
 * runtime without restarting the app.
 */
class ConfigLoader(
    private val templateRegistry: TemplateRegistry,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {

    /**
     * Load templates from an embedded classpath resource (JSON array).
     * Typically called once at startup.
     */
    fun loadFromEmbedded(resourcePath: String) {
        val stream = this::class.java.classLoader?.getResourceAsStream(resourcePath)
            ?: return
        val content = stream.bufferedReader().use { it.readText() }
        val templates = json.decodeFromString<List<WorkflowTemplate>>(content)
        templateRegistry.replaceAll(templates)
    }

    /**
     * Load templates from an external file path (JSON array).
     * Used for hot-swap: replaces all currently loaded templates.
     */
    fun loadFromExternal(path: String) {
        val file = File(path)
        if (!file.exists() || !file.isFile) return
        val content = file.readText()
        val templates = json.decodeFromString<List<WorkflowTemplate>>(content)
        templateRegistry.replaceAll(templates)
    }
}
