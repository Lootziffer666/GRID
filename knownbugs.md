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

Status: OPEN
Gate: 4
Severity: MEDIUM
Summary: Local Gate 4 verification cannot complete `:app:assembleDebug` because Android SDK is not configured in this execution environment.

Evidence:
- `./gradlew :app:assembleDebug` on 2026-04-26 failed with: "SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file at '/workspace/PAINKILLER/local.properties'."
- `./gradlew :domain:test` and `./gradlew :domain:build` succeeded in the same run.

Action:
- Keep Gate 4 marked `PARTIAL` for this local run.
- Re-run `./gradlew :app:assembleDebug` on CI or SDK-enabled runner before promoting Gate 4 to `PASS`.


## BUG-20260426-008

Status: ACCEPTED
Gate: 6
Severity: LOW
Summary: No `handoff/GATE_5_HANDOFF.md` exists in the repository at the
time Gate 6 was started, but the user explicitly authorized Gate 6 to
proceed ("Previous gates 0–5 are PASS").

Evidence:
- Repository tree at the start of Gate 6 contained
  `handoff/GATE_0_HANDOFF.md` through `handoff/GATE_4_HANDOFF.md` only.
- `git log origin/main` shows no Gate 5 commit.
- The user's Gate 6 prompt asserted gates 0–5 were PASS and authorized
  proceeding with Gate 6.
- AGENTS.md § "Required Reading Before Any Change" allows continuing
  when "the user explicitly says to proceed".

Action:
- Accepted. Gate 6 was implemented to take its inputs directly
  (`SingleFileCommitInput` = `RepoTarget` + `fileName` + `contentBase64`
  + `commitMessage`) so that whatever Gate 5 ultimately ships for the
  preview / `UploadPlan` layer can produce that input shape without any
  change to Gate 6 code.
- Gate 7 (multi-file) requires the full Gate 5 `UploadPlan` /
  per-file size diagnosis machinery, so Gate 5 should be implemented
  before Gate 7 begins.

---

## BUG-20260426-009

Status: ACCEPTED
Gate: 6
Severity: LOW
Summary: Concrete HTTP client implementation of `GithubGitDataApi` is
deferred to a later networking hardening step. Same pattern as Gate 3
(`GithubOAuthApi` and `GithubRepositoryApi` are still contracts).

Evidence:
- `GithubGitDataApi` is an interface in `:domain` only.
- `SingleFileCommitOrchestrator` consumes the interface and is fully
  unit-tested with a recording fake.
- `SingleFileCommitRepository` consumes the same interface from `:app`,
  ready for an HTTP-backed implementation to be injected.

Action:
- Accepted for Gate 6. The networking hardening step that follows must:
  - implement `GithubGitDataApi` over an HTTP client (Retrofit/OkHttp or
    Ktor)
  - throw the appropriate `GithubGitDataException` subtype based on
    HTTP status / transport failure (401 → `AuthRequired`, 403 with
    "protected branch" hint → `ProtectedBranch`, other 403 →
    `PermissionDenied`, 404 → `RefNotFound`, 422 SHA-mismatch →
    `ShaMismatch`, IO/timeout → `NetworkUnavailable`)
  - never log raw token, response body, or stack trace
  - never set `force = true` on `updateRef`
