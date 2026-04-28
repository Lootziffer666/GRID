# Gate 27 Handoff

## Status

PASS

## Gate Scope

- Implement Gate 27 only: Streaming Large Uploads.
- Remove eager in-memory payload materialization from single-file Git LFS upload path.
- Remove eager in-memory payload materialization from single-file Release Asset upload path.
- Keep commit safety invariants unchanged (`force=false`, LFS object upload before pointer commit).

## Streaming Audit Result

- LFS selected file read path (`UploadFlowViewModel.uploadSingleFileViaLfs`): **Needs streaming refactor → DONE**.
- `LoadedFile.contentBase64`: **In-memory acceptable for normal commit path <=100 MiB; large-file path now bypasses it**.
- `GithubLfsRepository`: **Needs streaming refactor → DONE** (streaming hash + streaming upload).
- `KtorGithubLfsApi`: **Needs streaming refactor → DONE** (`OutgoingContent.WriteChannelContent`).
- `UploadReleaseAssetRequest`: **Needs streaming refactor → DONE** (`UploadPayload` instead of `ByteArray`).
- `GithubReleaseRepository`: **Updated** to validate + forward streaming payload.
- `KtorGithubReleaseApi`: **Needs streaming refactor → DONE** (`OutgoingContent.WriteChannelContent`).
- `UploadFlowViewModel.uploadSelectedFileAsReleaseAsset()`: **Needs streaming refactor → DONE** (SAF stream payload, no Base64 decode).
- `SafFileReader`: **Partially streaming**; added metadata-only read + payload creation for stream paths.
- Base64 decode to `ByteArray`: **Removed from LFS/release upload paths**.
- Tests assuming eager bytes: **Updated** to stream-backed payload fakes.

## Implemented

- Added domain upload stream contract: `UploadPayload { sizeBytes, openStream() }`.
- Refactored release-asset request model to carry `UploadPayload` instead of `ByteArray`.
- Added app payload implementations:
  - `SafUriUploadPayload` (ContentResolver/Uri-backed)
  - `ByteArrayUploadPayload` (test helper payload).
- Added metadata-first single-file load path in `SafFileReader` and `UploadFlowViewModel`:
  - for files >100 MiB: metadata only (no eager Base64 read)
  - for normal-size files: existing Base64 read path remains for normal Git commit flow.
- Added streaming SHA-256 + size digest in `LfsPointer` (`digestStream`, `buildPlanFromStream`).
- Refactored `GithubLfsRepository` to:
  - compute oid/size from stream,
  - upload stream payload,
  - keep pointer commit strictly after successful object upload,
  - map failures with explicit “repo not updated” messaging.
- Refactored `KtorGithubLfsApi.uploadObject(...)` to stream `UploadPayload` via `OutgoingContent.WriteChannelContent`.
- Refactored release asset path to stream:
  - `UploadFlowViewModel.uploadSelectedFileAsReleaseAsset()` now passes stream-backed payload,
  - `KtorGithubReleaseApi.uploadReleaseAsset(...)` now streams payload body.

- Follow-up compile fix: corrected named arguments at streaming upload call sites (`payloadContentType` and `uploadContentType`) after helper parameter rename.
- Updated UI copy to reflect streaming truth for LFS and release assets.

## Files Changed

- `domain/src/main/kotlin/com/painkiller/domain/github/UploadPayload.kt`
- `domain/src/main/kotlin/com/painkiller/domain/github/GitDataModels.kt`
- `domain/src/main/kotlin/com/painkiller/domain/github/ReleaseAssetValidation.kt`
- `domain/src/main/kotlin/com/painkiller/domain/lfs/LfsPointer.kt`
- `domain/src/test/kotlin/com/painkiller/domain/github/ReleaseAssetValidationTest.kt`
- `domain/src/test/kotlin/com/painkiller/domain/lfs/LfsPointerTest.kt`
- `app/src/main/java/com/painkiller/data/files/SafFileReader.kt`
- `app/src/main/java/com/painkiller/data/files/UploadPayloads.kt`
- `app/src/main/java/com/painkiller/data/github/GithubLfsRepository.kt`
- `app/src/main/java/com/painkiller/data/github/KtorGithubLfsApi.kt`
- `app/src/main/java/com/painkiller/data/github/GithubReleaseRepository.kt`
- `app/src/main/java/com/painkiller/data/github/KtorGithubReleaseApi.kt`
- `app/src/main/java/com/painkiller/ui/flow/UploadFlowViewModel.kt`
- `app/src/main/java/com/painkiller/ui/flow/UploadFlowScreen.kt`
- `app/src/test/java/com/painkiller/data/github/GithubLfsRepositoryTest.kt`
- `app/src/test/java/com/painkiller/data/github/KtorGithubLfsApiTest.kt`
- `app/src/test/java/com/painkiller/data/github/KtorGithubReleaseApiTest.kt`
- `README.md`
- `knownbugs.md`
- `handoff/GATE_27_HANDOFF.md`

## Checks Run

- command: `./gradlew :domain:test :domain:build`
- result: PASS

- command: `./gradlew --no-daemon :app:compileDebugKotlin`
- result: local SDK missing (`SDK location not found`), CI remains authoritative per CI-first policy.

- command: `./gradlew --no-daemon :app:testDebugUnitTest`
- result: local SDK missing (`SDK location not found`), CI remains authoritative per CI-first policy.

## CI Status

- workflow: `.github/workflows/build.yml`
- result: pending push

## Known Bugs / Risks

- LFS remains single-file only (no multi-file/folder/ZIP LFS routing).
- Release assets remain single-file only (no batch upload).
- Normal single-file Git commit path still reads Base64 content into memory (intentional current scope, bounded by >100 MiB block for normal commit).

## Explicitly Not Done

- No multi-file/folder/ZIP LFS routing.
- No release asset batch upload.
- No PR feature expansion.
- No OAuth work.
- No Gate 28+ functionality.

## Next Gate May Start Only If

- CI validates Android build green, or user explicitly accepts CI-first progression.
