# Gate 1 Handoff

## Status

PARTIAL

## Gate Scope

- Implement Gate 1 only: File Intake without GitHub.
- Add domain models and planning for selected file sources.
- Integrate path normalization with existing `PathValidation`.
- Add default ignore rules.
- Add pure Kotlin unit tests for FilePlan behavior.
- Add Android SAF-facing intake boundary interface only (no upload/network/auth).

## Implemented

- Added `domain/files` package with Gate 1 models:
  - `SourceKind`
  - `SelectedSource`, `SelectedSourceItem`
  - `IgnoreRule`, `DefaultIgnoreRules`
  - `PlannedFile`
  - `FilePlan`, `FilePlanBuildResult`
  - `FilePlanIssue`, `FilePlanIssueCode`
  - `FilePlanBuilder`
- `FilePlanBuilder` behavior:
  - validates non-empty source input
  - normalizes and validates target path via `PathValidation.normalizeRepoPath`
  - normalizes source relative paths
  - generates deterministic repo paths
  - applies default ignore rules (`.git`, `.gradle`, `build`, `node_modules`, `.idea`)
  - detects duplicate normalized repo paths
  - captures validation/planning issues without throwing
- Added Android-facing interface in `:app`:
  - `SafSourceIntake` with intake methods for single file, multiple files, folder tree, ZIP.
- Added Gate 1 domain unit tests covering:
  - single file source
  - multiple file source
  - folder source
  - ZIP source kind
  - root and nested target paths
  - unsafe target path rejection
  - default ignore rules
  - duplicate normalized path detection
  - empty source list rejection
  - deterministic ordering
- Updated `README.md` status and Gate 1 deliverables summary.

## Files Changed

- `README.md`
- `knownbugs.md`
- `handoff/GATE_1_HANDOFF.md`
- `app/src/main/java/com/painkiller/data/files/SafSourceIntake.kt`
- `domain/src/main/kotlin/com/painkiller/domain/files/SourceKind.kt`
- `domain/src/main/kotlin/com/painkiller/domain/files/SelectedSource.kt`
- `domain/src/main/kotlin/com/painkiller/domain/files/IgnoreRule.kt`
- `domain/src/main/kotlin/com/painkiller/domain/files/DefaultIgnoreRules.kt`
- `domain/src/main/kotlin/com/painkiller/domain/files/PlannedFile.kt`
- `domain/src/main/kotlin/com/painkiller/domain/files/FilePlanIssue.kt`
- `domain/src/main/kotlin/com/painkiller/domain/files/FilePlan.kt`
- `domain/src/main/kotlin/com/painkiller/domain/files/FilePlanBuilder.kt`
- `domain/src/test/kotlin/com/painkiller/domain/files/FilePlanBuilderTest.kt`

## Checks Run

- command: `./gradlew :domain:test`
  - result: PASS (`BUILD SUCCESSFUL`)
- command: `./gradlew :domain:build`
  - result: PASS (`BUILD SUCCESSFUL`)
- command: `./gradlew :app:assembleDebug`
  - result: BLOCKED in this environment (`SDK location not found`)

## Known Bugs / Risks

- `BUG-20260426-004` (OPEN): local environment missing Android SDK blocks `:app:assembleDebug` verification for this run.

## Explicitly Not Done

- No GitHub authentication.
- No network calls.
- No upload/commit/push behavior.
- No preview UI workflow implementation.
- No ZIP extraction logic beyond ZIP source kind representation.
- No DataStore/token storage implementation.

## Next Gate May Start Only If

- Gate 1 is promoted from `PARTIAL` to `PASS` after successful `./gradlew :app:assembleDebug` on an Android SDK-enabled runner.
- Then Gate 2 (Large File Doctor) may begin.
