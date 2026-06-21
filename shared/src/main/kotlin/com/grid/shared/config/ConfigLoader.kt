package com.grid.shared.config

import kotlinx.serialization.json.Json
import java.io.File

/**
 * Loads [WorkflowTemplate] objects from embedded classpath resources and/or
 * an external file-system path.
 *
 * Supports hot-swap: call [loadFromExternal] to replace all templates at
 * runtime without restarting the app.
 *
 * Returns [Result] from each load operation so callers can detect and handle
 * configuration failures (missing resource, malformed JSON, etc.).
 */
class ConfigLoader(
    private val templateRegistry: TemplateRegistry,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {

    /**
     * Load templates from an embedded classpath resource (JSON array).
     * Typically called once at startup.
     *
     * @return [Result.success] with the number of templates loaded, or
     *         [Result.failure] if the resource is missing or malformed.
     */
    fun loadFromEmbedded(resourcePath: String): Result<Int> {
        val stream = this::class.java.classLoader?.getResourceAsStream(resourcePath)
            ?: return Result.failure(
                IllegalStateException("Embedded resource not found: $resourcePath")
            )
        return runCatching {
            val content = stream.bufferedReader().use { it.readText() }
            val templates = json.decodeFromString<List<WorkflowTemplate>>(content)
            templateRegistry.replaceAll(templates)
            templates.size
        }
    }

    /**
     * Load templates from an external file path (JSON array).
     * Used for hot-swap: replaces all currently loaded templates.
     *
     * @return [Result.success] with the number of templates loaded, or
     *         [Result.failure] if the file is missing or malformed.
     */
    fun loadFromExternal(path: String): Result<Int> {
        val file = File(path)
        if (!file.exists() || !file.isFile) {
            return Result.failure(
                IllegalStateException("External config file not found or not a file: $path")
            )
        }
        return runCatching {
            val content = file.readText()
            val templates = json.decodeFromString<List<WorkflowTemplate>>(content)
            templateRegistry.replaceAll(templates)
            templates.size
        }
    }
}
