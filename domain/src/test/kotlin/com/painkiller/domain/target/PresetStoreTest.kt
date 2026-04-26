package com.painkiller.domain.target

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PresetStoreTest {

    @Test
    fun inMemoryStore_restoresLastUsedTargetAndPreset() {
        val store = InMemoryRepoTargetPresetStore()
        val target = sampleTarget(path = "docs")
        val preset = samplePreset(target)

        store.saveLastUsedTarget(target)
        store.savePreset(preset)

        assertEquals(target, store.loadLastUsedTarget())
        assertEquals(preset, store.loadPreset())
    }

    @Test
    fun presetCodec_roundTripRepoTarget() {
        val target = sampleTarget(path = "release/notes")

        val encoded = PainkillerPresetCodec.encodeRepoTarget(target)
        val decoded = PainkillerPresetCodec.decodeRepoTarget(encoded)

        assertEquals(target, decoded)
    }

    @Test
    fun presetCodec_roundTripPreset() {
        val preset = samplePreset(sampleTarget(path = ""))

        val encoded = PainkillerPresetCodec.encodePreset(preset)
        val decoded = PainkillerPresetCodec.decodePreset(encoded)

        assertNotNull(encoded)
        assertEquals(preset, decoded)
    }

    private fun sampleTarget(path: String): RepoTarget {
        val targetPath = (TargetPath.fromRaw(path) as TargetPathValidationResult.Valid).targetPath
        return RepoTarget(
            owner = "openai",
            repo = "painkiller",
            branch = BranchTarget("main"),
            targetPath = targetPath
        )
    }

    private fun samplePreset(target: RepoTarget): PainkillerPreset =
        PainkillerPreset(
            id = "last-used",
            label = "Last Used",
            repoTarget = target,
            updatedAtEpochMillis = 1_714_000_000_000L
        )
}
