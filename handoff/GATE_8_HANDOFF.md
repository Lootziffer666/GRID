# Gate 8 Handoff

## Status

PARTIAL

Gate 8 domain error mapping is implemented. All 36 new unit tests pass
alongside the existing 93 domain tests (`./gradlew :domain:test` — 129
tests, 0 failures). `./gradlew :domain:build` is green.

`./gradlew :app:assembleDebug` was not run in this container (no Android
SDK). Per the CI-first policy delegated to GitHub Actions, this is not a
Gate 8 blocker. No new Android-specific APIs were introduced.

The status is PARTIAL because the concrete `GithubGitDataApi` HTTP client
implementation is still deferred (same pattern as Gate 3, 6, 7). The
mapper is fully tested in isolation; it will be exercised at runtime once
the HTTP client is wired in a future networking hardening step.

## Gate Scope

- Implement Gate 8 only: robust error classification, human-readable error
  mapping, retry safety, recovery hints, and token sanitization.
- Pure-Kotlin error layer in `:domain/error/` — testable without the
  Android SDK or a network.
- Map every existing failure variant from:
  - Gate 6 (`SingleFileCommitResult.Failure` — 8 variants)
  - Gate 7 (`MultiFileCommitResult.Failure` — 8 variants)
  - Gate 3 auth / repo / branch listing failures (string-reason variants)
  - Gate 5 upload-plan blocked-file condition
- Classify each failure's `RetrySafety` so the UI knows when a "Try again"
  button is safe to show.
- Provide a `RecoveryHint` per failure for contextual next-step guidance.
- `PainkillerErrorMapper.sanitize()` redacts GitHub token patterns before
  any text can reach the UI or developer logs.
- No `force=true`. No silent overwrite. No automatic write retry.
  SHA mismatch is `REQUIRES_PLAN_REFRESH`, never `SAFE_TO_RETRY`.

## Implemented

`:domain/error/` (pure Kotlin / JVM):

- `domain/error/RetrySafety.kt` — enum:
  - `SAFE_TO_RETRY` — transient failure; user may tap "Try again"
  - `REQUIRES_PLAN_REFRESH` — plan is stale; must rebuild before any write
  - `NOT_RETRYABLE` — user action required; same op will fail again

- `domain/error/RecoveryHint.kt` — enum (8 values):
  `SIGN_IN`, `CHOOSE_DIFFERENT_BRANCH`, `REFRESH_PLAN`,
  `CHECK_PERMISSIONS`, `CHECK_NETWORK`, `REMOVE_LARGE_FILES`,
  `FIX_FILE_PATHS`, `NO_ACTION`

- `domain/error/HumanReadableError.kt` — value type: `title`, `detail`,
  `retrySafety`, `recoveryHint`. Never contains a raw token. Title is ≤ 60
  chars. Detail is one or two sentences.

- `domain/error/PainkillerErrorMapper.kt` — stateless object:

  Gate 6 mapping (`SingleFileCommitResult.Failure`):
  - `InvalidInput` → title "Invalid file path", hint `FIX_FILE_PATHS`,
    not retryable
  - `AuthError` → title "Sign in required", hint `SIGN_IN`, not retryable
  - `PermissionError` → title "Permission denied", hint
    `CHECK_PERMISSIONS`, not retryable. Detail does NOT say "bad
    credentials" — only "access to this repository".
  - `BranchNotFound` → title "Branch not found", hint `NO_ACTION`, not
    retryable
  - `ProtectedBranch` → title "Branch is protected", hint
    `CHOOSE_DIFFERENT_BRANCH`, not retryable
  - `ShaMismatch` → title "Branch changed", hint `REFRESH_PLAN`, safety
    `REQUIRES_PLAN_REFRESH`
  - `NetworkError` → title "Cannot reach GitHub", hint `CHECK_NETWORK`,
    safety `SAFE_TO_RETRY`
  - `UnknownError` → title "Unexpected error", hint `NO_ACTION`, not
    retryable. Detail states "Nothing was written."

  Gate 7 mapping (`MultiFileCommitResult.Failure`): identical logic,
  parallel variants.

  Gate 3 mapping (string-reason):
  - `mapAuthExchange(reason)` → "Sign in failed", `SIGN_IN`, not retryable
  - `mapRepoListing(reason)` → detects auth string → `SIGN_IN`; otherwise
    "Could not load repositories", `CHECK_NETWORK`, safe to retry
  - `mapBranchListing(reason)` → same auth detection pattern

  Pre-commit (blocked file):
  - `mapBlockedForCommit()` → "Upload blocked", `REMOVE_LARGE_FILES`,
    not retryable

  Token sanitization:
  - `sanitize(text)` — replaces `ghp_…`, `ghs_…`, `gho_…`,
    `github_pat_…`, and `Bearer <token>` patterns with `[token redacted]`

`:domain` tests (36 new):

- `domain/error/PainkillerErrorMapperTest.kt`
  - Gate 6: all 8 failure variants mapped correctly (title, hint, retry)
  - Gate 7: all 8 failure variants mapped correctly; auth maps same as G6
  - Gate 3: auth exchange, repo listing (auth + network), branch listing
  - Blocked-for-commit factory method
  - Token redaction: `ghp_`, `ghs_`, `github_pat_`, `Bearer` patterns
  - Clean message unchanged by sanitize
  - Partial token (no known prefix) not redacted
  - Retry classification: network is retryable, auth is not
  - SHA mismatch requires plan refresh, is NOT immediately retryable
  - All mapped errors have non-blank title and detail
  - Tokens do not leak into `HumanReadableError` from exception messages

## Files Changed

```
domain/src/main/kotlin/com/painkiller/domain/error/RetrySafety.kt
domain/src/main/kotlin/com/painkiller/domain/error/RecoveryHint.kt
domain/src/main/kotlin/com/painkiller/domain/error/HumanReadableError.kt
domain/src/main/kotlin/com/painkiller/domain/error/PainkillerErrorMapper.kt
domain/src/test/kotlin/com/painkiller/domain/error/PainkillerErrorMapperTest.kt
README.md
handoff/GATE_8_HANDOFF.md
```

## Checks Run

- command: `./gradlew :domain:test`
  - result: PASS — `BUILD SUCCESSFUL`. 129 tests across 12 test classes,
    0 failures, 0 ignored. The 36 new tests in `PainkillerErrorMapperTest`
    are green alongside the 93 pre-existing tests.
- command: `./gradlew :domain:build`
  - result: PASS — `BUILD SUCCESSFUL`.
- command: `./gradlew :app:assembleDebug`
  - result: not run — no Android SDK in this container. Delegated to
    GitHub Actions per CI-first policy. No new Android-specific code.

## CI Status

- workflow: `.github/workflows/build.yml`
- result: not yet executed. Will be validated by GitHub Actions on push.

## Safety Invariants Verified

- No `force=true` anywhere in Gate 8 code.
- No silent overwrite. No automatic write retry.
- No token in any `HumanReadableError` field — mapper uses fixed strings.
- Token sanitization tested for `ghp_`, `ghs_`, `github_pat_`, `Bearer`.
- SHA mismatch → `REQUIRES_PLAN_REFRESH` (never `SAFE_TO_RETRY`).
- `SAFE_TO_RETRY` covers network-only transient failures; it authorises
  a user-triggered retry button, not background re-execution.
- No upload on screen open, no upload from sample state.
- `HumanReadableError` is an immutable value type — no side effects.

## Explicitly Not Done

- No Gate 9 release candidate work.
- No new upload modes.
- No new GitHub write behavior or API endpoints.
- No real Git LFS upload.
- No GitHub Release Asset upload.
- No Conflict Cards or automatic merge/conflict resolution.
- No background upload worker.
- No retry engine that performs an automatic write.
- No HTTP client implementation of `GithubGitDataApi` (deferred, same
  pattern as Gates 3 / 6 / 7, tracked under BUG-20260426-009).
- No auth redesign, no token storage redesign.
- No broad UI redesign.
- `force=true` is not exposed anywhere.

## Next Gate May Start Only If

- This handoff is committed and pushed.
- The GitHub Actions build workflow on this branch is green (or user
  confirms CI result).
- Gate 9 is the v0 release candidate — UX polish, README, security
  review, manual test checklist, scope lock.
