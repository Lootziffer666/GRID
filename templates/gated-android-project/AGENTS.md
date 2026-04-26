# AGENTS.md — Gated Android Project Agent Instructions

This repository uses gated implementation. Agents must work conservatively, document their work, run available checks, and stop at the selected gate boundary.

The repository files are the source of truth. Do not rely on chat memory.

---

## Project Summary

Replace this section with the project-specific product summary.

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

GitHub Actions is the authoritative build verifier.

Local agents may not have the Android SDK, caches, or full build environment. A missing local Android SDK is **not** a product defect and must not be repeatedly escalated as a gate blocker when CI is expected to verify the build.

Default assumption:

```text
Everything is OK until the user says otherwise, GitHub Actions fails, or a repository file documents a real blocker.
```

Agents should not reopen old environment-only blockers unless new evidence appears.

If GitHub Actions fails, the failing workflow output becomes the source of truth and the current gate must not be marked `PASS` until fixed.

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
- Keep Android-only platform concerns in `:app` or Android-facing adapter interfaces.
- Reuse existing utilities instead of duplicating logic.
- Prefer small immutable data models.
- Prefer explicit result types over throwing for expected validation failures.
- Do not add dependency injection frameworks unless a gate explicitly requires them.
- Do not add networking libraries until the network gate.
- Do not add databases unless a gate explicitly requires them.

---

## Build and Check Commands

Run the most relevant local checks before committing.

For early gates, try:

```bash
./gradlew :domain:test
./gradlew :domain:build
./gradlew :app:assembleDebug
```

If local `:app:assembleDebug` cannot run only because the local environment lacks an Android SDK, document it briefly but do not treat it as a gate blocker. GitHub Actions is responsible for final Android SDK-backed verification.

---

## Commit Rules

Create exactly one coherent commit per gate if the gate is complete.

Commit message format:

```text
Gate X: <short result>
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

---

## knownbugs.md Rules

Maintain `knownbugs.md` as a permanent structured risk and bug log.

Never delete old entries.

When something is fixed, mark it `FIXED`.

When a risk is intentionally accepted, mark it `ACCEPTED` and explain why.

Do not add repeated entries for the same local Android SDK limitation if it is already covered by CI.
