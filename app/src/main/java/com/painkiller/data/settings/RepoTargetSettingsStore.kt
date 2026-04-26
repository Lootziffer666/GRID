package com.painkiller.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.painkiller.domain.target.PainkillerPreset
import com.painkiller.domain.target.PainkillerPresetCodec
import com.painkiller.domain.target.RepoTarget
import kotlinx.coroutines.flow.firstOrNull

interface RepoTargetSettingsStore {
    suspend fun loadLastUsedTarget(): RepoTarget?
    suspend fun saveLastUsedTarget(repoTarget: RepoTarget)

    suspend fun loadPreset(): PainkillerPreset?
    suspend fun savePreset(preset: PainkillerPreset)
}

class DataStoreRepoTargetSettingsStore(
    private val dataStore: DataStore<Preferences>
) : RepoTargetSettingsStore {

    override suspend fun loadLastUsedTarget(): RepoTarget? {
        val raw = dataStore.data.firstOrNull()?.get(LAST_USED_TARGET_KEY) ?: return null
        return runCatching { PainkillerPresetCodec.decodeRepoTarget(raw) }.getOrNull()
    }

    override suspend fun saveLastUsedTarget(repoTarget: RepoTarget) {
        val encoded = PainkillerPresetCodec.encodeRepoTarget(repoTarget)
        dataStore.edit { preferences ->
            preferences[LAST_USED_TARGET_KEY] = encoded
        }
    }

    override suspend fun loadPreset(): PainkillerPreset? {
        val raw = dataStore.data.firstOrNull()?.get(PRESET_KEY) ?: return null
        return runCatching { PainkillerPresetCodec.decodePreset(raw) }.getOrNull()
    }

    override suspend fun savePreset(preset: PainkillerPreset) {
        val encoded = PainkillerPresetCodec.encodePreset(preset)
        dataStore.edit { preferences ->
            preferences[PRESET_KEY] = encoded
        }
    }

    private companion object {
        val LAST_USED_TARGET_KEY = stringPreferencesKey("last_used_repo_target")
        val PRESET_KEY = stringPreferencesKey("repo_target_preset")
    }
}
