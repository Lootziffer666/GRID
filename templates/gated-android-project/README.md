# Gated Android Project Template

Reusable shape for an Android project built using the Painkiller-style
gated workflow. Use this as the starting structure for any future
single-purpose Android tool that should be implemented in small,
auditable steps.

## Why "gated"

Each gate is a small, self-contained slice of work with a clear scope,
acceptance criteria, hard stop, and one of three outcomes:

- `PASS` — fully complete, with checks run and a clean commit.
- `PARTIAL` — meaningful work landed, but the gate is not complete.
- `BLOCKED` — progress cannot continue safely.

You only advance to gate `N+1` after gate `N` is `PASS`. This trades a
slower start for a much higher confidence baseline at every commit.

## Recommended root layout

```
project-root/
├── README.md                       Overview + current gate status.
├── claude.md                       Working instructions for Claude Code.
├── knownbugs.md                    Structured bug / risk log.
├── instructions.md                 Authoritative gate definitions.
├── handoff/
│   ├── GATE_0_HANDOFF.md
│   ├── GATE_1_HANDOFF.md
│   └── ...
├── templates/                      (optional) reusable scaffolds.
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   └── java/<package>/
        │       ├── ui/
        │       ├── ui/theme/
        │       ├── ui/components/
        │       ├── domain/model/
        │       ├── domain/usecase/
        │       └── data/...
        └── test/java/<package>/
```

## Required root files

### `claude.md`

Captures:

- Project scope boundaries.
- Fixed architecture decision.
- Gate discipline.
- Routine commands and checks.
- Safety rules (no destructive git operations, no skipping hooks, etc).

### `knownbugs.md`

Append-only structured log. Every issue gets:

```
## BUG-YYYYMMDD-NNN

Status: OPEN | FIXED | ACCEPTED | BLOCKED
Gate: X
Severity: LOW | MEDIUM | HIGH
Summary: ...

Evidence:
- ...

Action:
- ...
```

Never delete entries. Mark them `FIXED` or `ACCEPTED` instead.

### `README.md`

Short, honest, current. Explains what the project is, what it is not,
the current gate, and how to build/run on a host with the right tooling.

### `handoff/GATE_X_HANDOFF.md`

One file per gate. Use this exact structure:

```
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

## Gate cadence

Each gate must:

1. State its goal up front.
2. Inspect existing files before changing them.
3. Make the smallest useful implementation.
4. Add or update tests.
5. Run the most meaningful checks the environment supports.
6. Update `knownbugs.md`.
7. Write `handoff/GATE_X_HANDOFF.md`.
8. Commit exactly one coherent change with message `Gate X: <result>`.

If a check is unavailable in the environment (e.g. no Android SDK on the
build host), document that honestly in the handoff and in
`knownbugs.md`. Never fake a green check.

## Build / check expectations

Default Android checks:

```bash
./gradlew test
./gradlew assembleDebug
```

If those checks cannot be meaningfully run yet, document:

- Which checks were expected.
- Why they could not run.
- What must exist before they can run.

## Suggested first gate (Gate 0)

Foundation only — no product features.

- Android / Kotlin / Compose skeleton.
- Package skeleton: `ui`, `ui.theme`, `ui.components`, `domain.model`,
  `domain.usecase`, `data.*`.
- Theme tokens.
- Reusable base UI components.
- Domain model + API model spike.
- Root hygiene files (`claude.md`, `knownbugs.md`, `README.md`).
- Optional minimal pure-Kotlin domain test.

Anything beyond foundation belongs to a later gate.

## Anti-patterns to avoid

- Skipping a gate because it "feels small."
- Bundling two gates into one commit.
- Adding dependencies, abstractions, or screens that the current gate
  does not require.
- Refactoring unrelated code while implementing a gate.
- Marking a gate `PASS` without running the checks it advertises.
- Hardcoding secrets or storing tokens outside an explicit secure store
  abstraction.
