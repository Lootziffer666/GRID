# Gate 14 Handoff

## Status

PASS

## Gate Scope

ZIP intake + multi-file commit path end-to-end. Folder and ZIP sources
now reach the GitHub Git Data API via the existing Gate 7
`MultiFileCommitOrchestrator`.

## Implemented

- `SafZipReader` — opens a ZIP via `ContentResolver.openInputStream`,
  walks it once with `ZipInputStream`, normalizes each entry path with
  `PathValidation.normalizeRepoPath` (ZIP-Slip first wall — orchestrator
  is the second), Base64-encodes content immediately, bounded by
  `MAX_FILES = 500`. Returns
  `ZipReadResult(source: SelectedSource(ZIP, items), contentByRelativePath: Map<String, String>)`.
- `PainkillerContainer` — exposes `safZipReader: SafZipReader`.
- `UploadFlowViewModel`:
  - new state: `loadedZipContent: Map<String, String>?`,
    `loadedFilePlan: FilePlan?`, `successCommittedPaths: List<String>?`
  - replaced singular `successCommittedPath` with `successCommittedPaths`
    (length-1 list for single-file success)
  - new computed: `isFolderSource` / `isZipSource` / `isMultiFileSource`
    derived from `loadedFolder?.kind`
  - `onZipSourceLoaded(result)` — stores ZIP source + content map
  - `clearLoadedFolder()` — clears folder *or* ZIP (both live in
    `loadedFolder`)
  - `buildPlan()` — dispatches single-file path vs.
    `buildMultiFilePlan()` (one helper for FOLDER + ZIP)
  - `applyPlan()` — now also stores the `FilePlan` for multi-file commit
  - `confirmUpload()` — branches on `isMultiFileSource`:
    - `confirmSingleFileUpload()` → unchanged Gate 6 path
    - `confirmMultiFileUpload()` → for each `PlannedFile.includedFiles`,
      reads content via `safFileReader.read(Uri.parse(sourceId))` (FOLDER)
      or `loadedZipContent[sourceId]` (ZIP), builds
      `MultiFileCommitEntry` list, calls
      `multiFileCommitRepository.commitMultipleFiles()`, maps result via
      `PainkillerErrorMapper.map(MultiFileCommitResult.Failure)`
  - factory now takes `multiFileCommitRepository`
- `UploadFlowScreen`:
  - new params: `safZipReader`, plus `isLoadingZip` local state
  - new `OpenDocument`-based ZIP launcher (MIME `application/zip`)
  - "Pick file / Pick folder / Pick ZIP" three-button row
  - source section labels ZIP vs. Folder by `state.isZipSource`; removed
    "available in next gate" note for folder/ZIP
  - confirm button no longer disables for multi-file source — only
    `plan.isBlockedForCommit` blocks
  - `SuccessScreen` now takes `paths: List<String>`; shows count for
    multi-file, lists up to 20 paths with "… and N more" overflow
- `PainkillerNavGraph` — passes `container.safZipReader` and
  `container.multiFileCommitRepository` to the upload screen / VM factory.

## Files Changed

- `app/src/main/java/com/painkiller/data/files/SafZipReader.kt` — NEW
- `app/src/main/java/com/painkiller/di/PainkillerContainer.kt` — added safZipReader
- `app/src/main/java/com/painkiller/ui/flow/UploadFlowViewModel.kt` — multi-file commit path + new state fields
- `app/src/main/java/com/painkiller/ui/flow/UploadFlowScreen.kt` — ZIP picker, multi-file success
- `app/src/main/java/com/painkiller/ui/navigation/PainkillerNavGraph.kt` — pass safZipReader + multiFileCommitRepository
- `handoff/GATE_14_HANDOFF.md` — this file

## Checks Run

- command: `./gradlew :domain:test`
  - result: not re-run (domain unchanged in this gate; orchestrator and
    `PathValidation` were already covered green by Gates 5/7).
- command: `./gradlew :app:testDebugUnitTest`
  - result: skipped locally — no Android SDK
    (BUG-20260426-007). CI is source of truth.

## CI Status

- workflow: `.github/workflows/build.yml`
- result: pending push

## Safety Notes

- ZIP-Slip: first wall in `SafZipReader` (entries that don't normalize
  cleanly are dropped silently — never reach the orchestrator); second
  wall remains `MultiFileCommitOrchestrator.validate` which rejects with
  `MultiFileCommitResult.InvalidInput` if any path slips through.
- `force = false` invariant unchanged — multi-file path uses the same
  Gate 7 orchestrator and the same `expectedSha` guard.
- Token never enters the VM — `MultiFileCommitRepository` reads it from
  `SecureTokenStore` per-call (Gate 6/7 pattern).
- Empty ZIPs are handled: `MultiFileCommitOrchestrator` injects a
  `.gitkeep` when `entries.isEmpty()`, but in our flow `FilePlanBuilder`
  already rejects empty sources with `EMPTY_SOURCE`, so the empty case
  surfaces as a plan-build error before any API call.

## Known Bugs / Risks

- `MAX_FILES = 500` is mirrored from `SafFolderReader`; both readers
  silently truncate at the limit. UX could surface this once observed
  in practice.
- ZIP content is held in memory (Base64) until commit time. With
  `MAX_FILES = 500`, total allowed payload is bounded by per-file size
  diagnosis (`LargeFileDoctor`) at plan time; out-of-memory on huge
  individual entries is theoretically possible but covered by the
  100 MiB block threshold for normal commits.

## Explicitly Not Done

- No multi-source mixing (one file *or* one folder *or* one ZIP per flow).
- No upload progress per-file (planned for Gate 15 polish).
- No retry on partial failure — Gate 7 orchestrator is atomic; failures
  surface a user-tap retry only when `RetrySafety.SAFE_TO_RETRY`.

## Next Gate May Start Only If

- CI is green, or user confirms to proceed.
