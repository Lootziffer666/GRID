# AGENTS.md — PAINKILLER Agent Instructions

This repository uses gated implementation. Agents must work conservatively, document their work, run available checks, and stop at the selected gate boundary.

The repository files are the source of truth. Do not rely on chat memory.

---

## Project Summary

PAINKILLER is a focused Android tool for one mobile pain:

> I have files, folders, or ZIP archives on my Android phone. I want them safely committed and pushed into a GitHub repository.

PAINKILLER is intentionally narrow. It is not a full Git client, not an IDE, not GitHub Desktop, and not a general-purpose file manager.

---

## Required Reading Before Any Change

Before modifying files, read:

- `instructions.md`
- `README.md`
- `claude.md`
- `knownbugs.md`
- latest relevant `handoff/GATE_X_HANDOFF.md`
- current project tree
- recent commits

If the previous gate is documented as `PASS`, continue with the selected gate.

If the previous gate is documented as `PARTIAL` or `BLOCKED`, continue only when one of these is true:

- the user explicitly says to proceed
- the reason is clearly an environment-only local-build limitation already covered by GitHub Actions
- `main` is green and no later note from the user says otherwise

---

## CI-First Gate Policy

GitHub Actions is the authoritative build verifier for PAINKILLER.

Local agents may not have the Android SDK, caches, or full build environment. A missing local Android SDK is **not** a product defect and must not be repeatedly escalated as a gate blocker when CI is expected to verify the build.

Default assumption:

```text
Everything is OK until the user says otherwise, GitHub Actions fails, or a repository file documents a real blocker.
```

For follow-up prompts, agents should treat the previous gate as passed when:

- the previous gate handoff is `PASS`, or
- the user says the workflow/build passed, or
- the relevant build workflow on `main` is green, or
- the only unresolved issue is a local/environment-only build limitation already delegated to CI.

Agents should not reopen old environment-only blockers unless new evidence appears.

If GitHub Actions fails, the failing workflow output becomes the source of truth and the current gate must not be marked `PASS` until fixed.

---

## Current Gate State

Gate 0 is verified as `PASS`.

Gate 1 is the next implementation gate:

```text
Gate 1 — File Intake without GitHub
```

Gate 1 must not implement GitHub-facing behavior.

---

## Global Hard Rules

- Implement exactly one gate per task unless explicitly instructed otherwise.
- Do not skip gates.
- Do not merge multiple gates into one commit.
- Do not invent features.
- Do not perform broad cleanup.
- Do not rewrite working code for style.
- Do not introduce unrelated dependencies.
- Do not change product scope.
- Do not silently skip checks.
- Do not claim success without command output or CI confirmation.
- Do not commit secrets, tokens, passwords, API keys, or local config files.
- Do not hardcode credentials.
- Do not implement destructive behavior before the relevant gate explicitly allows it.

---

## Architecture Rules

- Keep pure planning, validation, and file-analysis logic in `:domain` whenever possible.
- `:domain` must not import Android packages.
- Keep Android-only SAF, URI, and platform concerns in `:app` or Android-facing adapter interfaces.
- Reuse existing utilities instead of duplicating logic.
- Reuse `PathValidation` for path normalization and path safety checks.
- Prefer small immutable data models.
- Prefer explicit result types over throwing for expected validation failures.
- Do not add dependency injection frameworks unless a later gate explicitly requires them.
- Do not add networking libraries until the GitHub/network gate.
- Do not add databases unless a later gate explicitly requires them.
- Do not add DataStore unless the selected gate explicitly requires it.

---

## Build and Check Commands

Run the most relevant local checks before committing.

For Gate 1 and most early gates, try:

```bash
./gradlew :domain:test
./gradlew :domain:build
./gradlew :app:assembleDebug
```

If local `:app:assembleDebug` cannot run only because the local environment lacks an Android SDK, document it briefly but do not treat it as a gate blocker. GitHub Actions is responsible for final Android SDK-backed verification.

If a check fails because of project code, fix only blockers related to the selected gate.

If a check cannot run because of the environment, document the reason in the handoff. Add or update `knownbugs.md` only when the issue is new, recurring, or actionable.

---

## Commit Rules

Create exactly one coherent commit per gate if the gate is complete.

Commit message format:

```text
Gate X: <short result>
```

For Gate 1:

```text
Gate 1: implement file intake planning
```

If blocked by a real project issue:

```text
Gate X: blocked - <short reason>
```

Do not create a blocked commit solely because a local Android SDK is unavailable while CI is configured to verify Android builds.

Do not commit unrelated files.

---

## Handoff Rules

Every attempted gate must create or update:

```text
handoff/GATE_X_HANDOFF.md
```

Use this format:

```md
# Gate X Handoff

## Status

PASS | PARTIAL | BLOCKED

## Gate Scope

- ...

## Implemented

- ...

## Files Changed

- ...

## Checks Run

- command:
- result:

## CI Status

- workflow:
- result:

## Known Bugs / Risks

- ...

## Explicitly Not Done

- ...

## Next Gate May Start Only If

- ...
```

A later gate may start when the previous gate is `PASS`, or when the user explicitly says to proceed and the only unresolved issue is local/environment-only verification delegated to CI.

---

## knownbugs.md Rules

Maintain `knownbugs.md` as a permanent structured risk and bug log.

Never delete old entries.

When something is fixed, mark it `FIXED`.

When a risk is intentionally accepted, mark it `ACCEPTED` and explain why.

Do not add repeated entries for the same local Android SDK limitation if it is already covered by CI.

Entry format:

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

# Gate 1 Instructions — File Intake without GitHub

## Mission

Implement Gate 1 only:

```text
File Intake without GitHub
```

Do not start Gate 2.

---

## Gate 1 May Include

- `SelectedSource`
- `SourceKind`
- source metadata models
- file source representation
- multiple-file source representation
- folder source representation
- ZIP source representation as a source kind
- path normalization integration using existing `PathValidation`
- ignore-rule model
- default ignore rules
- `FilePlan`
- pure Kotlin planning logic where possible
- Android SAF-facing interfaces or skeletons if needed
- package placeholders only where implementation belongs to later gates
- unit tests for all pure Kotlin logic
- documentation and handoff updates

---

## Gate 1 Hard Non-Goals

Do not implement:

- GitHub auth
- token storage
- repository picker
- branch picker
- network calls
- upload
- commit creation
- push/update-ref behavior
- preview screen as a real workflow
- Large File Doctor
- Conflict Cards
- Git LFS
- Release Assets
- background upload worker
- broad UI redesign
- unrelated refactors

---

## Suggested Gate 1 Implementation Shape

Suggested domain package:

```text
domain/src/main/kotlin/com/painkiller/domain/files/
```

Possible files:

```text
SourceKind.kt
SelectedSource.kt
PlannedFile.kt
FilePlan.kt
IgnoreRule.kt
DefaultIgnoreRules.kt
FilePlanBuilder.kt
FilePlanIssue.kt
```

Suggested tests:

```text
domain/src/test/kotlin/com/painkiller/domain/files/
```

Test cases should cover:

- single file source
- multiple file source
- folder source
- ZIP source as source kind, without extraction logic unless explicitly Gate 1
- root target path
- nested target path
- unsafe target path rejected via `PathValidation`
- default ignored files
- duplicate normalized paths
- empty source list rejected
- deterministic ordering in generated `FilePlan`

---

## Gate 1 Documentation Updates

Update:

- `README.md`
- `knownbugs.md` only if new bugs/risks are discovered
- `handoff/GATE_1_HANDOFF.md`

Gate 1 may be marked `PASS` when the local pure-Kotlin checks pass and Android verification is either locally green or delegated to a configured GitHub Actions workflow. If GitHub Actions later fails, reopen the gate and fix the failure.

---

## Gate 1 Stop Rule

Stop after Gate 1.

Do not start Gate 2.

Do not implement GitHub-facing behavior.

Do not implement upload behavior.
