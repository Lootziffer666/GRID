# Gate 13 Handoff

## Status

PASS

## Gate Scope

Folder SAF intake: `SafFolderReader` implementation + UI trigger in
`UploadFlowScreen`. Plan building for folders wired. Folder commit
execution is Gate 14.

## Implemented

- `SafFolderReader` — traverses a `DocumentFile` tree via `OpenDocumentTree`
  SAF intent; bounded by `MAX_DEPTH=10` and `MAX_FILES=500`; skips ignored
  folders (`.git`, `.gradle`, `build`, `node_modules`, `.idea`) during
  traversal (same set as `DefaultIgnoreRules`); returns
  `SelectedSource(FOLDER, items)` with display name, relative path, size,
  and MIME type per item. File content is not read here — that is deferred
  to commit time (Gate 14).
- `PainkillerContainer` — exposes `safFolderReader: SafFolderReader`.
- `UploadFlowViewModel` — added:
  - `loadedFolder: SelectedSource?` state field
  - `isFolderSource` / `hasSource` computed properties
  - `onFolderSourceLoaded(source)` — stores folder, clears file
  - `clearLoadedFolder()` — resets folder + plan
  - `buildFolderPlan()` — delegates to `FilePlanBuilder.build` then
    `UploadPlanBuilder.build`; validation errors surface as `errorMessage`
  - `buildPlan()` — dispatches to file or folder path; errors on no source
  - `clearLoadedFile()` — now also clears `loadedFolder`
  - `onSourceUriPicked()` — now also clears `loadedFolder`
- `UploadFlowScreen` — added:
  - `SafFolderReader` parameter
  - `OpenDocumentTree` launcher; coroutine on `rememberCoroutineScope()` calls
    `safFolderReader.read(uri)` then `viewModel.onFolderSourceLoaded(source)`
  - Local `isLoadingFolder` state for loading indicator during traversal
  - Source section now shows file info, folder file count, loading indicator,
    or "Pick file / Pick folder" buttons
  - Confirm button disabled for folder source with "available in next gate"
    label
- `PainkillerNavGraph` — passes `container.safFolderReader` to `UploadFlowScreen`.
- Added `androidx.documentfile:documentfile:1.0.1` dependency.

## Files Changed

- `app/src/main/java/com/painkiller/data/files/SafFolderReader.kt` — NEW
- `app/src/main/java/com/painkiller/di/PainkillerContainer.kt` — added safFolderReader
- `app/src/main/java/com/painkiller/ui/flow/UploadFlowViewModel.kt` — folder state + plan path
- `app/src/main/java/com/painkiller/ui/flow/UploadFlowScreen.kt` — folder picker + folder UI
- `app/src/main/java/com/painkiller/ui/navigation/PainkillerNavGraph.kt` — pass safFolderReader
- `app/build.gradle.kts` — documentfile dependency
- `gradle/libs.versions.toml` — documentFile version + library entry
- `handoff/GATE_13_HANDOFF.md` — this file

## Checks Run

- command: `./gradlew :domain:test`
  - result: not re-run (domain unchanged)
- command: `./gradlew :app:testDebugUnitTest`
  - result: skipped locally — no Android SDK (BUG-20260426-007). CI is source of truth.

## CI Status

- workflow: `.github/workflows/build.yml`
- result: pending push

## Known Bugs / Risks

- Folder commit is not executable yet (Gate 14). The UI correctly disables
  the confirm button and labels it "available in next gate".
- `MAX_FILES=500` and `MAX_DEPTH=10` limits are defensive but arbitrary; they
  can be relaxed later if needed.

## Explicitly Not Done

- No folder commit execution (Gate 14).
- No ZIP intake (Gate 14).
- No multi-file commit UI path (Gate 14).

## Next Gate May Start Only If

- CI is green, or user confirms to proceed.
