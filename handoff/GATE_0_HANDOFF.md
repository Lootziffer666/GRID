# Gate 0 Handoff

## Status

PARTIAL

Reason: the Gate 0 skeleton, theme, components, GitHub API spike, and
domain models are all in place, and the pure-Kotlin domain code was
verified by direct `kotlinc` compile + JUnit run (10 tests, all green).
However, the gate's primary acceptance check — `./gradlew test` and
`./gradlew assembleDebug` — could not be run in this environment
because no Android SDK is installed (see `BUG-20260426-001` in
`knownbugs.md`). The next run, on a host with the Android SDK
configured, must execute those commands before Gate 0 can be promoted
to `PASS` and Gate 1 can begin.

## Branch

- Working branch: `claude/magical-thompson-6P6vg`.
- The user-facing prompt suggested `claude/implement-instructions-ymH1Q`,
  but the system-level branch directive for this run pinned
  `claude/magical-thompson-6P6vg`. Per the prompt's guidance ("If the
  current branch is different: do not create a new branch unless
  necessary, document the actual branch in the handoff, continue
  conservatively"), the existing branch was kept.

## Gate Scope

Gate 0 — Project Skeleton + API/UI Spike.

- Android / Kotlin / Jetpack Compose project skeleton.
- Package skeletons for `ui`, `ui.theme`, `ui.components`, `ui.screens`,
  `domain.model`, `domain.usecase`, `data.github`, `data.files`,
  `data.zip`, `data.settings`, `data.security`.
- Painkiller theme (colors, shapes, spacing, typography) using the
  CATALON-GUARD tokens defined in `instructions.md`.
- Reusable UI components: severity badge, info card, warning/error
  card, error banner, primary action button, plus a Gate 0 placeholder
  screen that uses them.
- GitHub Git Data API model + interface spike (no production wiring).
- Root hygiene files: `claude.md`, `knownbugs.md`, `README.md`,
  `templates/gated-android-project/README.md`, `handoff/`.

Explicitly out of scope for Gate 0: file picker, auth, upload, preview,
Large File Doctor, presets, LFS, Release Assets.

## Implemented

- Gradle Kotlin DSL build:
  - `settings.gradle.kts`, root `build.gradle.kts`, `gradle.properties`.
  - Single source of truth for versions in `gradle/libs.versions.toml`
    (AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.10, kotlinx.serialization
    1.7.3, JUnit 4).
  - `app/build.gradle.kts` for the single Android module
    (`com.painkiller.app`, minSdk 26, targetSdk/compileSdk 35).
  - `app/proguard-rules.pro` with serialization keep-rules.
- Gradle wrapper at `gradle/wrapper/`, `gradlew`, `gradlew.bat`
  (Gradle 8.10.2 distribution).
- Android manifest, application class, single activity entry point.
- `res/values/` and `res/xml/` resources matching the manifest, with
  backup and data-extraction rules opting out (no app data is intended
  to be backed up or transferred — tokens land in a Keystore-backed
  store in Gate 3).
- Painkiller theme:
  - `PainkillerColors` — verbatim CATALON-GUARD token palette plus
    severity tints and on-* contrast colors.
  - `PainkillerShapes` — 4 / 12 / 24 dp Material 3 shape set.
  - `PainkillerSpacing` — 4 / 8 / 12 / 16 / 20 / 24 dp rhythm.
  - `PainkillerTypography` — platform-default typography with the
    expected six text roles.
  - `PainkillerTheme` Material 3 wrapper with dark-default + light
    color schemes.
- Reusable components in `ui.components`:
  - `PainkillerSeverityBadge`
  - `PainkillerInfoCard`
  - `PainkillerWarningCard`
  - `PainkillerErrorBanner` (consumes `HumanReadableError`)
  - `PainkillerPrimaryActionButton`
- `PainkillerPlaceholderScreen` — Compose `Scaffold` + `TopAppBar` shell
  that exercises each reusable component. Primary action is disabled to
  signal "no upload in Gate 0".
- Domain models declared in Gate 0 because later layers refer to them:
  - `DiagnosticSeverity` (SAFE / WARNING / BLOCKED / DEFERRED).
  - `HumanReadableError` (title / explanation / data-loss / next-step).
  - `RepoTarget`, `BranchTarget`, `TargetPath` (value class).
- GitHub Git Data API spike:
  - `GitHubApiModels.kt` — `@Serializable` DTOs for blob, tree, commit,
    update-ref, ref, repository summary, branch summary.
  - `GitHubGitDataApi` — interface ordered to match the upload flow
    (read ref → blobs → tree → commit → update ref), with safety
    contract documented inline.
  - `PathValidation` — `normalizeRepoPath` / `isSafeRepoPath` helpers
    that enforce the rules later gates depend on.
- Tests:
  - `PathValidationTest` (8 cases) — empty, simple, backslash, double
    slash, trim, parent traversal, dot segment, Windows absolute,
    safety predicate.
  - `DiagnosticSeverityTest` (1 case) — fixes the four-severity
    vocabulary.
- Root hygiene files: `claude.md`, `knownbugs.md`, `README.md`,
  `templates/gated-android-project/README.md`, this handoff.

## Files Changed

Created (all new in this gate):

- `.gitignore`
- `README.md`
- `claude.md`
- `knownbugs.md`
- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle.properties`
- `gradle/libs.versions.toml`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradlew`, `gradlew.bat`
- `app/build.gradle.kts`
- `app/proguard-rules.pro`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/src/main/java/com/painkiller/app/PainkillerApplication.kt`
- `app/src/main/java/com/painkiller/app/MainActivity.kt`
- `app/src/main/java/com/painkiller/app/ui/PainkillerApp.kt`
- `app/src/main/java/com/painkiller/app/ui/theme/PainkillerColors.kt`
- `app/src/main/java/com/painkiller/app/ui/theme/PainkillerShapes.kt`
- `app/src/main/java/com/painkiller/app/ui/theme/PainkillerSpacing.kt`
- `app/src/main/java/com/painkiller/app/ui/theme/PainkillerTypography.kt`
- `app/src/main/java/com/painkiller/app/ui/theme/PainkillerTheme.kt`
- `app/src/main/java/com/painkiller/app/ui/components/PainkillerSeverityBadge.kt`
- `app/src/main/java/com/painkiller/app/ui/components/PainkillerInfoCard.kt`
- `app/src/main/java/com/painkiller/app/ui/components/PainkillerWarningCard.kt`
- `app/src/main/java/com/painkiller/app/ui/components/PainkillerErrorBanner.kt`
- `app/src/main/java/com/painkiller/app/ui/components/PainkillerPrimaryActionButton.kt`
- `app/src/main/java/com/painkiller/app/ui/screens/PainkillerPlaceholderScreen.kt`
- `app/src/main/java/com/painkiller/app/domain/model/DiagnosticSeverity.kt`
- `app/src/main/java/com/painkiller/app/domain/model/HumanReadableError.kt`
- `app/src/main/java/com/painkiller/app/domain/model/RepoTarget.kt`
- `app/src/main/java/com/painkiller/app/data/github/GitHubApiModels.kt`
- `app/src/main/java/com/painkiller/app/data/github/GitHubGitDataApi.kt`
- `app/src/main/java/com/painkiller/app/data/github/PathValidation.kt`
- `app/src/test/java/com/painkiller/app/data/github/PathValidationTest.kt`
- `app/src/test/java/com/painkiller/app/domain/model/DiagnosticSeverityTest.kt`
- `templates/gated-android-project/README.md`
- `handoff/GATE_0_HANDOFF.md`

## Checks Run

- command: `gradle wrapper --gradle-version 8.10.2 --distribution-type bin`
  result: BUILD SUCCESSFUL — Gradle wrapper generated.
- command: `kotlinc -d <out> -classpath <kotlinx-serialization-core> ...`
  on the pure-Kotlin sources
  (`DiagnosticSeverity.kt`, `HumanReadableError.kt`, `RepoTarget.kt`,
  `PathValidation.kt`, `GitHubApiModels.kt`, `GitHubGitDataApi.kt`).
  result: clean compile, no warnings.
- command: `kotlinc` on the two test files against the compiled output.
  result: clean compile.
- command:
  `java -cp <classes>:<junit>:<hamcrest>:<kotlin-stdlib> org.junit.runner.JUnitCore PathValidationTest DiagnosticSeverityTest`
  result: `OK (10 tests)` — all 10 pure-Kotlin tests passed.
- command (deferred): `./gradlew test`, `./gradlew assembleDebug`.
  result: not run — Android SDK not available in this environment
  (see `BUG-20260426-001` in `knownbugs.md`). The Gradle wrapper itself
  is in place and downloads cleanly, so any host with the SDK can run
  these immediately.

The Kotlin compiler used for the pure-Kotlin verification was the
standalone JetBrains distribution at version 2.0.21, matching the
project's pinned Kotlin version.

## Fixes Applied

- During the verification compile, `GitHubGitDataApi.kt` initially failed
  in isolation because `GitHubApiModels.kt` was not on the source set in
  the first compile invocation. Fixed by including the models file in
  the same `kotlinc` invocation, which is how Gradle will build them
  too. No source changes were needed.
- No bugs were introduced or fixed in product code — Gate 0 is a clean
  green-field implementation.

## Known Bugs / Risks

- `BUG-20260426-001` (ACCEPTED): Android SDK not installed in this
  environment, so `./gradlew test` and `./gradlew assembleDebug` could
  not run during this gate. Documented in `knownbugs.md`. The skeleton
  is structured to build immediately on any host with a standard
  Android SDK matching `compileSdk = 35`.

## Explicitly Not Done

- No file picker / SAF wiring (Gate 1).
- No Large File Doctor logic (Gate 2).
- No GitHub authentication, no token storage, no `SecureTokenStore`
  implementation (Gate 3).
- No repo / branch / target path selection UI (Gate 4).
- No `UploadPlan` generation, no preview screen (Gate 5).
- No real network call to GitHub. The `GitHubGitDataApi` interface is
  a contract only; no implementation class exists.
- No Conflict Cards. No LFS upload. No Release Asset upload. No
  history browser. No PR / branch graph. No background sync.
- No automated UI test or instrumentation test infrastructure was set
  up — that lands when there's UI worth testing (Gate 5+).

## Next Gate May Start Only If

- Gate 0 is promoted from `PARTIAL` to `PASS` by running
  `./gradlew test` and `./gradlew assembleDebug` on a host with the
  Android SDK installed, with both commands green.
- Any failures revealed by that build are fixed inside Gate 0's scope
  before Gate 1 begins.
- The branch directive (`claude/magical-thompson-6P6vg`) is still
  current.
- `instructions.md` § "Gate 1" remains the source of truth for Gate 1
  scope (SAF intake, FilePlan generation, path normalization, ignore
  rules). No GitHub work in Gate 1.

This run does not advance to Gate 1. The autopilot rule "stop
immediately if [the previous gate is not `PASS`]" applies — Gate 1
must be started in a fresh run after Gate 0 reaches `PASS`.

## Commit

- hash: see `git log -1 --format=%H` after this commit lands.
- message: `Gate 0: project skeleton, theme, components, GitHub API spike`.
