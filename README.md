# PAINKILLER

A focused Android tool that solves one mobile pain:

> "I have files / folders / ZIPs on my Android phone. I want them safely
> committed and pushed into a GitHub repository."

That is the whole product. Painkiller is mobile-first, preview-first, and
explicit-confirmation-first. It is not a Git client, not a GitHub Desktop
clone, and not an IDE.

## Current status

**Gate 0 — Project Skeleton + API/UI Spike** (see
`handoff/GATE_0_HANDOFF.md` for the authoritative status of this run).

Gate 0 establishes the foundation only:

- Android / Kotlin / Jetpack Compose project skeleton.
- Package skeletons for `ui`, `domain`, and `data`.
- Painkiller theme using CATALON-GUARD color and shape grammar.
- Reusable base UI components (severity badge, info/warning/error cards,
  primary action button).
- GitHub Git Data API model and interface spike (no production wiring).
- Root hygiene files: `claude.md`, `knownbugs.md`, this README,
  `templates/gated-android-project/`, and `handoff/`.

No upload, no auth, no file picker, no preview screen, no Large File
Doctor — those land in Gate 1+.

## Repository layout

```
PAINKILLER/
├── README.md                       Project overview and gate status.
├── claude.md                       Working instructions for Claude Code.
├── knownbugs.md                    Structured bug / risk log.
├── instructions.md                 Source of truth for gates and scope.
├── handoff/                        One handoff file per gate.
│   └── GATE_0_HANDOFF.md
├── templates/
│   └── gated-android-project/      Reusable template for future gated
│                                   Android projects.
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml          Single source of truth for versions.
│   └── wrapper/                    Gradle wrapper.
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   └── java/com/painkiller/app/
        │       ├── MainActivity.kt
        │       ├── PainkillerApp.kt
        │       ├── ui/theme/        Color, Shape, Type, Theme.
        │       ├── ui/components/   PainkillerSeverityBadge, ...
        │       ├── domain/model/    FilePlan-adjacent skeleton models.
        │       └── data/github/     GitHub Git Data API spike.
        └── test/java/com/painkiller/app/
            └── domain/...           Pure-Kotlin unit tests.
```

The exact app package is `com.painkiller.app`.

## Build and run

Painkiller targets Android. Building the app requires:

- JDK 17+
- Android SDK with platform-tools and a recent compileSdk (35).
- Either an `ANDROID_HOME` / `ANDROID_SDK_ROOT` env var or a
  `local.properties` file at the repository root with `sdk.dir=...`.

Once the environment is set up, the standard commands work:

```bash
./gradlew test            # JVM unit tests for domain logic
./gradlew assembleDebug   # Build the debug APK
```

> **Gate 0 caveat:** the environment that produced this Gate 0 commit did
> not have the Android SDK installed. The Android build was therefore not
> verified in this run. See `BUG-20260426-001` in `knownbugs.md` and the
> Gate 0 handoff for which checks did run.

## What Painkiller intentionally does not do (v0)

- No real Git LFS upload (only diagnoses).
- No GitHub Release Asset upload (only diagnoses).
- No Conflict Cards / merge resolution.
- No background sync.
- No PR / branch graph / blame / history browser.
- No file manager features unrelated to upload.
- No dashboards.

## UI grammar

Painkiller borrows the visual grammar of CATALON-GUARD (color tokens, shape
tokens, compact technical density, status badges) but does not copy its
domain screens. See `app/src/main/java/com/painkiller/app/ui/theme/` for
the concrete tokens.

## License

Not yet declared. To be set in a later gate.
