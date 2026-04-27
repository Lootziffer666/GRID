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
