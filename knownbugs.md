# knownbugs.md

Structured log of bugs, blockers, failed assumptions, workarounds, and
unresolved risks. Never delete entries. When something is fixed, mark the
status `FIXED` and keep the entry. When a risk is consciously accepted,
mark it `ACCEPTED` and explain why.

## Entry format

```md
## BUG-YYYYMMDD-NNN

Status: OPEN | FIXED | ACCEPTED | BLOCKED
Gate: X
Severity: LOW | MEDIUM | HIGH
Summary: <one line>

Evidence:
- <log line, command output, file path:line, etc.>

Action:
- <fix, workaround, follow-up needed>
```

---

## BUG-20260426-001

Status: FIXED
Gate: 0
Severity: MEDIUM
Summary: `:app:assembleDebug` could not run in the sleep-mode environment because no Android SDK was installed. A GitHub Actions build workflow was added and now provides the Android SDK-backed verification path.

Evidence:
- Earlier sleep-mode runner output reported: "SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file at '/home/user/PAINKILLER/local.properties'."
- `ANDROID_HOME` was unset in the sleep-mode runner.
- Project Gradle / AGP configuration parsed cleanly before CI: `:app:help` succeeded, AGP 8.7.3 resolved, and `:app` was included correctly.
- `.github/workflows/build.yml` now installs Android SDK packages (`platforms;android-35`, `build-tools;35.0.0`, `platform-tools`) and runs `:domain:test`, `:domain:build`, and `:app:assembleDebug`.
- User confirmed the newly added workflow runs through successfully after commit `01bd66e399bf1c02a1d84e4bd2da78a4af8424b4`.

Action:
- Fixed by adding the GitHub Actions build workflow.
- Gate 0 may now be treated as verified `PASS`, assuming the corresponding GitHub Actions run remains green.

---

## BUG-20260426-002

Status: ACCEPTED
Gate: 0
Severity: LOW
Summary: `kotlinx.serialization` Json instances must be configured with
`encodeDefaults = true` for outgoing GitHub Git Data API requests. The
default `encoding = "base64"` field on `CreateBlobRequest` is a default
value and will be omitted from output otherwise.

Evidence:
- Initial test
  `domain/src/test/kotlin/com/painkiller/domain/github/GitDataModelsSerializationTest.kt::createBlobRequest_defaultEncoding_isBase64`
  failed with the default Json instance because the `encoding` field was
  omitted.

Action:
- The test now uses `Json { ignoreUnknownKeys = true; encodeDefaults = true }`.
- The Android-side network client (Gate 6) must use the same configuration
  for outgoing requests so GitHub receives explicit `"encoding": "base64"`.

---

## BUG-20260426-003

Status: ACCEPTED
Gate: 0
Severity: LOW
Summary: Two parallel Gate 0 runs produced divergent implementations. Prior
run (`6P6vg`) was merged to `main` as PR #1, then reverted (PR #3). Best
elements of both runs were merged onto this branch (`eFEvQ`).

Evidence:
- Run A (`6P6vg`): single-module, `com.painkiller.app` package, included
  out-of-scope models (`RepoTarget`, `HumanReadableError`), tests run via
  direct `kotlinc` (not Gradle).
- Run B (`eFEvQ`, this branch): multi-module (`:domain` + `:app`),
  `com.painkiller` package, stricter scope, `./gradlew :domain:test`
  verified (19/19 PASS after merge).

Action:
- Accepted. Merged state is now canonical on `main` after PR #2. Out-of-scope models
  (`RepoTarget`, `BranchTarget`, `TargetPath`, `HumanReadableError`) were
  intentionally excluded. `PathValidation` brought into `:domain/path/` as
  a pure-Kotlin utility; full Gate 1/4 integration lands in those gates.
  Typography, `PainkillerApplication`, manifest improvements (INTERNET
  permission, DataExtraction/BackupContent), and standard Android files
  (`proguard-rules.pro`, `backup_rules.xml`, `data_extraction_rules.xml`,
  `colors.xml`) merged in from Run A.

---

## BUG-20260426-004

Status: FIXED
Gate: 1
Severity: MEDIUM
Summary: Gate 1 Android assembly verification is now covered by CI and no longer blocks Gate 1 status.

Evidence:
- Initial local Gate 1 run failed without Android SDK (`SDK location not found`).
- User confirmed CI passed and Gate 1 should be marked `PASS`.

Action:
- Fixed by CI-backed Android SDK verification and Gate 1 promotion to `PASS`.
- No further Gate 1 action required.

---

## BUG-20260426-005

Status: FIXED
Gate: 2
Severity: MEDIUM
Summary: Gate 2 Android assembly verification is now covered by CI and no longer blocks Gate 2 status.

Evidence:
- Initial local Gate 2 run failed without Android SDK (`SDK location not found`).
- User confirmed previous gates 0–2 are `PASS`.

Action:
- Fixed by CI-backed Android SDK verification and Gate 2 promotion to `PASS`.
- No further Gate 2 action required.

---

## BUG-20260426-006

Status: FIXED
Gate: 3
Severity: MEDIUM
Summary: Gate 3 Android assembly verification is now covered by CI and no longer blocks Gate 3 status.

Evidence:
- Initial local Gate 3 run failed without Android SDK (`SDK location not found`).
- User confirmed previous gates 0–3 are `PASS` and build is green.

Action:
- Fixed by CI-backed Android SDK verification and Gate 3 promotion to `PASS`.
- No further Gate 3 action required.

---

## BUG-20260426-007

Status: ACCEPTED
Gate: 4 (and all subsequent local runs)
Severity: LOW
Summary: Local agent environments without an Android SDK cannot run
`:app:assembleDebug`. Per AGENTS.md / claude.md CI-first policy, this is
not a project defect — Android verification is delegated to GitHub
Actions on `main`.

Evidence:
- `./gradlew :app:assembleDebug` fails locally with: "SDK location not
  found. Define a valid SDK location with an ANDROID_HOME environment
  variable or by setting the sdk.dir path in your project's local
  properties file."
- `./gradlew :domain:test` and `./gradlew :domain:build` succeed in
  every gate run.
- The GitHub Actions workflow `.github/workflows/build.yml` provides the
  Android SDK and runs `:app:assembleDebug` as the authoritative check.

Action:
- Accepted as a permanent environment-only condition. No further action.
- Do not reopen for individual gates; the CI workflow is the source of
  truth for Android-side verification.


## BUG-20260426-008

Status: FIXED
Gate: 5 (recovery)
Severity: LOW
Summary: No `handoff/GATE_5_HANDOFF.md` existed at the time Gate 6 was
started. Gate 5 was later recovered in a dedicated session and is now
implemented. Gate sequence 0–7 is complete.

Evidence:
- Repository tree at the start of Gate 6 contained
  `handoff/GATE_0_HANDOFF.md` through `handoff/GATE_4_HANDOFF.md` only.
- `git log origin/main` showed no Gate 5 commit.
- Gate 5 recovery session confirmed Codex had implemented Gate 5 but
  could not push due to missing GitHub credentials.
- `handoff/GATE_5_HANDOFF.md` and all Gate 5 files are now present on
  this branch.

Action:
- Fixed. Gate 5 implemented: `UploadPlanEntry`, `UploadPlan`,
  `UploadPlanBuilder`, `CommitMessageSuggester` (`:domain`);
  `UploadPreviewScreen`, `Gate5PreviewSample` (`:app`). 16 new tests.
- Gate 6 input shape (`SingleFileCommitInput`) is compatible — no changes
  needed to Gates 6 or 7.
- Gate 7 multi-file input shape (`MultiFileCommitInput`) is compatible —
  `plan.safeEntries + warningEntries` → `List<MultiFileCommitEntry>`.

---

## BUG-20260426-009

Status: PARTIAL
Gate: 6 / 10–14
Severity: LOW
Summary: Concrete HTTP client implementations now exist for all three
deferred APIs. PAT-based flow is wired; OAuth web flow remains deferred.

Evidence (Gate 6 deferral):
- `GithubGitDataApi` is an interface in `:domain` only.
- `SingleFileCommitOrchestrator` consumes the interface and is fully
  unit-tested with a recording fake.
- `SingleFileCommitRepository` consumes the same interface from `:app`,
  ready for an HTTP-backed implementation to be injected.

Evidence (Gate 10–14 partial resolution):
- `KtorGithubGitDataApi` implements `GithubGitDataApi` with status-code
  mapping, `force=true` assertion guard, and defence-in-depth exception
  wrapping. Unit-tested via MockEngine.
- `KtorGithubRepositoryApi` implements `GithubRepositoryApi` (repo/branch
  listing, paginated). Unit-tested via MockEngine.
- `KtorGithubTokenProbeApi` implements `GithubTokenProbeApi` (PAT
  validation via `GET /user`). Unit-tested via MockEngine.
- `EncryptedSecureTokenStore` provides real AndroidX Keystore-backed
  token storage replacing the `InMemorySecureTokenStore` placeholder.
- `PainkillerContainer` wires all of the above as lazy singletons.
- `AuthScreen`, `PainkillerNavGraph`, and `UploadFlowScreen` are now
  wired and exercise PAT sign-in + upload planning/commit flows.
- `MainActivity` now launches the navigation graph and splashscreen.

Action:
- Remaining: optional OAuth auth-code UX + backend-assisted token exchange.
- Keep PAT flow as primary path until OAuth server component exists.

---

## BUG-20260427-010

Status: ACCEPTED
Gate: 14
Severity: LOW
Summary: Binary logo source assets are intentionally ignored in git; vector drawables are the canonical in-repo branding assets.

Evidence:
- `.gitignore` now ignores common binary assets (`*.png`, `*.jpg`, `*.zip`, etc.) and `icons/`.
- `icons/painkiller_round_icon_1024.png` was removed from git tracking.
- App branding now uses `app/src/main/res/drawable/painkiller_logo.xml` (+ night variant).

Action:
- Keep editable vector/logo source in drawable XML.
- If raster exports are needed for store publication, generate them in release pipelines or locally without committing binaries.

---

## BUG-20260427-011

Status: FIXED
Gate: 15
Severity: MEDIUM
Summary: Kotlin app compile failed after branding-token rename because multiple UI components still referenced legacy color names.

Evidence:
- CI/Gradle output reported unresolved references in app UI files: `RauschRed`, `BabuTeal`, `AccentAmber`.
- `:app:compileDebugKotlin` failed before packaging.

Action:
- Fixed by adding backward-compatible aliases in `PainkillerColors` mapped to the new palette tokens.
- Follow-up: key call sites in warning/error/severity components were migrated to new token names in Gate 15 polish; aliases remain for compatibility until full cleanup.

---

## BUG-20260427-012

Status: FIXED
Gate: 25
Severity: LOW
Summary: ZIP intake collisions are now surfaced in UI and unsafe ZIP paths are blocked before upload planning.

Evidence:
- `ZipIntakePlanner` records collision and unsafe-path issues during ZIP normalization.
- `UploadFlowViewModel` stores ZIP issues and blocks plan-build when unsafe entries are present.
- `UploadFlowScreen` shows collision and unsafe-path warnings for ZIP sources.

Action:
- Fixed in Gate 25 ZIP-core recentering.
- Keep deterministic first-entry-wins behavior for collisions, now with explicit user-visible warnings.

---

## BUG-20260427-013

Status: ACCEPTED
Gate: 22 planning
Severity: LOW
Summary: Planned ONNX model for merge-assist (~23 MB) can be committed directly without LFS under current size rules.

Evidence:
- Current Large File Doctor warning threshold starts above 25 MB.
- User-provided expected model size is approximately 23 MB.

Action:
- Accept direct commit for initial model artifact.
- Reevaluate LFS routing if model size grows beyond warning thresholds.

---

## BUG-20260427-014

Status: ACCEPTED
Gate: 20
Severity: LOW
Summary: OAuth additional-login UI is wired, but token exchange remains build-dependent until a configured `GithubOAuthApi` backend path exists.

Evidence:
- `GithubAuthRepository.authenticateWithAuthorizationCode()` returns "OAuth web flow is not available in this build." when `oauthApi` is `null`.
- `PainkillerContainer` currently wires `oauthApi = null` by default.

Action:
- Keep OAuth code UI visible as optional path.
- Integrate backend-assisted exchange in a dedicated follow-up gate when credentials flow is available.

---

## BUG-20260427-015

Status: ACCEPTED
Gate: 21
Severity: LOW
Summary: PR foundation currently lists open pull requests only; merge execution and PR write flows are deferred to later gates.

Evidence:
- `KtorGithubPullRequestApi` requests `/pulls?state=open`.
- Upload flow integration only sets branch input from selected PR head ref.

Action:
- Keep as intentional scope boundary for Gate 21.
- Expand in Gate 22+ with merge assist and PR write workflows.

---

## BUG-20260427-016

Status: ACCEPTED
Gate: 22
Severity: LOW
Summary: PR mergeability diagnostics can return `unknown`/`null` transiently, so merge-assist UI may require manual refresh/retry.

Evidence:
- GitHub PR detail endpoint can return undecided mergeability while background checks are still computing.
- Gate 22 UI surfaces this state explicitly rather than guessing.

Action:
- Keep explicit user confirmation flow for merge actions.
- Follow up in later gate with periodic refresh/backoff for mergeability status if needed.

---

## BUG-20260427-017

Status: FIXED
Gate: 24.6
Severity: LOW
Summary: GitHub App installation sign-in dev spike has been removed from the product auth path.

Evidence:
- `GithubAuthRepository` no longer accepts `GithubAppAuthApi` and no longer exposes `signInWithGithubAppInstallation(...)`.
- `AuthViewModel`/`AuthScreen` no longer expose installation-id input or GitHub App broker sign-in actions.
- `PainkillerContainer` no longer wires `appAuthApi`.

Action:
- Keep PAT as the only active auth path.
- Evaluate OAuth Device Flow / OAuth App as the future mobile-friendly path in a later gate.

---

## BUG-20260427-018

Status: FIXED
Gate: 27
Severity: LOW
Summary: Release asset upload now streams selected file content to GitHub without eager ByteArray materialization (single-file scope).

Evidence:
- `UploadFlowViewModel.uploadSelectedFileAsReleaseAsset()` now builds a stream-backed payload from SAF URI metadata.
- `UploadReleaseAssetRequest` now carries `UploadPayload` with `openStream()` + `sizeBytes`.

Action:
- Fixed in Gate 27 by streaming release asset body in `KtorGithubReleaseApi` via `OutgoingContent.WriteChannelContent`.
- Scope remains single-file release uploads only.

---

## BUG-20260427-019

Status: FIXED
Gate: 24.6
Severity: MEDIUM
Summary: GitHub App broker has been removed from the product path by decision; PAT remains active and OAuth Device Flow / OAuth App is the future candidate.

Evidence:
- `BuildConfig.GITHUB_APP_BROKER_BASE_URL` was removed from app build config.
- `RetrofitGithubAppAuthApi` production wiring and broker injection path were removed.
- `tools/github-app-exchange-server` was removed from the repository.

Action:
- Keep PAT as the current login path.
- Do not implement OAuth yet; evaluate OAuth Device Flow / OAuth App in a later gate.

---

## BUG-20260427-020

Status: FIXED
Gate: 28
Severity: LOW
Summary: Some auth and release actions are visible in UI even when build/runtime support is limited; labels and enablement must truthfully reflect availability.

Evidence:
- OAuth section is visible in auth screen while backend exchange may be unavailable in default builds.
- Gate 28 added a dedicated large-file routing panel that marks unsupported source routes as unavailable and keeps their action buttons disabled.
- Gate 27 removed eager ByteArray decoding for release asset uploads.

Action:
- Fixed for large-file routing in Gate 28: action cards now explain availability and keep unsupported routes non-executable.
- OAuth remains future-path work and is out of scope for this gate.

---

## BUG-20260428-021

Status: FIXED
Gate: 27
Severity: LOW
Summary: Git LFS single-file upload now hashes and uploads from stream without eager full-file ByteArray.

Evidence:
- `GithubLfsRepository.uploadSingleFileAndCommitPointer()` now builds LFS oid/size via streaming digest and uploads from `UploadPayload` stream.
- `UploadFlowViewModel.uploadSingleFileViaLfs()` now passes SAF-backed stream payload and remains single-file-only.

Action:
- Fixed in Gate 27 for the single-file LFS path.
- Multi-file/folder/ZIP LFS routing remains out of scope.

---

## BUG-20260428-022

Status: ACCEPTED
Gate: 28
Severity: LOW
Summary: Large-file alternate routes are intentionally limited by source type; multi-file/folder/ZIP LFS and batch Release Asset upload remain unavailable.

Evidence:
- Gate 28 routing model keeps Git LFS executable only for single-file large uploads.
- Gate 28 routing model keeps Release Asset executable only for selected single-file uploads with an explicit selected release.
- Multi-file/folder/ZIP sources show these routes as unavailable with reason text instead of executable actions.

Action:
- Accepted as current product boundary.
- Future gates may expand routing transport support, but Gate 28 intentionally does not.
