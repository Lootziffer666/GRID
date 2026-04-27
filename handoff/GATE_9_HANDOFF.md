# Gate 9 Handoff

## Status

PASS

## Candidate Type

**Internal Test Candidate / v0 Integration Candidate**

This is **not** a public Release Candidate. The concrete HTTP client
implementation of `GithubGitDataApi` is intentionally deferred (see
`knownbugs.md` BUG-20260426-009), so no real upload reaches GitHub at
runtime. The gated layers (intake, diagnosis, planning, orchestration,
error mapping) are coherent, documented, tested, and CI-green.

## Gate Scope

- Final reconciliation pass over Gates 0–8.
- README update to honest "Internal Test Candidate" language.
- `knownbugs.md` cleanup: BUG-20260426-007 reclassified from OPEN to
  ACCEPTED (local Android SDK absence is environment-only and covered by
  CI; per AGENTS.md it must not be tracked as a project bug).
- Verification that all expected gate code layers are present.
- Safety audit (no hardcoded tokens, no `force=true`, no auto-retry, no
  Gate 10 behavior, no upload-from-sample-state).
- New `handoff/GATE_9_HANDOFF.md` (this file).
- No new product features. No HTTP client implementation. No Gate 10
  work.

## Verified

### Gate Files

All nine handoffs are present:

```
handoff/GATE_0_HANDOFF.md   — skeleton, theme, Git Data spike (PASS)
handoff/GATE_1_HANDOFF.md   — file intake / FilePlan (PASS)
handoff/GATE_2_HANDOFF.md   — LargeFileDoctor / size diagnosis (PASS)
handoff/GATE_3_HANDOFF.md   — auth + repo/branch listing boundaries (PASS)
handoff/GATE_4_HANDOFF.md   — RepoTarget / presets (PASS)
handoff/GATE_5_HANDOFF.md   — UploadPlan / preview / commit suggestion (PARTIAL)
handoff/GATE_6_HANDOFF.md   — single-file commit orchestration (PARTIAL)
handoff/GATE_7_HANDOFF.md   — multi-file / ZIP / .gitkeep (PARTIAL)
handoff/GATE_8_HANDOFF.md   — robust error mapping (PARTIAL)
handoff/GATE_9_HANDOFF.md   — this file
```

### Core Code Presence

| Gate | Layer                              | Path                                                     | Verified |
|------|------------------------------------|----------------------------------------------------------|----------|
| 1    | File intake / FilePlan             | `domain/files/`                                          | yes      |
| 2    | LargeFileDoctor / SizeDiagnosis    | `domain/files/SizeDiagnosis.kt`                          | yes      |
| 3    | Auth + repo/branch boundaries      | `app/data/github/Github*Repository.kt`, `Github*Api.kt`  | yes      |
| 4    | RepoTarget / BranchTarget / preset | `domain/target/RepoTargetModels.kt`, `PresetStore.kt`    | yes      |
| 5    | UploadPlan / preview / suggester   | `domain/upload/`, `app/ui/screens/UploadPreviewScreen.kt`| yes      |
| 6    | Single-file commit orchestration   | `domain/github/SingleFileCommit*.kt`                     | yes      |
| 7    | Multi-file / ZIP / .gitkeep        | `domain/github/MultiFileCommit*.kt`                      | yes      |
| 8    | Robust error mapping               | `domain/error/`                                          | yes      |

### Safety Checks

| Check                                       | Result |
|---------------------------------------------|--------|
| No hardcoded GitHub tokens in source        | PASS — only test fake `ghp_FAKEFAKE…` strings used to verify redaction in `PainkillerErrorMapperTest` |
| No token logging                            | PASS — no `Log.*` / `println` writes a token; mapper redacts known prefixes |
| No `force = true`                           | PASS — every `updateRef` call passes `force = false`; the only `force=true` mention is the safety-doc comment in `GithubGitDataApi.kt` saying it must not be used |
| No automatic conflict resolution            | PASS — SHA mismatch surfaces as `REQUIRES_PLAN_REFRESH`; no auto-merge code |
| No silent overwrite                         | PASS — `expectedSha` enforced on every ref update |
| No automatic write retry                    | PASS — `RetrySafety.SAFE_TO_RETRY` only authorises a user-tap retry, never a background re-execution |
| No upload on screen open                    | PASS — `UploadPreviewScreen.onConfirm` defaults to `null`; button is disabled when `null` |
| No upload from sample state                 | PASS — `Gate5PreviewSample` is a hardcoded `UploadPlan` value with no API access |
| No Gate 10 behavior                         | PASS — no LFS / Release Asset / Conflict Cards / background worker / JGit / libgit2 references in source |
| No new upload mode introduced in Gate 9     | PASS — only doc and reclassification changes |
| No `local.properties`, `.env`, secret files | PASS — none in repo |

### CI Status

- workflow: `.github/workflows/build.yml` (configured in Gate 0)
- result on previous gate: green per BUG-20260426-001 evidence and the
  user's confirmation history. Will be re-validated on push of this
  commit.

## Files Changed

```
README.md
knownbugs.md
handoff/GATE_9_HANDOFF.md
```

No source code changed. No new tests added. No new dependencies. Gate 9
is a documentation, reconciliation, and verification pass only.

## Checks Run

- command: `./gradlew :domain:test`
  - result: PASS — `BUILD SUCCESSFUL`. 129 tests across 12 test classes,
    0 failures, 0 ignored.
- command: `./gradlew :domain:build`
  - result: PASS — `BUILD SUCCESSFUL`.
- command: `./gradlew :app:assembleDebug`
  - result: not run — no Android SDK in this container. Per AGENTS.md /
    claude.md CI-first policy, delegated to GitHub Actions
    (`.github/workflows/build.yml`). No new knownbug entry created;
    existing BUG-20260426-007 is reclassified to ACCEPTED to reflect that
    this is a permanent environment-only condition.

## Known Bugs / Risks

After Gate 9 reclassification, the open / accepted ledger is:

| Bug ID              | Status   | Gate    | Summary                                                        |
|---------------------|----------|---------|----------------------------------------------------------------|
| BUG-20260426-001    | FIXED    | 0       | Android SDK build path — solved by CI workflow                 |
| BUG-20260426-002    | ACCEPTED | 0       | `kotlinx.serialization` `encodeDefaults = true` requirement    |
| BUG-20260426-003    | ACCEPTED | 0       | Dual-run merge between two parallel Gate 0 implementations     |
| BUG-20260426-004    | FIXED    | 1       | Gate 1 SDK verification covered by CI                          |
| BUG-20260426-005    | FIXED    | 2       | Gate 2 SDK verification covered by CI                          |
| BUG-20260426-006    | FIXED    | 3       | Gate 3 SDK verification covered by CI                          |
| BUG-20260426-007    | ACCEPTED | (env)   | Local Android SDK absence — permanent CI-delegated condition   |
| BUG-20260426-008    | FIXED    | 5       | Gate 5 was missing; recovered and merged                       |
| BUG-20260426-009    | ACCEPTED | 6/7/8   | Concrete `GithubGitDataApi` HTTP client deferred               |

No `OPEN` entries remain after Gate 9.

## Explicitly Not Done

- No new product features.
- No new GitHub API endpoints.
- No HTTP client implementation of `GithubGitDataApi` (remains the
  primary deferred integration task — BUG-20260426-009).
- No Git LFS upload, no GitHub Release Asset upload.
- No Conflict Cards, no automatic merge/conflict resolution.
- No background upload worker.
- No retry engine that performs an automatic write.
- No UI redesign, no auth redesign, no token storage redesign.
- No new persistence layer.
- No local clone / JGit / libgit2 behavior.
- No broad refactors.
- No Gate 10 work.
- No README claims of "release-ready" or "production-ready".

## Release Readiness

This branch is suitable for:

- internal end-to-end domain testing
- code review of the gated layers
- CI build verification on `main`
- handoff to a networking integration step that adds the concrete HTTP
  adapter for `GithubGitDataApi`

This branch is **not yet** suitable for:

- public release — no end-to-end executable upload path
- distribution to non-technical end users — no SAF wiring, no auth UI,
  no commit confirmation flow

The remaining work to reach a true Release Candidate is the deferred
networking layer described in BUG-20260426-009: implement
`GithubOAuthApi`, `GithubRepositoryApi`, and `GithubGitDataApi` over a
real HTTP client (Retrofit/OkHttp or Ktor), wire the SAF intake, and
attach the preview screen's `onConfirm` to the
`SingleFileCommitRepository` / `MultiFileCommitRepository`.

## Next Recommended Gate / Workstream

After Gate 9, the repository owner should plan a separate workstream
(not Gate 10) that:

1. Implements `GithubGitDataApi`, `GithubOAuthApi`, and
   `GithubRepositoryApi` over a real HTTP client; throws the
   `GithubGitDataException` subtypes per HTTP status mapping documented
   in BUG-20260426-009.
2. Wires `SafSourceIntake` to a real Storage Access Framework adapter
   in `:app`.
3. Adds the OAuth flow UI screen and connects `GithubAuthRepository`.
4. Connects `UploadPreviewScreen.onConfirm` to
   `SingleFileCommitRepository` (single-file path) and
   `MultiFileCommitRepository` (multi-file path) using
   `PainkillerErrorMapper.map(...)` for any `Failure` result.
5. Adds an instrumented end-to-end test against a sandbox repository
   before claiming Release Candidate status.

These tasks are an integration / hardening pass, not a new feature gate.
