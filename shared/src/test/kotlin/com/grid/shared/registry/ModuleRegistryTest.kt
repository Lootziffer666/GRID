package com.grid.shared.registry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ModuleRegistryTest {

    private lateinit var registry: ModuleRegistry

    private val dummyEntry = object : FeatureEntry {
        override val id = FeatureId("test-module")
        override val displayName = "Test Module"
        override suspend fun execute(intent: UserIntent): Outcome =
            Outcome.Success("executed")
    }

    private val secondEntry = object : FeatureEntry {
        override val id = FeatureId("second-module")
        override val displayName = "Second Module"
        override suspend fun execute(intent: UserIntent): Outcome =
            Outcome.Success("second")
    }

    @Before
    fun setUp() {
        registry = ModuleRegistry()
    }

    @Test
    fun `register makes module retrievable`() {
        registry.register(dummyEntry)
        val result = registry.get(FeatureId("test-module"))
        assertNotNull(result)
        assertEquals("Test Module", result!!.displayName)
    }

    @Test
    fun `get returns null for unknown id`() {
        val result = registry.get(FeatureId("nonexistent"))
        assertNull(result)
    }

    @Test
    fun `disable removes module from active set`() {
        registry.register(dummyEntry)
        registry.disable(FeatureId("test-module"), "crash")
        assertNull(registry.get(FeatureId("test-module")))
    }

    @Test
    fun `disable records reason`() {
        registry.register(dummyEntry)
        registry.disable(FeatureId("test-module"), "test crash reason")
        val disabled = registry.listDisabled()
        assertEquals("test crash reason", disabled[FeatureId("test-module")])
    }

    @Test
    fun `listActive returns all registered modules`() {
        registry.register(dummyEntry)
        registry.register(secondEntry)
        val active = registry.listActive()
        assertEquals(2, active.size)
        assertTrue(active.any { it.id == FeatureId("test-module") })
        assertTrue(active.any { it.id == FeatureId("second-module") })
    }

    @Test
    fun `listActive excludes disabled modules`() {
        registry.register(dummyEntry)
        registry.register(secondEntry)
        registry.disable(FeatureId("test-module"), "removed")
        val active = registry.listActive()
        assertEquals(1, active.size)
        assertEquals(FeatureId("second-module"), active.first().id)
    }

    @Test
    fun `re-register clears disabled state`() {
        registry.register(dummyEntry)
        registry.disable(FeatureId("test-module"), "was disabled")
        assertTrue(registry.listDisabled().containsKey(FeatureId("test-module")))

        registry.register(dummyEntry)
        assertNotNull(registry.get(FeatureId("test-module")))
        assertTrue(registry.listDisabled().isEmpty())
    }
}
