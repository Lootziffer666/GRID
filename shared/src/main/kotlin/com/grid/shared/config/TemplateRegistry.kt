package com.grid.shared.config

import java.util.concurrent.ConcurrentHashMap

/**
 * Holds loaded [WorkflowTemplate] instances. Thread-safe.
 *
 * Templates can be replaced atomically via [replaceAll] for hot-swap support.
 */
class TemplateRegistry {

    private val templates = ConcurrentHashMap<String, WorkflowTemplate>()

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
     */
    fun replaceAll(newTemplates: List<WorkflowTemplate>) {
        templates.clear()
        newTemplates.forEach { templates[it.id] = it }
    }
}
