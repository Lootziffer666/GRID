# Gate 0 Handoff

## Status

PARTIAL

Code deliverables for Gate 0 are in place. The verifiable subset — the
pure-Kotlin `:domain` module — builds and all 9 unit tests pass. AGP
8.7.3 configures the Android `:app` module cleanly (`:app:help` succeeds).
The Android assembly check (`:app:assembleDebug`) was not executed in
this run because the sleep-mode environment does not have an Android SDK
installed; this is an environmental limit, not a project defect. Per
explicit user direction, missing `:app:assembleDebug` is not treated as
a hard stop for Gate 0, and Gate 1 is permitted to begin once this
partial state is committed and pushed.

## Branch

- `claude/magical-thompson-eFEvQ` (current working branch; this matches
  the branch named in the system Git Development Branch Requirements
  block, not the one named in the task body).

## Gate Scope

- Verify or create the Android / Kotlin / Compose project skeleton.
- Add package / module skeletons for `ui`, `ui.theme`, `ui.components`,
  `domain`, `domain.model`, `domain.usecase`, `data.github`, `data.files`,
  `data.zip`, `data.settings`, `data.security`.
- Add a minimal Compose app shell.
- Add Painkiller theme using CATALON-GUARD color and shape grammar.
- Add minimal reusable UI components (severity badge, info card,
  warning card, error banner, primary action button).
- Spike the GitHub Git Data API as interfaces and models only — no
  network, no implementation.
- Add root hygiene files: `claude.md`, `knownbugs.md`, `README.md`,
  `templates/gated-android-project/`.
- Add minimal tests (model serialization, validation).
- Do **not** implement file picker, auth, upload, preview, Large File
  Doctor, presets, Conflict Cards, LFS, or Release Assets.

## Implemented

- Root Gradle setup with version catalog and Gradle wrapper:
  `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`,
  `gradle/libs.versions.toml`, `gradlew`, `gradle/wrapper/`.
- `:domain` (pure Kotlin / JVM) module:
  - `GitDataModels.kt` — kotlinx.serialization request / response types
    for `GitRef`, `GitCommit`, `CreateBlob{Request,Response}`,
    `CreateTree{Request,Response}`, `TreeEntry` (with file-mode
    constants), `CreateCommit{Request,Response}`, `UpdateRefRequest`,
    `GithubRepositorySummary`, `GithubBranchSummary`.
  - `GithubGitDataApi.kt` — `GithubGitDataApi` and
    `GithubRepositoryApi` interfaces. Method-level documentation reminds
    later gates that `force=true` ref updates are not allowed without an
    audited future feature.
  - `RepoCoordinates.kt` — value type with narrow validation (non-blank
    owner / repo / branch). Provides `fullName` and `refPath`.
  - `Severity.kt` — `DiagnosticSeverity` enum (`SAFE`, `WARNING`,
    `BLOCKED`, `DEFERRED`) shared between the domain layer and UI.
- `:app` (Android) module:
  - `AndroidManifest.xml` with `MainActivity` as launcher.
  - `MainActivity.kt` — `ComponentActivity` that calls `setContent` with
    `PainkillerTheme { PainkillerApp() }`.
  - `ui/PainkillerApp.kt` — Material 3 `Scaffold` + `TopAppBar`, an
    `InfoCard`, a `WarningCard` (severity = `DEFERRED`), and a disabled
    primary action button. Includes a Compose `@Preview`.
  - `ui/theme/Color.kt`, `Shape.kt`, `Type.kt`, `Spacing.kt`, `Theme.kt`
    — color tokens (`#FF5A5F`, `#00A699`, `#F7B731`, `#1A1A1A`,
    `#222222`, `#2E2E2E`, `#F5F5F5`, `#FFFFFF`, `#F0F0F0`), shape
    tokens (4 / 12 / 24 dp), spacing (8 / 12 / 16 / 20 dp), light and
    dark `ColorScheme`s, and a `PainkillerTheme` wrapper.
  - `ui/components/PainkillerSeverityBadge.kt`,
    `PainkillerInfoCard.kt`, `PainkillerWarningCard.kt`,
    `PainkillerErrorBanner.kt`, `PainkillerPrimaryActionButton.kt`.
  - `data/{github,files,zip,settings,security}/Placeholder*.kt` —
    package markers naming the gate that will fill each package.
  - `ui/screens/PlaceholderScreens.kt` — package marker for upload-flow
    screens (Gates 1+).
  - `res/values/strings.xml`, `res/values/themes.xml` — minimal Android
    resources required by the manifest.
  - `app/src/test/java/com/painkiller/SmokeTest.kt` — confirms `:domain`
    is reachable from `:app`.
- Root documentation:
  - `claude.md` — gate discipline, scope, safety rules, build commands.
  - `knownbugs.md` — entry format and the two Gate 0 entries.
  - `README.md` — project overview, current Gate 0 status, repository
    structure, build instructions, known limitations.
  - `templates/gated-android-project/README.md` — reusable template
    documenting layout, gate workflow, handoff format, knownbugs format.
- `.gitignore` for Gradle, IDE, Android, and OS artifacts.

## Files Changed

```
.gitignore
README.md
build.gradle.kts
claude.md
gradle.properties
gradle/libs.versions.toml
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
gradlew
gradlew.bat
handoff/GATE_0_HANDOFF.md
knownbugs.md
settings.gradle.kts
templates/gated-android-project/README.md

app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/java/com/painkiller/MainActivity.kt
app/src/main/java/com/painkiller/data/files/PlaceholderFiles.kt
app/src/main/java/com/painkiller/data/github/PlaceholderGithub.kt
app/src/main/java/com/painkiller/data/security/PlaceholderSecurity.kt
app/src/main/java/com/painkiller/data/settings/PlaceholderSettings.kt
app/src/main/java/com/painkiller/data/zip/PlaceholderZip.kt
app/src/main/java/com/painkiller/ui/PainkillerApp.kt
app/src/main/java/com/painkiller/ui/components/PainkillerErrorBanner.kt
app/src/main/java/com/painkiller/ui/components/PainkillerInfoCard.kt
app/src/main/java/com/painkiller/ui/components/PainkillerPrimaryActionButton.kt
app/src/main/java/com/painkiller/ui/components/PainkillerSeverityBadge.kt
app/src/main/java/com/painkiller/ui/components/PainkillerWarningCard.kt
app/src/main/java/com/painkiller/ui/screens/PlaceholderScreens.kt
app/src/main/java/com/painkiller/ui/theme/Color.kt
app/src/main/java/com/painkiller/ui/theme/Shape.kt
app/src/main/java/com/painkiller/ui/theme/Spacing.kt
app/src/main/java/com/painkiller/ui/theme/Theme.kt
app/src/main/java/com/painkiller/ui/theme/Type.kt
app/src/main/res/values/strings.xml
app/src/main/res/values/themes.xml
app/src/test/java/com/painkiller/SmokeTest.kt

domain/build.gradle.kts
domain/src/main/kotlin/com/painkiller/domain/github/GitDataModels.kt
domain/src/main/kotlin/com/painkiller/domain/github/GithubGitDataApi.kt
domain/src/main/kotlin/com/painkiller/domain/github/RepoCoordinates.kt
domain/src/main/kotlin/com/painkiller/domain/model/Severity.kt
domain/src/test/kotlin/com/painkiller/domain/github/GitDataModelsSerializationTest.kt
domain/src/test/kotlin/com/painkiller/domain/github/RepoCoordinatesTest.kt
```

## Checks Run

- command: `gradle wrapper --gradle-version=8.10.2 --distribution-type=bin`
  result: BUILD SUCCESSFUL. Wrapper generated.
- command: `./gradlew :domain:test`
  result: BUILD SUCCESSFUL. 9 tests, 0 failures (after the fix below).
- command: `./gradlew :domain:build`
  result: BUILD SUCCESSFUL.
- command: `./gradlew :app:help`
  result: BUILD SUCCESSFUL. Confirms AGP 8.7.3 configures `:app` correctly.
- command: `./gradlew :app:assembleDebug`
  result: FAILED — "SDK location not found." This is an environmental
  limit (no Android SDK installed in the runner), not a project defect.
  See `knownbugs.md` BUG-20260426-001. Per user direction this is not a
  hard stop for Gate 0.

## Fixes Applied

- The serialization test for `CreateBlobRequest` initially failed
  because `kotlinx.serialization` omits default-valued fields by
  default and `encoding = "base64"` is a default. Fixed by configuring
  the test's `Json` instance with `encodeDefaults = true`. The future
  Android-side network client (Gate 6) must use the same setting so
  GitHub always receives an explicit `"encoding": "base64"`.
  Tracked as `knownbugs.md` BUG-20260426-002 (status `ACCEPTED`).

## Known Bugs / Risks

- BUG-20260426-001 (OPEN, MEDIUM) — `:app:assembleDebug` not yet
  validated end-to-end because the sleep-mode environment lacks an
  Android SDK. Project Gradle / AGP configuration is otherwise valid.
  Action: validate on a machine with `compileSdk = 35` installed.
- BUG-20260426-002 (ACCEPTED, LOW) — kotlinx.serialization default-
  field omission. Documented and addressed.

## Explicitly Not Done

- No file picker.
- No SAF abstraction.
- No ZIP analysis.
- No GitHub authentication.
- No `SecureTokenStore` implementation. Only a `data.security` package
  marker exists.
- No real network calls. Only API interface contracts and request /
  response models exist.
- No upload, commit, or push behavior.
- No preview screen.
- No Large File Doctor.
- No presets / DataStore wiring.
- No Conflict Cards, no LFS upload, no Release Asset upload.

## Next Gate May Start Only If

- This handoff is committed.
- The branch is pushed.
- The next gate is Gate 1 — File Intake without GitHub. It must be
  implemented strictly within its own scope (SAF abstraction,
  `SelectedSource`, `SourceKind`, path normalization, ignore rules,
  `FilePlan`). It must not pull GitHub, auth, or upload work forward.
- BUG-20260426-001 should be cleared on a machine with the Android SDK
  before any later gate that adds Android-only behavior is shipped to
  users.

## Commit

- hash: `15ac2d1ccc9f147cb44328cce42d2a6ddb6b5277`
- message: `Gate 0: partial Android skeleton and domain verification`
