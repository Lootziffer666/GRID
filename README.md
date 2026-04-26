# Painkiller

A focused Android tool for one mobile pain:

> "I have files, folders, or ZIPs on my Android phone. I want them safely
> committed and pushed into a GitHub repository."

Painkiller is intentionally narrow. It is not a Git client, not GitHub
Desktop, and not an IDE. Its v0 workflow is:

1. Pick a source — file, multiple files, folder, or ZIP.
2. Pick a GitHub repository, branch, and target path.
3. Generate a diagnosis and preview.
4. Suggest a commit message (editable).
5. User explicitly confirms.
6. Commit and push using the GitHub Git Data API as one atomic commit.
7. Show a human-readable result.

For the full product brief, see `instructions.md`.

## Current status

- **Gate 1 — file intake without GitHub: PASS.**
- **Gate 2 — Large File Doctor (pure domain): PASS.**
- **Gate 3 — GitHub auth + repository/branch listing: PASS.**
- **Gate 4 — RepoTarget + presets: PASS (CI-verified).**
- **Gate 5 — UploadPlan + preview UI: PARTIAL (domain planning + preview screen landed; SAF wiring and editable commit message deferred).**
- **Gate 6 — single-file commit via Git Data API: PARTIAL (orchestration + tests landed; HTTP client wiring deferred, same pattern as Gate 3).**
- **Gate 7 — multi-file / folder / ZIP commit + `.gitkeep`: PARTIAL (orchestration + tests landed; HTTP client wiring deferred, same pattern as Gate 6).**
- **Gate 8 — robustness / error mapping: PARTIAL (pure-domain error mapper landed; HTTP client not wired so runtime mapping is exercised by tests only).**

### Gate 1 completed

- Domain file-intake models: `SourceKind`, `SelectedSource`, `SelectedSourceItem`,
  `IgnoreRule`, `DefaultIgnoreRules`, `PlannedFile`, `FilePlan`, `FilePlanIssue`.
- Deterministic file planning with safe target/source path normalization and
  duplicate path detection.
- Android-facing SAF boundary interface (`SafSourceIntake`) as Gate 1 adapter seam.

### Gate 2 completed

- Pure domain `LargeFileDoctor` with threshold logic:
  - `>25 MB` warning
  - `>50 MiB` strong warning
  - `>100 MiB` blocked
- `SizeDiagnosis` + `SizeRiskLevel` + deferred recommendations (`GIT_LFS`,
  `RELEASE_ASSETS`).
- `FilePlanBuilder` now assigns diagnosis per planned file and marks
  `isBlockedForNormalCommit` when any included file is >100 MiB.

See `handoff/GATE_1_HANDOFF.md`, `handoff/GATE_2_HANDOFF.md`, `handoff/GATE_3_HANDOFF.md`, and `handoff/GATE_4_HANDOFF.md` for exact run details.
### Gate 3 completed

- `SecureTokenStore` abstraction added in `:app` with a temporary in-memory implementation for local wiring.
- OAuth auth boundary and auth repository flow added (auth code exchange -> token store).
- Repository/branch listing repository added with auth gating.
- No upload, commit creation, push/update-ref logic added.

### Gate 4 implemented

- Added `RepoTarget`, `BranchTarget`, `TargetPath`, and `PainkillerPreset` in `:domain`.
- Added `TargetPath` validation using existing `PathValidation` normalization/safety rules.
- Added preset/last-used storage contracts and in-memory fake in `:domain`.
- Added DataStore-backed app settings store for non-secret repo target/preset data.
- No upload/commit/push behavior added.

### Gate 5 implemented

- Pure-Kotlin `UploadPlanBuilder` in `:domain/upload/` combines a `FilePlan`
  (Gate 1) and Gate 2 `SizeDiagnosis` results into an `UploadPlan` pre-grouped
  by severity (`safeEntries`, `warningEntries`, `blockedEntries`,
  `deferredEntries`, `ignoredEntries`).
- `UploadPlan.isBlockedForCommit` is true when any blocked entry is present —
  gates the Confirm action in the preview screen.
- `CommitMessageSuggester` generates a short human-readable commit message from
  the safe + warning entries. Result is an editable starting point.
- `UploadPreviewScreen` Compose screen renders each severity group, the target
  repository, and the suggested commit message. Confirm button is disabled when
  blocked or when commit wiring is absent (Gates 6 / 7). No GitHub write.
- `Gate5PreviewSample` provides a deterministic `UploadPlan` for `@Preview` only.
- 16 new domain tests (`./gradlew :domain:test` — 93 tests, 0 failures).

### Gate 6 implemented

- Pure-Kotlin `SingleFileCommitOrchestrator` in `:domain/github/` runs the safe
  six-step Git Data API flow (`getRef → getCommit → createBlob → createTree →
  createCommit → updateRef`) with `force=false` and `expectedSha` so a
  concurrent push surfaces as `ShaMismatch` instead of an overwrite.
- `SingleFileCommitInput` (`RepoTarget` + file name + base64 content + commit
  message) and `SingleFileCommitResult` (`Success` + 7 explicit `Failure`
  variants) bracket the orchestrator with explicit, token-free user-facing
  messages.
- `GithubGitDataException` sealed hierarchy classifies the failure modes the
  HTTP client implementation will throw (auth required, permission denied,
  ref not found, protected branch, SHA mismatch, network unavailable).
- `:app/data/github/SingleFileCommitRepository` mirrors the Gate 3 wrapper
  pattern: token-gated, delegates to the domain orchestrator.
- HTTP client implementation of `GithubGitDataApi` is intentionally deferred
  (same pattern as `GithubOAuthApi` / `GithubRepositoryApi` from Gate 3).
- 16 new orchestrator tests (`./gradlew :domain:test` — 57 tests, 0 failures at Gate 6 merge).

### Gate 7 implemented

- Pure-Kotlin `MultiFileCommitOrchestrator` in `:domain/github/` extends the
  Gate 6 flow to N files atomically: validate every entry → `getRef →
  getCommit → createBlob` (one per entry) `→ createTree` (all entries in
  one call) `→ createCommit → updateRef` with `force=false` and
  `expectedSha`. The branch ref is only touched in the final step, so any
  failure leaves the repository visibly unchanged.
- `MultiFileCommitInput` (`RepoTarget` + `List<MultiFileCommitEntry>` +
  commit message) and `MultiFileCommitResult` (`Success` with
  `committedPaths` + 7 explicit `Failure` variants) bracket the
  orchestrator with explicit, token-free user-facing messages.
- ZIP-Slip prevention is enforced in the domain validation step: any
  entry whose `repoPath` is not already in `PathValidation`-normalized
  canonical form (e.g. leading `/`, `..` traversal, Windows drive prefix,
  duplicate slashes) is rejected with `InvalidInput` before any API call.
- `.gitkeep` is auto-injected at the target path when the entries list
  is empty, so an explicit "empty target folder" still produces a
  visible folder on GitHub. Empty content is sent with `encoding="utf-8"`.
- Folder structure and ZIP virtual entries are supported transparently
  — the orchestrator does not care about source kind, only about safe
  pre-validated paths. Tree entries are sorted by path for deterministic
  commits.
- `:app/data/github/MultiFileCommitRepository` mirrors the Gate 6
  wrapper pattern: token-gated, delegates to the domain orchestrator.
- HTTP client implementation of `GithubGitDataApi` remains intentionally
  deferred (same pattern as Gate 3 / Gate 6).
- 20 new orchestrator tests (`./gradlew :domain:test` — 77 tests, 0
  failures at Gate 7 merge).

### Gate 8 implemented

- Pure-Kotlin `PainkillerErrorMapper` in `:domain/error/` maps every Gate 6
  and Gate 7 failure variant, plus Gate 3 auth/listing failures, to a
  `HumanReadableError` with a `RetrySafety` classification and a
  `RecoveryHint`.
- `RetrySafety` distinguishes `SAFE_TO_RETRY` (network, tap to retry),
  `REQUIRES_PLAN_REFRESH` (SHA mismatch — plan must be rebuilt before any
  new write), and `NOT_RETRYABLE` (auth, permissions, invalid input).
- `RecoveryHint` surfaces the most useful next step per failure type
  (`SIGN_IN`, `CHOOSE_DIFFERENT_BRANCH`, `REFRESH_PLAN`,
  `CHECK_PERMISSIONS`, `CHECK_NETWORK`, `REMOVE_LARGE_FILES`,
  `FIX_FILE_PATHS`, `NO_ACTION`).
- Token sanitization: `PainkillerErrorMapper.sanitize()` replaces known
  GitHub token prefixes (`ghp_`, `ghs_`, `gho_`, `github_pat_`, `Bearer`)
  with `[token redacted]` before any text can reach the UI or logs.
- Pre-commit blocked-file path: `mapBlockedForCommit()` covers the Gate 5
  upload-plan gate where large files prevent the operation.
- No `force=true`. No silent overwrite. No automatic write retry.
- 36 new mapper tests (`./gradlew :domain:test` — 129 tests, 0 failures).


## Repository structure

```text
PAINKILLER/
├── README.md
├── claude.md
├── knownbugs.md
├── instructions.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
├── gradlew
├── handoff/
├── templates/
├── domain/
└── app/
```

## Build and run

Painkiller targets:

- `compileSdk = 35`
- `minSdk = 26`
- `targetSdk = 35`
- Kotlin `2.0.21`
- Android Gradle Plugin `8.7.3`
- JVM target `17`

### Pure-Kotlin domain checks

```bash
./gradlew :domain:test
./gradlew :domain:build
```

### Android assembly (Android SDK required)

Set `ANDROID_HOME`, or create `local.properties` in the repository root:

```properties
sdk.dir=/path/to/Android/sdk
```

Then:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## Known limitations

- SAF implementation behind `SafSourceIntake` is not wired yet (interface only).
- GitHub auth + repository/branch listing domain/data wiring exists, but UI and real HTTP client wiring are still pending.
- Single-file (Gate 6) and multi-file / folder / ZIP / `.gitkeep` (Gate 7)
  commit orchestration exists, but no real HTTP client implementation of
  `GithubGitDataApi` is wired yet, so no actual upload reaches GitHub.
- ZIP archive byte extraction itself happens at the SAF/`:app` boundary;
  the Gate 7 orchestrator validates and commits already-decoded entries
  (ZIP-Slip prevention is enforced regardless of the source).
- `PainkillerErrorMapper` maps failures to `HumanReadableError` in the domain
  layer, but the HTTP client (which would produce runtime errors) is not wired
  yet; error mapping is exercised by unit tests only until the HTTP layer lands.
- No preview screen (Gate 5 has the preview Compose screen; navigation wiring is pending).
- Preset selection UI is not wired yet (storage/model support exists).
- The primary action button in the app shell is intentionally disabled.

## Out of scope (whole project)

- Git LFS upload (only diagnosed, not executed).
- GitHub Release Asset upload (only diagnosed).
- Conflict Cards / automatic merge resolution.
- PRs, branch graphs, full Git history.
- Background sync.
- A general-purpose file manager.
