package com.grid.feature.import_pipeline

import com.grid.shared.registry.FeatureEntry
import com.grid.shared.registry.FeatureId
import com.grid.shared.registry.Outcome
import com.grid.shared.registry.UserIntent

/**
 * Feature entry point for the Import Pipeline module.
 *
 * This registers the import/upload feature with the Module Registry,
 * proving the dynamic feature registration pattern works. The actual
 * composable UI is wired through the feature module's internal navigation.
 */
class ImportFeatureEntry : FeatureEntry {

    override val id: FeatureId = FeatureId("import-pipeline")

    override val displayName: String = "Import Pipeline"

    override suspend fun execute(intent: UserIntent): Outcome {
        return when (intent.action) {
            "start" -> Outcome.Success("Import pipeline ready")
            else -> Outcome.Error("Unknown action: ${intent.action}")
        }
    }

    override fun navigationRoute(): String = "import"
}
