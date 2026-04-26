package com.painkiller.domain.target

import kotlinx.serialization.json.Json

interface RepoTargetPresetStore {
    fun loadLastUsedTarget(): RepoTarget?
    fun saveLastUsedTarget(repoTarget: RepoTarget)

    fun loadPreset(): PainkillerPreset?
    fun savePreset(preset: PainkillerPreset)
}

class InMemoryRepoTargetPresetStore : RepoTargetPresetStore {
    private var lastUsedTarget: RepoTarget? = null
    private var preset: PainkillerPreset? = null

    override fun loadLastUsedTarget(): RepoTarget? = lastUsedTarget

    override fun saveLastUsedTarget(repoTarget: RepoTarget) {
        lastUsedTarget = repoTarget
    }

    override fun loadPreset(): PainkillerPreset? = preset

    override fun savePreset(preset: PainkillerPreset) {
        this.preset = preset
    }
}

object PainkillerPresetCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeRepoTarget(repoTarget: RepoTarget): String =
        json.encodeToString(RepoTarget.serializer(), repoTarget)

    fun decodeRepoTarget(raw: String): RepoTarget =
        json.decodeFromString(RepoTarget.serializer(), raw)

    fun encodePreset(preset: PainkillerPreset): String =
        json.encodeToString(PainkillerPreset.serializer(), preset)

    fun decodePreset(raw: String): PainkillerPreset =
        json.decodeFromString(PainkillerPreset.serializer(), raw)
}
