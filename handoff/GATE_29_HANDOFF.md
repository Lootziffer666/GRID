# Gate 29 Handoff

## Status

PARTIAL

## Gate Scope

- Implement Gate 29 only: Codex Conflict Presets MVP.
- Build parser + preset resolver + preview-first UX.
- Keep write/commit/push disabled unless a safe write path exists.

## Conflict Support Audit Result

- PR merge-assist code: **Existing but unrelated** (API merge action; not file conflict-marker parsing).
- SHA mismatch/error mapper: **Existing and reusable as safety precedent**, but not direct parser logic reuse.
- File intake abstractions (`SafFileReader`, folder/ZIP intake): **Existing and reusable for read-only preview input**.
- Diff/merge utilities: **Missing** (no conflict marker parser existed).
- UI cards/banners/components: **Existing and reusable** for preset messaging and preview output.
- Conflict preset UX: **Missing** before this gate.
- Safe write-back path for selected conflict files: **Missing / dangerous to guess** (no verified SAF write flow for this gate scope).

## Implemented

- Added pure domain conflict models:
  - `ConflictSourceFile`, `ConflictBlock`, `ConflictFile`, `ConflictPreset`
  - `ConflictResolutionPreview`, `ConflictResolutionPlan`
  - parse/chunk result contracts (`ConflictParseResult`, `ConflictChunk`)
- Added `ConflictMarkerParser`:
  - parses standard Git markers (`<<<<<<<`, `=======`, `>>>>>>>`)
  - supports multiple blocks per file
  - preserves non-conflict content exactly
  - fails safely on malformed/nested markers
- Added `ConflictPresetPlanner`:
  - applies presets in memory (`KEEP_CURRENT`, `KEEP_INCOMING`, `KEEP_BOTH`, `REVIEW_MANUALLY`)
  - builds summary + per-file preview
  - preview-only (`writeAllowed=false`)
- Added UploadFlow MVP UI wiring:
  - new “Codex collision cleanup (MVP)” section
  - default preset: keep current
  - preset selector buttons
  - explicit preview action
  - preview summary + snippet
  - write button shown but disabled with Gate 29 label
- Added ViewModel support:
  - preset selection
  - in-memory preview plan generation from selected source data
  - no write/commit/push path added

## Parser / Preview / Write-back Status

- Parser works: YES
- Preview works: YES
- Write-back works: NO (intentionally blocked in Gate 29)

## Preset Behavior (Exact)

- `KEEP_CURRENT`: conflict blocks replaced by current block text.
- `KEEP_INCOMING`: conflict blocks replaced by incoming block text.
- `KEEP_BOTH`: current + incoming concatenation, with a newline inserted only when needed to avoid line-joining.
- `REVIEW_MANUALLY`: no auto-resolution output for those files.
- Malformed markers: blocked/manual; no resolved output generated.

## Unsupported Paths

- No SAF write-back yet.
- No auto-commit.
- No auto-push.
- No AI merge suggestions.
- No conflict cards/swipe UX.
- No PR/auth/LFS/release scope expansion.

## Files Changed

- `domain/src/main/kotlin/com/painkiller/domain/conflict/ConflictModels.kt`
- `domain/src/main/kotlin/com/painkiller/domain/conflict/ConflictMarkerParser.kt`
- `domain/src/main/kotlin/com/painkiller/domain/conflict/ConflictPresetPlanner.kt`
- `domain/src/test/kotlin/com/painkiller/domain/conflict/ConflictPresetPlannerTest.kt`
- `app/src/main/java/com/painkiller/ui/flow/UploadFlowViewModel.kt`
- `app/src/main/java/com/painkiller/ui/flow/UploadFlowScreen.kt`
- `README.md`
- `knownbugs.md`
- `handoff/GATE_29_HANDOFF.md`

## Checks Run

- command: `./gradlew :domain:test :domain:build`
- result: PASS

- command: `./gradlew --no-daemon :app:testDebugUnitTest`
- result: local SDK missing (`SDK location not found`), CI remains authoritative per CI-first policy.

## CI Status

- workflow: `.github/workflows/build.yml`
- result: pending push

## Known Risks

- Parser currently treats nested markers as malformed and blocks bulk preset (intentional safety behavior).
- Preview UI currently shows first resolved-file snippet only (minimal MVP scope).

## Explicitly Not Done

- No write-back implementation.
- No commit/push integration.
- No swipe conflict cards.
- No full conflict editor.
- No AI/autonomous merge behavior.
- No Gate 30+ functionality.

## Next Gate May Start Only If

- CI validates Android build green, or user explicitly accepts CI-first progression.
