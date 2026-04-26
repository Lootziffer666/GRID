# Gate 4 Handoff

## Status

PARTIAL

## Gate Scope

- Implement Gate 4 only: RepoTarget + branch/target-path selection + presets.
- Store non-secret settings only.
- No upload/commit/push behavior.

## Implemented

- Added Gate 4 domain target models:
  - `BranchTarget`
  - `TargetPath`
  - `TargetPathValidationResult`
  - `RepoTarget`
  - `PainkillerPreset`
- `TargetPath` validation now uses existing `PathValidation` and returns explicit validation results.
- Added preset storage contracts/codecs in `:domain`:
  - `RepoTargetPresetStore`
  - `InMemoryRepoTargetPresetStore`
  - `PainkillerPresetCodec` for serialization.
- Added DataStore-backed app settings store for non-secret selection data:
  - `RepoTargetSettingsStore`
  - `DataStoreRepoTargetSettingsStore`
  - Stores and restores last-used `RepoTarget` and optional `PainkillerPreset`.
- Added Gate 4 domain tests:
  - `TargetPathValidationTest`
  - `PresetStoreTest`
- Updated README and gate status references.

## Files Changed

- `README.md`
- `knownbugs.md`
- `handoff/GATE_4_HANDOFF.md`
- `domain/src/main/kotlin/com/painkiller/domain/target/RepoTargetModels.kt`
- `domain/src/main/kotlin/com/painkiller/domain/target/PresetStore.kt`
- `domain/src/test/kotlin/com/painkiller/domain/target/TargetPathValidationTest.kt`
- `domain/src/test/kotlin/com/painkiller/domain/target/PresetStoreTest.kt`
- `app/src/main/java/com/painkiller/data/settings/RepoTargetSettingsStore.kt`
- `app/src/main/java/com/painkiller/data/settings/PlaceholderSettings.kt`
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`

## Checks Run

- command: `./gradlew :domain:test`
  - result: PASS (`BUILD SUCCESSFUL`)
- command: `./gradlew :domain:build`
  - result: PASS (`BUILD SUCCESSFUL`)
- command: `./gradlew :app:assembleDebug`
  - result: BLOCKED in this local container (`SDK location not found`)

## Known Bugs / Risks

- `BUG-20260426-007` (OPEN): local container lacks Android SDK, so Gate 4 app assembly is not verifiable here.

## Explicitly Not Done

- No upload implementation.
- No commit creation.
- No push/update-ref behavior.
- No GitHub write behavior.
- No token behavior changes.

## Next Gate May Start Only If

- Gate 4 is promoted from `PARTIAL` to `PASS` after Android SDK-backed app assembly check is green in CI or SDK-enabled runner.
- Then Gate 5 may begin.
