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
- **Gate 4 — RepoTarget + presets: PARTIAL (implemented locally, Android assemble not runnable in this container).**

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
- No upload, commit, or push behavior.
- No preview screen.
- Preset selection UI is not wired yet (storage/model support exists).
- The primary action button in the app shell is intentionally disabled.

## Out of scope (whole project)

- Git LFS upload (only diagnosed, not executed).
- GitHub Release Asset upload (only diagnosed).
- Conflict Cards / automatic merge resolution.
- PRs, branch graphs, full Git history.
- Background sync.
- A general-purpose file manager.
