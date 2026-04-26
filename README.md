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

**Gate 0 — project skeleton + UI/API spike: PARTIAL.**

Code deliverables are complete; the only outstanding item is an end-to-end
Android assembly check on a machine with the Android SDK installed. The
current state is the merged best of two parallel Gate 0 runs (see
`knownbugs.md` BUG-20260426-003).

What is in place in Gate 0:

- Android / Kotlin / Jetpack Compose project skeleton (`:app` module),
  including `PainkillerApplication`, manifest with `INTERNET` permission
  and explicit Android backup / data-extraction opt-out, `proguard-rules.pro`
  for kotlinx.serialization, and the standard `xml/` and `values/` resources.
- Pure-Kotlin domain module (`:domain`) holding:
  - GitHub Git Data API request / response models (kotlinx.serialization).
  - `GithubGitDataApi` and `GithubRepositoryApi` interface contracts (no
    implementation, no network calls).
  - `RepoCoordinates` value type with narrow validation.
  - `DiagnosticSeverity` enum (consumed by the UI severity badge).
  - `PathValidation` (`normalizeRepoPath` / `isSafeRepoPath`) pure utility,
    placed in `:domain/path/` so it is testable without the Android SDK.
    Full Gate 1 / 4 integration lands in those later gates.
- Painkiller theme: colors and shapes lifted directly from CATALON-GUARD as
  specified in `instructions.md` (`#FF5A5F`, `#00A699`, `#F7B731`, dark and
  light surfaces, 4 / 12 / 24 dp shape grammar, 8 / 12 / 16 / 20 dp spacing),
  plus an explicit Material 3 type scale.
- Reusable Compose components: `PainkillerSeverityBadge`, `PainkillerInfoCard`,
  `PainkillerWarningCard`, `PainkillerErrorBanner`,
  `PainkillerPrimaryActionButton`.
- Minimal app shell: `MainActivity` + `PainkillerApp` with a Material 3
  Scaffold, a TopAppBar, an info card, a deferred warning card, and a
  disabled primary action button.
- Package skeletons for `data.github`, `data.files`, `data.zip`,
  `data.settings`, `data.security`, and `ui.screens`.
- **19 unit tests in `:domain`**, all PASS via `./gradlew :domain:test`:
  - 5 GitHub API model serialization tests
  - 4 `RepoCoordinates` validation tests
  - 9 `PathValidation` tests
  - 1 `DiagnosticSeverity` test

Intentionally **not** in Gate 0 even though the parallel run included
them: `RepoTarget` / `BranchTarget` / `TargetPath` (Gate 4 scope) and
`HumanReadableError` (Gate 8 scope).

Why `PARTIAL`, not `PASS`: the sleep-mode environment used to assemble Gate 0
does not have an Android SDK installed, so `./gradlew :app:assembleDebug`
cannot run there. The Gradle / AGP configuration itself is valid (`:app:help`
configures cleanly). See `handoff/GATE_0_HANDOFF.md` and `knownbugs.md`
(BUG-20260426-001) for the exact details and the next concrete action.

## Repository structure

```
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
│       ├── main/kotlin/com/painkiller/domain/
│       │   ├── github/       # API models + interface contracts + RepoCoordinates
│       │   ├── model/        # DiagnosticSeverity
│       │   └── path/         # PathValidation
│       └── test/kotlin/com/painkiller/domain/...
└── app/                      # Android application module
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── main/AndroidManifest.xml
        ├── main/java/com/painkiller/
        │   ├── MainActivity.kt
        │   ├── PainkillerApplication.kt
        │   ├── ui/{theme,components,screens}/...
        │   └── data/{github,files,zip,settings,security}/...
        ├── main/res/values/{strings,themes,colors}.xml
        ├── main/res/xml/{backup_rules,data_extraction_rules}.xml
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

These run anywhere with JDK 17+ and an internet connection (first run
downloads the Gradle and Maven dependencies).

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

`./gradlew :app:assembleDebug` is currently expected to succeed on any
machine with a `compileSdk = 35` Android SDK installed. It has not yet been
verified in this build environment because no Android SDK is present.

## Known limitations (Gate 0)

- No file picker.
- No GitHub authentication.
- No upload, commit, or push behavior.
- No preview screen.
- No Large File Doctor.
- No presets.
- The primary action button in the app shell is intentionally disabled.

These all arrive in later gates. Painkiller is built one gate at a time and
no gate ships features that belong to a later gate.

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
- `handoff/GATE_0_HANDOFF.md` — what was implemented this run, what was
  verified, and what is blocked.
- `knownbugs.md` — every known issue, including the missing-Android-SDK
  blocker for `:app:assembleDebug` in this environment.
- `templates/gated-android-project/README.md` — reusable structure for
  starting other gated Android projects in this style.
