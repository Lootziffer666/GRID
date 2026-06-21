package com.grid.app

import com.painkiller.domain.github.RepoCoordinates
import com.grid.shared.registry.FeatureId
import com.grid.shared.registry.ModuleRegistry
import com.grid.shared.registry.FeatureEntry
import com.grid.shared.registry.Outcome
import com.grid.shared.registry.UserIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * App-side smoke test. Confirms the :domain and :shared modules are wired
 * into :app. Domain test coverage lives in the :domain module; shared module
 * tests live in :shared.
 */
class SmokeTest {

    @Test
    fun domainTypesReachable() {
        val c = RepoCoordinates(owner = "octocat", repo = "hello-world", branch = "main")
        assertEquals("octocat/hello-world", c.fullName)
    }

    @Test
    fun moduleRegistryReachable() {
        val registry = ModuleRegistry()
        val entry = object : FeatureEntry {
            override val id = FeatureId("test")
            override val displayName = "Test"
            override suspend fun execute(intent: UserIntent) = Outcome.Success("ok")
        }
        registry.register(entry)
        assertNotNull(registry.get(FeatureId("test")))
    }
}
