package com.grid.shared.config

import kotlinx.serialization.Serializable

/**
 * A workflow template definition loaded from JSON config.
 *
 * Templates describe available workflows that feature modules can execute.
 */
@Serializable
data class WorkflowTemplate(
    val id: String,
    val displayName: String,
    val description: String = "",
)
