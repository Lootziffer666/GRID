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

**Gate 1 — file intake without GitHub: PARTIAL.**

Gate 1 domain planning is implemented and verified, but this local environment
still cannot execute `:app:assembleDebug` because no Android SDK is configured.

What is in place through Gate 1:

- Android / Kotlin / Jetpack Compose project skeleton (`:app` module).
- Pure-Kotlin domain module (`:domain`) with:
  - GitHub Git Data API contracts/models from Gate 0.
  - `PathValidation` for safe repo path normalization.
  - Gate 1 file intake models:
    - `SourceKind`, `SelectedSource`, `SelectedSourceItem`
    - `IgnoreRule`, `DefaultIgnoreRules`
    - `PlannedFile`, `FilePlan`, `FilePlanIssue`
    - `FilePlanBuilder` with deterministic ordering, duplicate detection,
      unsafe path rejection, and ignore-rule application.
- Android-facing Gate 1 SAF boundary interface (`SafSourceIntake`) in `:app`
  for single file / multiple files / folder / ZIP intake wiring.
- Gate 1 unit tests in `:domain` covering:
  - single/multiple/folder/ZIP sources
  - root and nested target paths
  - unsafe target path rejection
  - default ignore rules
  - duplicate normalized repo paths
  - empty source rejection
  - deterministic file ordering

See `handoff/GATE_1_HANDOFF.md` for exact implementation and check output.

## Repository structure

```text
PAINKILLER/
├── README.md
├── claude.md                 # working instructions for Claude Code / future contributors
├── knownbugs.md              # structured bug / risk log
├── instructions.md           # product brief and gate plan (source of truth)
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml    # version catalog
│   └── wrapper/              # Gradle wrapper
├── gradlew                   # Gradle wrapper script
├── handoff/
│   └── GATE_X_HANDOFF.md     # one file per attempted gate
├── templates/
│   └── gated-android-project/
│       └── README.md         # reusable template for future gated Android projects
├── domain/                   # pure Kotlin / JVM module
│   ├── build.gradle.kts
│   └── src/
│       ├── main/kotlin/com/painkiller/domain/...
│       └── test/kotlin/com/painkiller/domain/...
└── app/                      # Android application module
    ├── build.gradle.kts
    └── src/
        ├── main/AndroidManifest.xml
        ├── main/java/com/painkiller/...
        ├── main/res/values/{strings,themes}.xml
        └── test/java/com/painkiller/SmokeTest.kt
```

## Build and run

Painkiller targets:

- `compileSdk = 35`
- `minSdk = 26`
- `targetSdk = 35`
- Kotlin `2.0.21`
- Android Gradle Plugin `8.7.3`
- JVM target `17`

### Pure-Kotlin domain checks (no Android SDK required)

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

## Known limitations (post Gate 1)

- SAF implementation behind `SafSourceIntake` is not wired yet (interface only).
- No GitHub authentication.
- No upload, commit, or push behavior.
- No preview screen.
- No Large File Doctor.
- No presets.
- The primary action button in the app shell is intentionally disabled.

These arrive in later gates. Painkiller is built one gate at a time and no gate
ships features that belong to a later gate.

## Out of scope (whole project)

- Git LFS upload (only diagnosed, not executed).
- GitHub Release Asset upload (only diagnosed).
- Conflict Cards / automatic merge resolution.
- PRs, branch graphs, full Git history.
- Background sync.
- A general-purpose file manager.

## Related references

- `instructions.md` — full product brief, gate plan, error message style,
  and CATALON-GUARD UI grammar.
- `handoff/GATE_1_HANDOFF.md` — Gate 1 implementation status and command output.
- `knownbugs.md` — structured bug/risk log including local Android SDK blocker.
- `templates/gated-android-project/README.md` — reusable structure for starting
  other gated Android projects in this style.
