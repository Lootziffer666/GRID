package com.grid.shared.config

import java.util.concurrent.ConcurrentHashMap

/**
 * Holds loaded [WorkflowTemplate] instances. Thread-safe.
 *
 * Templates can be replaced atomically via [replaceAll] for hot-swap support.
 * Uses a volatile reference swap so concurrent readers never observe an empty
 * or partially-populated registry during replacement.
 */
class TemplateRegistry {

    @Volatile
    private var templates: Map<String, WorkflowTemplate> = emptyMap()

    /**
     * Retrieve a template by its [id]. Returns null if not loaded.
     */
    fun getById(id: String): WorkflowTemplate? = templates[id]

    /**
     * List all currently loaded templates.
     */
    fun listAll(): List<WorkflowTemplate> = templates.values.toList()

    /**
     * Replace all templates atomically. Used for hot-swap.
     *
     * Builds the new map fully before swapping the reference, so readers
     * always see either the old complete set or the new complete set.
     */
    fun replaceAll(newTemplates: List<WorkflowTemplate>) {
        val replacement = ConcurrentHashMap<String, WorkflowTemplate>(newTemplates.size)
        newTemplates.forEach { replacement[it.id] = it }
        templates = replacement
    }
}
