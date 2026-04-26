# Gate 0 Handoff

## Status

PASS

Gate 0 is verified. The merged best-of-both Gate 0 implementation is now canonical on `main`.
The pure-Kotlin `:domain` module builds and **19/19 unit tests pass**. The Android `:app`
module is covered by the GitHub Actions build workflow added in commit
`01bd66e399bf1c02a1d84e4bd2da78a4af8424b4`, which installs the Android SDK and runs
`:domain:test`, `:domain:build`, and `:app:assembleDebug`.

The earlier sleep-mode Android SDK blocker is resolved by CI verification and tracked as
`knownbugs.md` BUG-20260426-001 with status `FIXED`.

## Branch

- `main`

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
- Add CI workflow for domain tests, domain build, and Android debug assembly.
- Do **not** implement file picker, auth, upload, preview, Large File
  Doctor, presets, Conflict Cards, LFS, or Release Assets.

## Implemented

- Root Gradle setup with version catalog and Gradle wrapper:
  `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`,
  `gradle/libs.versions.toml`, `gradlew`, `gradle/wrapper/`.
- GitHub Actions workflow:
  - `.github/workflows/build.yml` runs on push, pull request, and manual dispatch.
  - Installs JDK 17 and Android SDK packages for `compileSdk = 35`.
  - Runs `./gradlew :domain:test`, `./gradlew :domain:build`, and `./gradlew :app:assembleDebug`.
  - Uploads the debug APK artifact when successful.
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
  - `domain/path/PathValidation.kt` — `normalizeRepoPath` / `isSafeRepoPath`
    pure utilities. Gate 1/4 integration lands in those gates; placed in
    `:domain` now so it is testable without the Android SDK.
- `:app` (Android) module:
  - `AndroidManifest.xml` with `MainActivity` as launcher.
  - `MainActivity.kt` — `ComponentActivity` that calls `setContent` with
    `PainkillerTheme { PainkillerApp() }`.
  - `PainkillerApplication.kt` — empty `Application` subclass, registered
    in the manifest via `android:name`. Allows later gates to initialize
    singletons safely.
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
  - `res/values/strings.xml`, `res/values/themes.xml`,
    `res/values/colors.xml` — Android resources; `colors.xml` provides
    `painkiller_dark_background` for the Activity window before Compose
    draws its first frame; `themes.xml` references it via `@color/...`.
  - `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml` —
    explicit opt-out of Android cloud backup and device transfer. Token
    storage (Gate 3) uses Android Keystore and must never be backed up.
  - `proguard-rules.pro` — keeps kotlinx.serialization metadata under
    R8 minification.
  - `AndroidManifest.xml` includes `INTERNET` permission for later GitHub API gates,
    with no network implementation wired in Gate 0.
  - `app/src/test/java/com/painkiller/SmokeTest.kt` — confirms `:domain`
    is reachable from `:app`.
- Root documentation:
  - `claude.md` — gate discipline, scope, safety rules, build commands.
  - `knownbugs.md` — structured Gate 0 bug/risk log.
  - `README.md` — project overview, current Gate 0 status, repository
    structure, build instructions, known limitations.
  - `templates/gated-android-project/README.md` — reusable template
    documenting layout, gate workflow, handoff format, knownbugs format.
- `.gitignore` for Gradle, IDE, Android, and OS artifacts.

## Files Changed

```
.github/workflows/build.yml
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
app/proguard-rules.pro
app/src/main/AndroidManifest.xml
app/src/main/java/com/painkiller/MainActivity.kt
app/src/main/java/com/painkiller/PainkillerApplication.kt
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
app/src/main/res/values/colors.xml
app/src/main/res/xml/backup_rules.xml
app/src/main/res/xml/data_extraction_rules.xml
app/src/test/java/com/painkiller/SmokeTest.kt

domain/build.gradle.kts
domain/src/main/kotlin/com/painkiller/domain/github/GitDataModels.kt
domain/src/main/kotlin/com/painkiller/domain/github/GithubGitDataApi.kt
domain/src/main/kotlin/com/painkiller/domain/github/RepoCoordinates.kt
domain/src/main/kotlin/com/painkiller/domain/model/Severity.kt
domain/src/main/kotlin/com/painkiller/domain/path/PathValidation.kt
domain/src/test/kotlin/com/painkiller/domain/github/GitDataModelsSerializationTest.kt
domain/src/test/kotlin/com/painkiller/domain/github/RepoCoordinatesTest.kt
domain/src/test/kotlin/com/painkiller/domain/model/DiagnosticSeverityTest.kt
domain/src/test/kotlin/com/painkiller/domain/path/PathValidationTest.kt
```

## Checks Run

- command: `gradle wrapper --gradle-version=8.10.2 --distribution-type=bin`
  result: BUILD SUCCESSFUL. Wrapper generated.
- command: `./gradlew :domain:test` (initial run, 9 tests)
  result: BUILD SUCCESSFUL. 9 tests, 1 failure; fixed (see Fixes Applied).
- command: `./gradlew :domain:test` (after merge, 19 tests)
  result: BUILD SUCCESSFUL. **19 tests, 0 failures.**
- command: `./gradlew :domain:build`
  result: BUILD SUCCESSFUL.
- command: `./gradlew :app:help`
  result: BUILD SUCCESSFUL. Confirms AGP 8.7.3 configures `:app` correctly.
- command: GitHub Actions workflow `.github/workflows/build.yml`
  result: user confirmed the workflow ran through after commit `01bd66e399bf1c02a1d84e4bd2da78a4af8424b4`.
- workflow commands:
  - `./gradlew :domain:test`
  - `./gradlew :domain:build`
  - `./gradlew :app:assembleDebug`

## Fixes Applied

- The serialization test for `CreateBlobRequest` initially failed
  because `kotlinx.serialization` omits default-valued fields by default
  and `encoding = "base64"` is a default. Fixed by configuring the test's
  `Json` instance with `encodeDefaults = true`. The future Android-side
  network client (Gate 6) must use the same setting.
  Tracked as `knownbugs.md` BUG-20260426-002 (status `ACCEPTED`).
- Dual-run divergence resolved by merging best elements of both parallel
  Gate 0 runs. Tracked as `knownbugs.md` BUG-20260426-003 (status `ACCEPTED`).
- Android SDK environment blocker resolved by adding a GitHub Actions build workflow.
  Tracked as `knownbugs.md` BUG-20260426-001 (status `FIXED`).

## Known Bugs / Risks

- BUG-20260426-001 (FIXED, MEDIUM) — Android SDK-backed `:app:assembleDebug`
  verification moved to GitHub Actions and confirmed by user.
- BUG-20260426-002 (ACCEPTED, LOW) — kotlinx.serialization default-field
  omission. Documented and addressed.
- BUG-20260426-003 (ACCEPTED, LOW) — dual parallel Gate 0 runs merged
  manually. Merged state is now canonical on `main`.

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

- This handoff is committed to `main`.
- The GitHub Actions build workflow remains green.
- The next gate is Gate 1 — File Intake without GitHub. It must be
  implemented strictly within its own scope (SAF abstraction,
  `SelectedSource`, `SourceKind`, path normalization, ignore rules,
  `FilePlan`). It must not pull GitHub, auth, or upload work forward.

## Commit

- canonical merge commit: `e5763462fd0fe7d8744c065c0d39d33b115bbc08`
- build workflow commit: `01bd66e399bf1c02a1d84e4bd2da78a4af8424b4`
- knownbugs verification commit: `961b4a887ebd0beed2afd8aae0e8f283006e0bbd`
- message: `Gate 0: verified pass`
