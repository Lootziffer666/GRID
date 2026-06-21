package com.grid.shared.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConfigLoaderTest {

    private lateinit var templateRegistry: TemplateRegistry
    private lateinit var configLoader: ConfigLoader

    @Before
    fun setUp() {
        templateRegistry = TemplateRegistry()
        configLoader = ConfigLoader(templateRegistry)
    }

    @Test
    fun `loadFromEmbedded returns failure for missing resource`() {
        val result = configLoader.loadFromEmbedded("nonexistent/path.json")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("not found"))
    }

    @Test
    fun `loadFromExternal returns failure for missing file`() {
        val result = configLoader.loadFromExternal("/tmp/definitely-not-a-real-config-file.json")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("not found"))
    }

    @Test
    fun `loadFromExternal returns failure for directory path`() {
        val result = configLoader.loadFromExternal("/tmp")
        assertTrue(result.isFailure)
    }

    @Test
    fun `loadFromEmbedded loads valid resource`() {
        // This test uses a classpath resource - create one in test resources
        val result = configLoader.loadFromEmbedded("test-templates.json")
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
        assertEquals(2, templateRegistry.listAll().size)
    }
}
