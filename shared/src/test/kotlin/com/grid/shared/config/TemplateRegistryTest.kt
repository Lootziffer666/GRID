package com.grid.shared.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicBoolean

class TemplateRegistryTest {

    private lateinit var registry: TemplateRegistry

    private val templateA = WorkflowTemplate(id = "a", displayName = "Template A", description = "Desc A")
    private val templateB = WorkflowTemplate(id = "b", displayName = "Template B", description = "Desc B")
    private val templateC = WorkflowTemplate(id = "c", displayName = "Template C", description = "Desc C")

    @Before
    fun setUp() {
        registry = TemplateRegistry()
    }

    @Test
    fun `getById returns null when registry is empty`() {
        assertNull(registry.getById("nonexistent"))
    }

    @Test
    fun `replaceAll makes templates retrievable`() {
        registry.replaceAll(listOf(templateA, templateB))
        assertNotNull(registry.getById("a"))
        assertEquals("Template A", registry.getById("a")!!.displayName)
        assertNotNull(registry.getById("b"))
    }

    @Test
    fun `listAll returns all loaded templates`() {
        registry.replaceAll(listOf(templateA, templateB, templateC))
        val all = registry.listAll()
        assertEquals(3, all.size)
    }

    @Test
    fun `replaceAll replaces previous templates`() {
        registry.replaceAll(listOf(templateA, templateB))
        registry.replaceAll(listOf(templateC))
        assertNull(registry.getById("a"))
        assertNull(registry.getById("b"))
        assertNotNull(registry.getById("c"))
        assertEquals(1, registry.listAll().size)
    }

    @Test
    fun `replaceAll is atomic - readers never see empty during swap`() {
        // Pre-populate the registry
        registry.replaceAll(listOf(templateA, templateB))

        val sawEmpty = AtomicBoolean(false)
        val iterations = 1000
        val barrier = CyclicBarrier(2)
        val latch = CountDownLatch(2)

        // Reader thread: continuously reads and checks for emptiness
        val reader = Thread {
            barrier.await()
            repeat(iterations) {
                val list = registry.listAll()
                if (list.isEmpty()) {
                    sawEmpty.set(true)
                }
            }
            latch.countDown()
        }

        // Writer thread: continuously replaces templates
        val writer = Thread {
            barrier.await()
            repeat(iterations) {
                registry.replaceAll(listOf(templateA, templateB, templateC))
            }
            latch.countDown()
        }

        reader.start()
        writer.start()
        latch.await()

        assertEquals(
            "Reader should never see an empty registry during replaceAll",
            false,
            sawEmpty.get(),
        )
    }
}
