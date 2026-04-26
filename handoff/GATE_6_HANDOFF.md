# Gate 6 Handoff

## Status

PARTIAL

Gate 6 domain orchestration and the app-side repository wrapper are
implemented. All 16 new orchestrator unit tests pass alongside the existing
41 domain tests (`./gradlew :domain:test` — 57 tests, 0 failures). The
HTTP client implementation of `GithubGitDataApi` is intentionally
deferred to a later networking hardening step, matching the Gate 3
pattern (`GithubOAuthApi` / `GithubRepositoryApi` were defined as
contracts in Gate 3, with concrete HTTP wiring deferred).

`./gradlew :app:assembleDebug` was not run in this container (no
Android SDK installed). Per the CI-first policy and `claude.md` /
`AGENTS.md`, this is delegated to the GitHub Actions build workflow on
`main` and is not treated as a Gate 6 blocker.

This run picked up a Codex implementation that could not push because
its environment had no GitHub credentials. No Codex patch was provided,
so this is a fresh implementation following the same gate scope.

There is no `handoff/GATE_5_HANDOFF.md` in the repository. The user
explicitly stated "Previous gates 0–5 are PASS" before authorizing Gate
6, so per AGENTS.md ("the user explicitly says to proceed") Gate 6
proceeded. Gate 6 was implemented to take its inputs directly
(`SingleFileCommitInput` = `RepoTarget` + `fileName` + `contentBase64` +
`commitMessage`) so that whatever Gate 5 ultimately ships for the
preview/UploadPlan layer can produce that input shape without any
change to Gate 6 code.

## Gate Scope

- Implement Gate 6 only: single-file commit via the GitHub Git Data API.
- Pure-Kotlin orchestration in `:domain` so it is testable without the
  Android SDK or a real network.
- Auth-gated app-side repository in `:app/data/github/` that mirrors the
  Gate 3 wrapper pattern.
- Map known failure modes from `instructions.md` § Gate 6 to
  human-readable result variants:
  - auth error
  - permission error
  - branch not found
  - protected branch
  - SHA mismatch / branch changed
  - network error
- Tests cover: success path, all six failure modes, input validation.
- Do not implement multi-file, folder, or ZIP flows (Gate 7).
- Do not implement the HTTP client implementation of `GithubGitDataApi`
  (deferred, same pattern as Gate 3).
- Do not build a UI screen (UI surface is Gate 5 / Gate 8 polish).

## Implemented

`:domain` (pure Kotlin / JVM):

- `domain/github/GithubGitDataException.kt` — sealed `RuntimeException`
  hierarchy that the future HTTP client implementation will throw to
  signal classified failures (`AuthRequired`, `PermissionDenied`,
  `RefNotFound`, `ProtectedBranch`, `ShaMismatch`, `NetworkUnavailable`).
- `domain/github/SingleFileCommitInput.kt` — Gate 6 input value type.
  Carries the Gate 4 `RepoTarget`, the file name, base64-encoded content,
  and the commit message. Documents that the caller is responsible for
  Gate 2 size diagnosis before invoking Gate 6 (Gate 6 commits whatever
  bytes it is given).
- `domain/github/SingleFileCommitResult.kt` — sealed result. `Success`
  exposes `commitSha`, `commitUrl`, `committedPath`. `Failure` variants:
  `InvalidInput`, `AuthError`, `PermissionError`, `BranchNotFound`,
  `ProtectedBranch`, `ShaMismatch`, `NetworkError`, `UnknownError`. All
  failures expose a non-blank, token-free user-facing `message`.
- `domain/github/SingleFileCommitOrchestrator.kt` — runs the safe
  six-step flow:
  1. `getRef("heads/<branch>")` → captures `baseSha`
  2. `getCommit(baseSha)` → captures `baseTreeSha`
  3. `createBlob(content = base64, encoding = "base64")`
  4. `createTree(baseTree = baseTreeSha, [TreeEntry(path, MODE_FILE,
     TYPE_BLOB, sha = blob.sha)])`
  5. `createCommit(message, tree = newTreeSha, parents = [baseSha])`
  6. `updateRef("refs/heads/<branch>", sha = commit.sha, force = false,
     expectedSha = baseSha)`
  Validation up front (file name not blank / not `..` / no path
  separator, content non-empty, commit message non-blank, joined repo
  path passes `PathValidation.isSafeRepoPath`). Catches each
  `GithubGitDataException` subtype and maps to the matching `Failure`.
  Force is hard-coded to `false` per the existing safety contract on
  `GithubGitDataApi.updateRef`.

`:domain` tests (16 new):

- `domain/github/SingleFileCommitOrchestratorTest.kt`
  - `success_runsAllSixStepsInOrder_andUpdatesRefLastWithExpectedSha`
  - `success_emptyTargetFolder_committedPathIsJustFileName`
  - `shaMismatch_duringUpdateRef_isMappedAndRefHasNotAdvanced`
  - `protectedBranch_duringUpdateRef_isMapped`
  - `authError_atGetRef_stopsBeforeAnyMutation`
  - `branchNotFound_atGetRef_stopsBeforeAnyMutation`
  - `networkError_atGetRef_stopsBeforeAnyMutation`
  - `permissionDenied_atCreateBlob_stopsBeforeRefUpdate`
  - `blankFileName_isInvalidInput_andDoesNotCallApi`
  - `fileNameWithSlash_isInvalidInput_andDoesNotCallApi`
  - `fileNameDotDot_isInvalidInput`
  - `blankCommitMessage_isInvalidInput`
  - `emptyContent_isInvalidInput`
  - `successCommitUrlMayBeNull_whenApiOmitsHtmlUrl`
  - `createTree_request_includesBaseTreeAndOneBlobEntry`
  - `createCommit_request_singleParentIsTheOriginalBranchHead`
  Uses a recording `HappyPathFakeApi` that records the exact sequence of
  API calls so failure tests assert the orchestrator stopped *before*
  ref update.

`:app` (Android):

- `app/data/github/SingleFileCommitRepository.kt` — auth-gated wrapper
  that reads the token from `SecureTokenStore`, returns
  `SingleFileCommitResult.AuthError` when missing, otherwise delegates
  to `SingleFileCommitOrchestrator`. Mirrors `GithubRepoBranchRepository`.
- `app/data/github/PlaceholderGithub.kt` — note updated to mention the
  new repository.

Build:

- `gradle/libs.versions.toml` — added `kotlinx-coroutines-core` and
  `kotlinx-coroutines-test` (version `1.8.1`). Required because the
  Gate 6 orchestrator is `suspend` and tests need `runTest`.
  Coroutines are an in-scope addition for Gate 6 (Gate 6 is the first
  gate that exercises suspend orchestration end-to-end).
- `domain/build.gradle.kts` — added `implementation` of
  `kotlinx-coroutines-core` and `testImplementation` of
  `kotlinx-coroutines-test`.

Documentation:

- `README.md` — added Gate 6 status entry.
- `knownbugs.md` — added BUG-20260426-008 (no Gate 5 handoff;
  proceeding under explicit user authorization), and BUG-20260426-009
  (HTTP client implementation of `GithubGitDataApi` deferred).

## Files Changed

```
gradle/libs.versions.toml
domain/build.gradle.kts
domain/src/main/kotlin/com/painkiller/domain/github/GithubGitDataException.kt
domain/src/main/kotlin/com/painkiller/domain/github/SingleFileCommitInput.kt
domain/src/main/kotlin/com/painkiller/domain/github/SingleFileCommitResult.kt
domain/src/main/kotlin/com/painkiller/domain/github/SingleFileCommitOrchestrator.kt
domain/src/test/kotlin/com/painkiller/domain/github/SingleFileCommitOrchestratorTest.kt
app/src/main/java/com/painkiller/data/github/SingleFileCommitRepository.kt
app/src/main/java/com/painkiller/data/github/PlaceholderGithub.kt
README.md
knownbugs.md
handoff/GATE_6_HANDOFF.md
```

## Checks Run

- command: `./gradlew :domain:test`
  - result: PASS — `BUILD SUCCESSFUL`. 57 tests across 9 test classes,
    0 failures, 0 errors. The 16 new tests in
    `SingleFileCommitOrchestratorTest` are green alongside the 41
    pre-existing tests.
- command: `./gradlew :app:assembleDebug`
  - result: not run in this container — no Android SDK present. Per the
    CI-first policy delegated to GitHub Actions on `main`. No new code
    in `:app` introduces Android-specific APIs (the new
    `SingleFileCommitRepository` is plain Kotlin that depends only on
    other classes already in `:app` and `:domain`).

## CI Status

- workflow: `.github/workflows/build.yml`
- result: not yet executed for this branch in this run. Will be
  validated by GitHub Actions on push.

## Known Bugs / Risks

- `BUG-20260426-007` (OPEN) — pre-existing local-SDK environment limit;
  not reopened.
- `BUG-20260426-008` (ACCEPTED) — no `handoff/GATE_5_HANDOFF.md` exists
  in the repository, but the user explicitly authorized Gate 6 to begin
  ("Previous gates 0–5 are PASS"). Gate 6's input shape was chosen so
  Gate 5's eventual `UploadPlan` / preview layer can produce a
  `SingleFileCommitInput` without modifying Gate 6.
- `BUG-20260426-009` (ACCEPTED) — concrete HTTP client implementation
  of `GithubGitDataApi` is deferred to a later networking hardening
  step. Same pattern as Gate 3 (`GithubOAuthApi` and
  `GithubRepositoryApi` are still contracts).

## Explicitly Not Done

- No multi-file, folder, or ZIP commit (Gate 7 scope).
- No `.gitkeep` injection (Gate 7).
- No HTTP client implementation of `GithubGitDataApi` — the orchestrator
  consumes the existing Gate 0 interface; tests use a recording fake.
- No retry, backoff, or rate-limit handling (Gate 8).
- No UI screen, navigation, or Compose result surface (UI for the upload
  flow is Gate 5 / 8 territory).
- No size re-check inside the orchestrator. The Gate 2 Large File
  Doctor is the contractual gate for that, and Gate 6's docstring
  states the caller must check size before invoking the orchestrator.
- No expansion of `instructions.md` § Gate 6 error variants beyond the
  six explicitly listed there. Gate 8 widens the mapping.
- `force = true` is not exposed anywhere. The orchestrator hard-codes
  `force = false` per the existing safety contract on `updateRef`.

## Next Gate May Start Only If

- This handoff is committed and pushed.
- The GitHub Actions build workflow on this branch is green (or, per
  CI-first policy, the user confirms the workflow result).
- If Gate 5 (UploadPlan + Preview UI) has not been implemented by then,
  it must be implemented before Gate 7 begins, since Gate 7 (multi-file)
  consumes the full UploadPlan / per-file size diagnosis machinery that
  Gate 5 produces.
