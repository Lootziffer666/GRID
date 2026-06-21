package com.grid.shared.registry

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ModuleRegistryStateFlowTest {

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
    fun `activeModules flow starts empty`() = runTest {
        assertTrue(registry.activeModules.value.isEmpty())
    }

    @Test
    fun `activeModules flow updates on register`() = runTest {
        registry.register(dummyEntry)
        val modules = registry.activeModules.value
        assertEquals(1, modules.size)
        assertEquals(FeatureId("test-module"), modules.first().id)
    }

    @Test
    fun `activeModules flow updates on disable`() = runTest {
        registry.register(dummyEntry)
        registry.register(secondEntry)
        assertEquals(2, registry.activeModules.value.size)

        registry.disable(FeatureId("test-module"), "crashed")
        assertEquals(1, registry.activeModules.value.size)
        assertEquals(FeatureId("second-module"), registry.activeModules.value.first().id)
    }

    @Test
    fun `disabledModules flow updates on disable`() = runTest {
        registry.register(dummyEntry)
        registry.disable(FeatureId("test-module"), "crash reason")

        val disabled = registry.disabledModules.value
        assertEquals(1, disabled.size)
        assertEquals("crash reason", disabled[FeatureId("test-module")])
    }

    @Test
    fun `disabledModules flow clears on re-register`() = runTest {
        registry.register(dummyEntry)
        registry.disable(FeatureId("test-module"), "was disabled")
        assertTrue(registry.disabledModules.value.isNotEmpty())

        registry.register(dummyEntry)
        assertTrue(registry.disabledModules.value.isEmpty())
        assertEquals(1, registry.activeModules.value.size)
    }
}
