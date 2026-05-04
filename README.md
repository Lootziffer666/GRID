# Painkiller

![Painkiller App Icon](app/src/main/res/drawable/painkiller_logo.xml)

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

**Gate 37 PASS is the last safe implementation gate. Gate 38 is BLOCKED pending concrete, user-approved next-gate scope.**

Painkiller now includes:

- file, multi-file, folder, and ZIP intake
- repo/branch/target selection
- open pull-request picker foundation (select PR head branch)
- PR merge-assist foundation (mergeability diagnostics + explicit merge/squash/rebase confirmation)
- upload planning + diagnosis + preview
- single and multi-file Git Data API orchestration
- Ktor-based GitHub API adapters (repo/branch listing + git-data + token probe)
- PAT-based sign-in with encrypted secure storage
- OAuth Device Flow / OAuth App documented as a future mobile-friendly auth candidate (not yet implemented)
- ZIP intake as a real source path with root normalization, ZIP-Slip blocking, collision surfacing, and commit integration
- dark mode toggle (default is light mode)
- splash screen + vector app icon branding
- intake hardening + UX polish in progress (see `handoff/NEXT_GATES_PLAN.md`)
- user-directed scope expansion roadmap now includes OAuth (additional login), PR merge assist/management (optional ONNX local scoring), and further LFS/release hardening in later gates
- release workflow now supports listing releases, creating a release, and uploading the currently selected single file as a GitHub Release Asset
- large-file routing panel now explains Normal commit vs Git LFS vs Release Asset vs Blocked/Unsupported per source type
- conflict preset MVP now supports parsing Git conflict markers and generating bulk preset previews (default: keep current version)
- collision card review path now supports per-block decisions (keep current/incoming/both/manual) with summary preview before any write
- safe conflict write-back plan + execution for eligible selected SAF files after explicit final confirmation
- conflict resolution → commit bridge with marker safety scan and explicit commit confirmation

## Runtime feature status (Gate 28 routing baseline)

- **Stable**
  - PAT sign-in
  - single-file/multi-file/folder/ZIP intake + upload planning
  - Git Data API commit flow and safety guards
- **Experimental**
  - Git LFS single-file upload flow (streaming object upload; uploads object first, then commits pointer)
  - Release asset workflow (single-file source only, streaming upload path; requires explicit release selection)
  - Large-file routing decision panel (meaning-first route cards with recommended/blocked/unsupported states)
  - PR merge-assist diagnostics/actions
  - Codex collision cleanup preset preview (KEEP_CURRENT / KEEP_INCOMING / KEEP_BOTH / manual review)
  - Collision card review flow (button-first card decisions + in-memory summary preview)
- **Deferred**
  - OAuth Device Flow / OAuth App sign-in path (candidate only; not yet implemented)
  - multi-file/folder/ZIP Git LFS routing
  - multi-file release asset batch upload
  - conflict write-back for ZIP-entry sources (blocked for safety)
  - conflict commit bridge for ZIP-entry sources (blocked for safety)
- **Hidden**
  - none currently

This is **not** a public Release Candidate. It is an **Internal Test
Candidate** suitable for end-to-end domain testing, code review, and CI
verification of the gated layers.

## Workbench flow spine (Gate 43 alignment)

Canonical operator flow is:

1. **Source** — pick single file, multiple files, folder, or ZIP.
2. **Target** — set owner/repo/branch/target path.
3. **Diagnose** — build plan and classify risks/blocks.
4. **Route** — choose valid execution lane (normal commit, LFS single-file, release asset single-file, or blocked/unsupported).
5. **Confirm** — explicit human confirmation before any local write, commit, merge, or upload action.
6. **Execute** — perform selected action with safety guards (SHA freshness, non-force ref updates, explicit constraints).
7. **Result/Recovery** — show success details or human-readable failure + safe next step.

## Source intake hardening focus (Gate 44)

Current intake safety posture:

- explicit SAF-driven source selection (single/multi/folder/ZIP)
- unsafe ZIP path blocking before commit execution
- collision visibility before final write/commit actions
- no implicit write on intake/preview alone

## Handoff index (Gate 0 → current)

- Gate 0: `handoff/GATE_0_HANDOFF.md`
- Gate 1: `handoff/GATE_1_HANDOFF.md`
- Gate 2: `handoff/GATE_2_HANDOFF.md`
- Gate 3: `handoff/GATE_3_HANDOFF.md`
- Gate 4: `handoff/GATE_4_HANDOFF.md`
- Gate 5: `handoff/GATE_5_HANDOFF.md`
- Gate 6: `handoff/GATE_6_HANDOFF.md`
- Gate 7: `handoff/GATE_7_HANDOFF.md`
- Gate 8: `handoff/GATE_8_HANDOFF.md`
- Gate 9: `handoff/GATE_9_HANDOFF.md`
- Gate 10: `handoff/GATE_10_HANDOFF.md`
- Gate 11: `handoff/GATE_11_HANDOFF.md`
- Gate 12: `handoff/GATE_12_HANDOFF.md`
- Gate 13: `handoff/GATE_13_HANDOFF.md`
- Gate 14: `handoff/GATE_14_HANDOFF.md`
- Gate 15: `handoff/GATE_15_HANDOFF.md`
- Gate 16: `handoff/GATE_16_HANDOFF.md`
- Gate 17: `handoff/GATE_17_HANDOFF.md`
- Gate 18: `handoff/GATE_18_HANDOFF.md`
- Gate 19: `handoff/GATE_19_HANDOFF.md`
- Gate 20: `handoff/GATE_20_HANDOFF.md`
- Gate 21: `handoff/GATE_21_HANDOFF.md`
- Gate 22: `handoff/GATE_22_HANDOFF.md`
- Gate 23: `handoff/GATE_23_HANDOFF.md`
- Gate 24: `handoff/GATE_24_HANDOFF.md`
- Gate 24.5: `handoff/GATE_24_5_HANDOFF.md`
- Gate 24.6: `handoff/GATE_24_6_HANDOFF.md`
- Gate 25: `handoff/GATE_25_HANDOFF.md`
- Gate 26: `handoff/GATE_26_HANDOFF.md`
- Gate 27: `handoff/GATE_27_HANDOFF.md`
- Gate 28: `handoff/GATE_28_HANDOFF.md`
- Gate 29: `handoff/GATE_29_HANDOFF.md`
- Gate 30: `handoff/GATE_30_HANDOFF.md`
- Gate 31: `handoff/GATE_31_HANDOFF.md`
- Gate 32: `handoff/GATE_32_HANDOFF.md`
- Gate 33: `handoff/GATE_33_HANDOFF.md`
- Gate 34: `handoff/GATE_34_HANDOFF.md`
- Gate 35: `handoff/GATE_35_HANDOFF.md`
- Gate 36: `handoff/GATE_36_HANDOFF.md`
- Gate 37: `handoff/GATE_37_HANDOFF.md`
- Gate 38 (blocked): `handoff/GATE_38_HANDOFF.md`
- Gate 38 recovery ledger: `handoff/GATE_38_RECOVERY_HANDOFF.md`
- Gate 40: `handoff/GATE_40_HANDOFF.md`
- Gate 41: `handoff/GATE_41_HANDOFF.md`
- Gate 42: `handoff/GATE_42_HANDOFF.md`
- Gate 43: `handoff/GATE_43_HANDOFF.md`
- Gate 44: `handoff/GATE_44_HANDOFF.md`
- Next planning ledger: `handoff/NEXT_GATES_PLAN.md`

| Layer            | Gates   | Status                              |
|------------------|---------|-------------------------------------|
| MVP Core         | 0–4     | PASS                                |
| Execution Layer  | 5–7     | PASS                                |
| Safety Layer     | 8       | PASS                                |
| Integration      | 9–14    | PASS (current baseline)             |

- **Gate 1 — file intake without GitHub: PASS.**
- **Gate 2 — Large File Doctor (pure domain): PASS.**
- **Gate 3 — GitHub auth + repository/branch listing: PASS** (boundaries; HTTP client deferred).
- **Gate 4 — RepoTarget + presets: PASS** (CI-verified).
- **Gate 5–14: PASS** (flow is wired through auth, planning, preview, and commit orchestration).

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

- PAT sign-in is the active product auth path.
- OAuth Device Flow / OAuth App remains a future candidate and is not implemented yet.
- ZIP collisions and unsafe paths are surfaced in the upload flow; unsafe paths block plan build before upload.
- Git LFS is single-file only; object upload is streaming in Gate 27.
- Some UX polish (progress feedback, richer source summaries) is still pending.

## What this candidate is and is not

**Is:**

- a coherent, tested, gated codebase covering intake, diagnosis,
  planning, orchestration, and error mapping
- safe by construction — no `force=true`, no silent overwrite, no
  automatic write retry, no token in any user-facing message or log
- ready for end-to-end domain testing, code review, and CI build
  verification

**Is not:**

- a public Release Candidate — `GithubGitDataApi` has no concrete HTTP
  adapter, so no real upload reaches GitHub at runtime
- a fully-wired Android UI — preset/SAF/auth screens are stubbed and
  the primary action button is disabled
- a Git client, GitHub Desktop replacement, file manager, or background
  sync agent

Git Data API orchestration exists, but concrete HTTP adapter / true
end-to-end GitHub execution remains deferred. See `knownbugs.md`
BUG-20260426-009 for the planned next-step contract.

## Out of scope (whole project)

- Git LFS upload (Gate 26: real single-file object upload + pointer commit).
- GitHub Release Asset upload (only diagnosed).
- Conflict Cards / automatic merge resolution.
- PRs, branch graphs, full Git history.
- Background sync.
- A general-purpose file manager.
## Large-file routing truth (Gate 28)

Painkiller now shows a dedicated routing panel after **Review upload**. The panel states what each path means, whether it is available, and whether it changes the repo.

- **Put into repo normally (Normal repo commit)**  
  Best for small text/code files. Creates one normal Git commit.
- **Store large file with Git LFS (single-file only)**  
  Uploads the large object first, then commits a small pointer file.
- **Publish as Release Asset (single-file only)**  
  Uploads a downloadable artifact to a selected GitHub Release; no normal repo commit.
- **Blocked for safety**  
  Appears when GitHub would reject the normal path (e.g., >100 MiB) or ZIP safety checks fail.

Current source-type mapping:

- Single small file: normal commit recommended.
- Single file >100 MiB: normal commit blocked; Git LFS and Release Asset routes shown.
- Multiple files / folder with large entries: normal commit blocked; LFS/Release Asset routes shown as unavailable for this source.
- ZIP with large entries: normal commit blocked for affected entries; ZIP-to-LFS and ZIP-entry Release routing are unavailable.
- Unsafe ZIP: blocked; no alternate route can bypass unsafe ZIP validation.

## Large-file truth audit (Gate 34)

Audit result against source-of-truth domain logic:

- Thresholds are:
  - warning above `25,000,000` bytes (decimal 25 MB)
  - warning above `50 * 1024 * 1024` bytes (50 MiB)
  - blocked above `100 * 1024 * 1024` bytes (100 MiB)
- Normal commit remains executable only when no blocked entries are present.
- Git LFS route remains executable only for one selected large single file.
- Release Asset route remains executable only for one selected file with an explicit selected release.
- Multi-file/folder/ZIP LFS and batch release asset routes remain unavailable and are shown as disabled explanatory options.

## Release asset streaming / batch truth (Gate 35)

Audit result against source-of-truth app/data logic:

- Release asset upload uses stream-backed body writing (`OutgoingContent.WriteChannelContent`).
- Upload entrypoint requires exactly one selected file source.
- Release upload requires explicit release selection before execution.
- Multi-file/folder/ZIP release batch upload remains unavailable and intentionally non-executable.

## LFS expansion decision (Gate 36)

Decision result:

- Keep LFS execution single-file only in current scope.
- Keep multi-file/folder/ZIP LFS routes visible but non-executable.
- Defer LFS expansion to a dedicated implementation gate with explicit safety/orchestration scope.

## Workbench navigation recenter (Gate 37)

- Authenticated navigation route naming now centers on `workbench`.
- Primary top bar framing now uses “GitHub Workbench” terminology.
- Flow order and feature behavior remain unchanged in this gate.

## Codex conflict presets MVP (Gate 29)

Painkiller now includes a minimal collision-cleanup preset flow for Git conflict markers:

- Detects standard markers (`<<<<<<<`, `=======`, `>>>>>>>`) in selected source files.
- Supports presets:
  - Keep current version
  - Keep incoming version
  - Keep both blocks
  - Manual review
- Builds an in-memory preview first.
- Blocks malformed markers (no guessing).
- Does **not** commit or push.
- Does **not** write resolved files yet in Gate 29.

Current limitation:

- Write-back through SAF is deferred; Gate 29 keeps this flow preview-only for safety.

## Collision cards review (Gate 30)

Painkiller now adds a second conflict-review path for phone UX:

- Review collisions one by one as cards.
- Decisions per card:
  - Keep current version
  - Keep incoming version
  - Keep both blocks
  - Review later (manual)
- Decisions are in-memory only until preview.
- Summary preview shows decision counts and unresolved/manual blocks.
- No file writes, commits, or pushes in this gate.

Current limitation:

- SAF write-back is still deferred; Gate 30 keeps this flow preview-only.

## Safe conflict write-back (Gate 31)

Painkiller now closes the preview loop for conflict cleanup:

- Build a write plan from preset preview or card-review preview.
- Require explicit final confirmation before writing.
- Write only eligible selected local SAF files.
- Block malformed/unresolved/manual files.
- Block ZIP-entry sources for safety.
- Report written/blocked/failed counts clearly.
- No commit is created and nothing is pushed.
