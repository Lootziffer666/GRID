# Gate 7 Handoff

## Status

PARTIAL

Gate 7 domain orchestration and the app-side repository wrapper are
implemented. All 20 new orchestrator unit tests pass alongside the existing
57 domain tests (`./gradlew :domain:test` — 77 tests, 0 failures). The
HTTP client implementation of `GithubGitDataApi` remains intentionally
deferred (same pattern as Gate 3 / Gate 6, tracked under
BUG-20260426-009).

`./gradlew :app:assembleDebug` was not run in this container (no Android
SDK installed). Per the CI-first policy delegated to the GitHub Actions
build workflow on `main`, this is not treated as a Gate 7 blocker. The
new `:app` code (`MultiFileCommitRepository`) is plain Kotlin that depends
only on already-Android-clean classes, so no new Android-specific surface
was introduced.

This run picks up Gate 6 as the prior gate. Gate 7 extends the Gate 6
single-file orchestration to N files atomically, supports folder
structure, ZIP virtual entries (with ZIP-Slip prevention in the domain),
and `.gitkeep` injection for empty target folders.

## Gate Scope

- Implement Gate 7 only: multi-file / folder / ZIP commit + `.gitkeep`.
- Pure-Kotlin orchestration in `:domain` so the multi-file flow is
  testable without the Android SDK or a real network.
- Auth-gated app-side repository in `:app/data/github/` that mirrors the
  Gate 3 / Gate 6 wrapper pattern.
- ZIP-Slip prevention in the domain validation step: any entry whose
  `repoPath` is not already in normalized canonical form (per
  `PathValidation`) is rejected before any API call.
- `.gitkeep` injection when the entries list is empty, so an explicit
  "empty target folder" still produces a visible folder on GitHub.
- All-or-nothing atomicity: branch ref is only touched in the final
  step. Any failure before `updateRef` leaves the repository visibly
  unchanged.
- Tests cover: multiple small files, folder with subfolders, ZIP
  with subfolders (subpath case), ZIP dangerous path blocked,
  `.gitkeep`, failure before ref update, SHA mismatch during ref update.
- Do not implement the HTTP client implementation of `GithubGitDataApi`
  (deferred, same pattern as Gate 3 / Gate 6).
- Do not extract ZIP archives in `:domain` (ZIP byte/extraction lives
  in the SAF/`:app` boundary; the orchestrator receives already-decoded
  entries).
- Do not build a UI screen.

## Implemented

`:domain` (pure Kotlin / JVM):

- `domain/github/MultiFileCommitEntry.kt` — one file in a multi-file
  commit. `repoPath` must be a normalized GitHub tree path.
  `contentBase64` is the file bytes (empty string for empty files
  such as the auto-injected `.gitkeep`).
- `domain/github/MultiFileCommitInput.kt` — Gate 7 input value type.
  Carries the Gate 4 `RepoTarget`, the list of entries, and the commit
  message. Empty entries list is allowed and triggers `.gitkeep`
  injection.
- `domain/github/MultiFileCommitResult.kt` — sealed result. `Success`
  exposes `commitSha`, `commitUrl`, `committedPaths`. `Failure`
  variants mirror Gate 6: `InvalidInput`, `AuthError`,
  `PermissionError`, `BranchNotFound`, `ProtectedBranch`,
  `ShaMismatch`, `NetworkError`, `UnknownError`. All failures expose
  a non-blank, token-free user-facing `message`.
- `domain/github/MultiFileCommitOrchestrator.kt` — runs the safe
  multi-file flow:
  1. validate (commit message non-blank, every `repoPath` already in
     normalized canonical form per `PathValidation`, no duplicates)
  2. build effective entries (sort by path, inject `.gitkeep` if empty)
  3. `getRef("heads/<branch>")` → captures `baseSha`
  4. `getCommit(baseSha)` → captures `baseTreeSha`
  5. `createBlob` for every effective entry; `.gitkeep`/empty content
     is sent as `encoding="utf-8"` with empty content, regular files
     as `encoding="base64"` with the supplied content
  6. `createTree(baseTree=baseTreeSha, [TreeEntry(...)])` for all
     entries in one call
  7. `createCommit(message, tree=newTreeSha, parents=[baseSha])`
  8. `updateRef("refs/heads/<branch>", sha=commit.sha, force=false,
     expectedSha=baseSha)` — concurrent push surfaces as
     `ShaMismatch`, never an overwrite
  Catches each `GithubGitDataException` subtype and maps to the matching
  `Failure`. Force is hard-coded to `false`.

`:domain` tests (20 new):

- `domain/github/MultiFileCommitOrchestratorTest.kt`
  - `success_twoFiles_blobCreatedForEach_oneTreeAndOneCommit`
  - `success_entriesSortedDeterministicallyByPath`
  - `success_folderWithSubfolders_allPathsCommitted`
  - `success_emptyEntries_withTargetPath_injectsGitkeepAtTargetPath`
  - `success_emptyEntries_emptyTargetPath_gitkeepAtRoot`
  - `success_updateRef_usesBaseShaAsExpectedSha_andForceIsFalse`
  - `success_createTree_usesBaseTreeSha_andContainsBlobShas`
  - `success_createCommit_singleParentIsOriginalBranchHead`
  - `success_commitUrlMayBeNull`
  - `zipSlipPath_dotDotTraversal_isInvalidInput_noApiCalls`
  - `zipSlipPath_absolutePath_isInvalidInput_noApiCalls`
  - `zipSlipPath_windowsAbsolutePath_isInvalidInput_noApiCalls`
  - `blankCommitMessage_isInvalidInput_noApiCalls`
  - `duplicateRepoPaths_isInvalidInput_noApiCalls`
  - `authError_atGetRef_stopsBeforeBlobs`
  - `branchNotFound_atGetRef_stopsBeforeAnyMutation`
  - `networkError_atGetRef_stopsBeforeAnyMutation`
  - `permissionDenied_atCreateBlob_stopsBeforeRefUpdate`
  - `shaMismatch_duringUpdateRef_isMapped`
  - `protectedBranch_duringUpdateRef_isMapped`
  Uses `MultiFakeApi`, a recording fake that issues sequential blob
  SHAs (`blob-sha-1`, `blob-sha-2`, ...) so the tree assembly can be
  inspected, and records `calls` so failure tests can assert the
  orchestrator stopped *before* `updateRef`.

`:app` (Android):

- `app/data/github/MultiFileCommitRepository.kt` — auth-gated wrapper
  that reads the token from `SecureTokenStore`, returns
  `MultiFileCommitResult.AuthError` when missing, otherwise delegates
  to `MultiFileCommitOrchestrator`. Mirrors `SingleFileCommitRepository`.
- `app/data/github/PlaceholderGithub.kt` — note updated to mention the
  new repository.

Documentation:

- `README.md` — Gate 7 status entry.
- `handoff/GATE_7_HANDOFF.md` — this file.
- `knownbugs.md` — no new entries; pre-existing BUG-20260426-007
  (local SDK environment) and BUG-20260426-009 (HTTP client deferred)
  still apply.

## Files Changed

```
domain/src/main/kotlin/com/painkiller/domain/github/MultiFileCommitEntry.kt
domain/src/main/kotlin/com/painkiller/domain/github/MultiFileCommitInput.kt
domain/src/main/kotlin/com/painkiller/domain/github/MultiFileCommitResult.kt
domain/src/main/kotlin/com/painkiller/domain/github/MultiFileCommitOrchestrator.kt
domain/src/test/kotlin/com/painkiller/domain/github/MultiFileCommitOrchestratorTest.kt
app/src/main/java/com/painkiller/data/github/MultiFileCommitRepository.kt
app/src/main/java/com/painkiller/data/github/PlaceholderGithub.kt
README.md
handoff/GATE_7_HANDOFF.md
```

## Checks Run

- command: `./gradlew :domain:test`
  - result: PASS — `BUILD SUCCESSFUL`. 77 tests across 10 test classes,
    0 failures, 0 ignored. The 20 new tests in
    `MultiFileCommitOrchestratorTest` are green alongside the 57
    pre-existing tests.
- command: `./gradlew :app:assembleDebug`
  - result: not run in this container — no Android SDK present. Per
    the CI-first policy delegated to GitHub Actions on `main`. No new
    code in `:app` introduces Android-specific APIs (the new
    `MultiFileCommitRepository` is plain Kotlin that depends only on
    other classes already in `:app` and `:domain`).

## CI Status

- workflow: `.github/workflows/build.yml`
- result: not yet executed for this branch in this run. Will be
  validated by GitHub Actions on push.

## Known Bugs / Risks

- `BUG-20260426-007` (OPEN) — pre-existing local-SDK environment limit;
  not reopened.
- `BUG-20260426-008` (ACCEPTED) — no `handoff/GATE_5_HANDOFF.md` exists
  in the repository, but the user explicitly authorized later gates to
  proceed. Gate 7's input shape (`MultiFileCommitInput` carrying a
  pre-validated entry list) is the same contract Gate 5's eventual
  `UploadPlan` / preview layer can produce without modifying Gate 7.
- `BUG-20260426-009` (ACCEPTED) — concrete HTTP client implementation
  of `GithubGitDataApi` is deferred to a later networking hardening
  step. Same pattern as Gate 3 / Gate 6.

## Explicitly Not Done

- No real ZIP extraction in `:domain`. Decoding archive bytes and
  enumerating entries is the SAF/`:app` boundary's job; the
  orchestrator receives already-decoded `MultiFileCommitEntry` values.
  ZIP-Slip prevention is enforced on the entry paths regardless of
  source, so a `:app` ZIP adapter cannot bypass the safety check by
  pre-normalizing dangerous paths — paths that need normalization are
  rejected.
- No HTTP client implementation of `GithubGitDataApi` — the
  orchestrator consumes the existing Gate 0 interface; tests use a
  recording fake.
- No retry, backoff, or rate-limit handling (Gate 8).
- No size re-check inside the orchestrator. Gate 2 `LargeFileDoctor`
  is the contractual gate for that, and Gate 7's docstring states the
  caller must filter blocked files before invoking the orchestrator.
- No UI screen, navigation, or Compose result surface (UI for the
  upload flow is Gate 5 / 8 territory).
- No `force = true` exposed anywhere. The orchestrator hard-codes
  `force = false`, same as Gate 6.
- No detection of "empty sub-folder" inside a partial entry list —
  `.gitkeep` is only injected when the *entire* entry list is empty.
  Per-subfolder `.gitkeep` is not in scope and is not required by the
  Gate 7 acceptance criteria.

## Next Gate May Start Only If

- This handoff is committed and pushed.
- The GitHub Actions build workflow on this branch is green (or, per
  CI-first policy, the user confirms the workflow result).
- Gate 8 widens error mapping and adds robustness (retry, rate limit,
  log sanitization). Gate 7's failure variants are the foundation Gate
  8 will extend, not replace.
