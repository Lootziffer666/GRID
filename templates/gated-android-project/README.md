# Gated Android Project Template

A small, opinionated structure for starting a new Android / Kotlin / Compose
project that is built **one gate at a time** with strict scope discipline.
This is the same structure Painkiller uses. It exists here so future
projects can be bootstrapped without re-deriving it.

## Why a gate-driven structure

- Each gate has a narrow, well-defined scope.
- Each gate ends with a written handoff and a clean commit.
- Each gate produces a verifiable artifact (build, tests, or a documented
  reason a check could not run).
- A gate may end as `PASS`, `PARTIAL`, or `BLOCKED`. The next gate may
  only start after the previous gate's status is `PASS`.
- No gate may silently absorb the work of another gate.

This makes the project easy to audit, easy to resume after a long gap,
and resistant to scope creep.

## Recommended repository layout

```
<project>/
├── README.md
├── claude.md                 # working instructions for AI / human contributors
├── knownbugs.md              # structured bug / risk log; entries are never deleted
├── instructions.md           # product brief and gate plan (source of truth)
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml    # version catalog (single source of truth for versions)
│   └── wrapper/
├── gradlew
├── handoff/
│   ├── GATE_0_HANDOFF.md
│   ├── GATE_1_HANDOFF.md
│   └── ...
├── templates/                # reusable project templates (this folder)
├── domain/                   # pure Kotlin / JVM module
│   └── src/{main,test}/kotlin/...
└── app/                      # Android application module
    └── src/main/...
```

## Module split

- `:domain` is pure Kotlin / JVM. It holds domain models, validators, and
  interface contracts for external systems (e.g. APIs). It can be unit
  tested without an Android SDK.
- `:app` is the Android application module. It depends on `:domain`.
- The boundary is one-way. Domain code must never import Android APIs.

This is the lightest possible separation that makes domain logic
verifiable on any developer machine, even without the Android SDK.

## Required root hygiene files

| File | Purpose |
|------|---------|
| `claude.md` | Working instructions: gate discipline, scope, safety rules. |
| `knownbugs.md` | Structured log of bugs / risks. Entries are append-only. |
| `README.md` | Project overview, current gate status, build instructions. |
| `instructions.md` | Product brief and gate plan. Source of truth for scope. |

## Gate workflow

For each gate:

1. Read `instructions.md` and the most recent handoff.
2. State the gate's exact scope.
3. Implement only that scope.
4. Add tests for everything testable in pure Kotlin.
5. Run the most meaningful checks the environment supports:
   ```bash
   ./gradlew :domain:test
   ./gradlew :app:assembleDebug          # requires Android SDK
   ./gradlew :app:testDebugUnitTest
   ```
6. Update `knownbugs.md` for any issue, blocker, or accepted risk.
7. Write `handoff/GATE_X_HANDOFF.md` with one of these statuses:
   - `PASS` — scope complete, checks ran, no unresolved blockers.
   - `PARTIAL` — meaningful work done, something remains unresolved.
   - `BLOCKED` — progress cannot continue safely.
8. Commit once for the gate. Suggested message form:
   ```
   Gate X: <short result>
   Gate X: blocked - <short reason>
   Gate X: document blocked state
   ```
9. If status is not `PASS`, **stop**. Do not start the next gate.

## Handoff template

```md
# Gate X Handoff

## Status
PASS | PARTIAL | BLOCKED

## Branch
- ...

## Gate Scope
- ...

## Implemented
- ...

## Files Changed
- ...

## Checks Run
- command:
- result:

## Fixes Applied
- ...

## Known Bugs / Risks
- ...

## Explicitly Not Done
- ...

## Next Gate May Start Only If
- ...

## Commit
- hash:
- message:
```

## knownbugs.md entry format

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

Entries are never deleted. If a bug is fixed, change `Status` to `FIXED`
and keep the entry.

## Scope discipline rules

- No unrelated refactors during a gate.
- No invented features. No future-gate features.
- No dependency changes unless required by the current gate.
- No silent test skips.
- No claiming success without evidence.
- No destructive cleanup of unfamiliar files.
- No backwards-compat hacks for code that was never released.

## Build / version baseline (recommended)

- JDK 17 toolchain
- Kotlin 2.0.x
- Android Gradle Plugin 8.7.x
- Gradle 8.10.x via the wrapper
- `compileSdk` = current stable Android SDK
- `minSdk` chosen for the actual feature surface

Pin all versions in `gradle/libs.versions.toml`. Do not put version strings
in module build files.
