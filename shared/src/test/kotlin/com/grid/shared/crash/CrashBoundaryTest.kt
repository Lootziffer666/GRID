package com.grid.shared.crash

import com.grid.shared.registry.FeatureEntry
import com.grid.shared.registry.FeatureId
import com.grid.shared.registry.ModuleRegistry
import com.grid.shared.registry.Outcome
import com.grid.shared.registry.UserIntent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CrashBoundaryTest {

    private lateinit var registry: ModuleRegistry
    private lateinit var boundary: CrashBoundary

    private val stableEntry = object : FeatureEntry {
        override val id = FeatureId("stable")
        override val displayName = "Stable Module"
        override suspend fun execute(intent: UserIntent): Outcome =
            Outcome.Success("ok")
    }

    private val crashingEntry = object : FeatureEntry {
        override val id = FeatureId("crasher")
        override val displayName = "Crashing Module"
        override suspend fun execute(intent: UserIntent): Outcome {
            throw RuntimeException("intentional crash")
        }
    }

    @Before
    fun setUp() {
        registry = ModuleRegistry()
        boundary = CrashBoundary(registry)
    }

    @Test
    fun `successful execution returns Result success`() = runTest {
        registry.register(stableEntry)
        val result = boundary.executeFeature(
            FeatureId("stable"),
            UserIntent(action = "test"),
        )
        assertTrue(result.isSuccess)
        assertEquals(Outcome.Success("ok"), result.getOrNull())
    }

    @Test
    fun `crashing module returns Result failure`() = runTest {
        registry.register(crashingEntry)
        val result = boundary.executeFeature(
            FeatureId("crasher"),
            UserIntent(action = "test"),
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }

    @Test
    fun `crashing module gets disabled`() = runTest {
        registry.register(crashingEntry)
        boundary.executeFeature(FeatureId("crasher"), UserIntent(action = "test"))

        assertNull(registry.get(FeatureId("crasher")))
        val disabled = registry.listDisabled()
        assertTrue(disabled.containsKey(FeatureId("crasher")))
        assertTrue(disabled[FeatureId("crasher")]!!.contains("intentional crash"))
    }

    @Test
    fun `stable module survives after another module crashes`() = runTest {
        registry.register(stableEntry)
        registry.register(crashingEntry)

        boundary.executeFeature(FeatureId("crasher"), UserIntent(action = "test"))

        // Stable module still works
        assertNotNull(registry.get(FeatureId("stable")))
        val result = boundary.executeFeature(
            FeatureId("stable"),
            UserIntent(action = "test"),
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun `non-registered module returns failure`() = runTest {
        val result = boundary.executeFeature(
            FeatureId("nonexistent"),
            UserIntent(action = "test"),
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
