# Gate 31 Handoff

## Status

PASS

## Gate Scope

- Implement Gate 31 only: Safe Conflict Write-back.
- Keep preview-first and explicit confirmation requirements.
- Keep commit/push disabled.

## Write-back Audit Result

- Conflict parser/resolver/session (`:domain/conflict`): **Reusable**.
- Preview generation (`ConflictPresetPlanner`, `ConflictReviewPreviewPlanner`): **Reusable**.
- Conflict source gathering in `UploadFlowViewModel`: **Needs narrow extension** (source id + source kind + writable flags).
- SAF file read abstraction (`SafFileReader`): **Reusable for read**.
- SAF file write abstraction: **Missing**.
- SAF permissions persistence/writability guarantees: **Unsafe to assume globally**; must fail per-file at runtime.
- UI write action in Gate 29/30: **UI-visible but nonfunctional** (disabled write buttons).
- Tests for write planning/execution: **Missing**.

## Implemented

- Added domain write-back models/execution:
  - `ResolvedConflictFile`
  - `ConflictWritePlan`
  - `ConflictWriteFailure`
  - `ConflictWriteResult`
  - `ConflictFileWriter` + `ConflictFileWriteOutcome`
  - `ConflictWritePlanner` (preset/card preview -> safe plan)
  - `ConflictWriteExecutor` (confirmed write execution + partial-failure reporting)
- Extended `ConflictSourceFile` with write metadata (`sourceId`, `sourceKind`, `writableBySaf`).
- Added app-side SAF writer adapter:
  - `SafFileWriter` (`openOutputStream(uri, "wt")`, UTF-8 write, structured failures)
- Wired writer into DI and ViewModel factory.
- Updated `UploadFlowViewModel`:
  - build write plan from preset preview
  - build write plan from card preview
  - execute confirmed write
  - clear write-plan/result state on preview/session reset
  - source collection now tags ZIP entries as non-writable/blocked
- Updated `UploadFlowScreen`:
  - meaning-first write-plan UX
  - "Save these decisions" action
  - explicit final confirmation dialog for "Write resolved files"
  - clear copy: local-only write, no commit, no push
  - blocked-file visibility and plan summary

## Write-back Capability

- Works for: selected SAF files with writable URI source IDs (single-file, multiple-file, folder sources).
- Blocked for: ZIP-entry conflict sources, unresolved/manual conflicts, malformed conflicts, missing/non-writable source metadata.

## Backup

- No backup system added in Gate 31.
- Safety remains preview-first + explicit confirmation + per-file structured result reporting.

## Partial Write Behavior

- Partial writes can happen when multiple files are eligible and a later file fails.
- Result reports exact written/blocked/failed file counts and file paths.
- No commit and no push path exists in this flow.

## Files Changed

- `domain/src/main/kotlin/com/painkiller/domain/conflict/ConflictModels.kt`
- `domain/src/main/kotlin/com/painkiller/domain/conflict/ConflictWriteBack.kt`
- `domain/src/test/kotlin/com/painkiller/domain/conflict/ConflictWriteBackTest.kt`
- `app/src/main/java/com/painkiller/data/files/SafFileWriter.kt`
- `app/src/main/java/com/painkiller/di/PainkillerContainer.kt`
- `app/src/main/java/com/painkiller/ui/navigation/PainkillerNavGraph.kt`
- `app/src/main/java/com/painkiller/ui/flow/UploadFlowViewModel.kt`
- `app/src/main/java/com/painkiller/ui/flow/UploadFlowScreen.kt`
- `README.md`
- `knownbugs.md`
- `handoff/GATE_31_HANDOFF.md`

## Checks Run

- command: `./gradlew --no-daemon :domain:test`
- result: PASS

- command: `./gradlew --no-daemon :app:compileDebugKotlin`
- result: local SDK missing (`SDK location not found`), CI remains authoritative.

## CI Status

- workflow: `.github/workflows/build.yml`
- result: pending push

## Known Risks

- Android providers may deny write access even when a source URI exists; handled as per-file failure.
- ZIP-entry write-back remains intentionally blocked in this gate.

## Explicitly Not Done

- No auto-commit.
- No auto-push.
- No AI merge suggestions.
- No full merge editor.
- No PR/auth/LFS/release feature expansion.

## Next Gate May Start Only If

- CI validates Android build green, or user explicitly accepts CI-first progression.
