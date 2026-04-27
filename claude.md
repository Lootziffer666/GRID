# claude.md — Painkiller working instructions

This file is project-specific guidance for Claude Code (and any future agent
or human contributor) working on Painkiller. The source of truth for product
scope is `instructions.md`. The source of truth for current state is the
repository itself plus the latest handoff in `handoff/`.

## What Painkiller is

Painkiller is a focused Android tool that solves one mobile pain:

> "I have files / folders / ZIPs on my Android phone. I want them safely
> committed and pushed into a GitHub repository."

It is not a Git client. It is not GitHub Desktop. It is not an IDE.

## Architecture (already decided — do not relitigate)

- Android, Kotlin, Jetpack Compose
- Pragmatic MVVM/MVI with ViewModel + StateFlow
- Coroutines
- Direct REST calls to the GitHub Git Data API (Retrofit/OkHttp or Ktor)
- kotlinx.serialization
- DataStore for non-secret presets
- `SecureTokenStore` abstraction backed by Android Keystore / AndroidX Security
- Storage Access Framework (SAF) for file/folder/ZIP intake
- `java.util.zip` for ZIP analysis with ZIP-Slip prevention
- GitHub Git Data API multi-file single-commit flow (blobs → tree → commit → ref)

Default v0 out-of-scope: JGit, libgit2, local clones, full Git history, branch
graph, PRs, Conflict Cards, real Git LFS upload, Release Asset upload,
background sync — unless the user explicitly requests scope expansion in a
new gate plan.

## Gate discipline

Painkiller is built one gate at a time. The gates are defined in
`instructions.md`:

- Gate 0 — project skeleton + UI/API spike
- Gate 1 — file intake (no GitHub)
- Gate 2 — Large File Doctor (pure domain)
- Gate 3 — GitHub auth + repo/branch listing
- Gate 4 — RepoTarget + presets
- Gate 5 — UploadPlan + preview UI
- Gate 6 — single-file commit via Git Data API
- Gate 7 — multi-file / folder / ZIP commit + `.gitkeep`
- Gate 8 — robustness / error mapping
- Gate 9 — v0 release candidate
- Gate 10–14 — integration wiring, Android adapters, and end-to-end intake/commit path hardening

Rules every contributor (and Claude Code) must follow:

1. Read `instructions.md` and the most recent handoff before changing anything.
2. Implement only the current gate. Never implement future-gate features early.
3. Keep diffs small and reviewable.
4. Update `knownbugs.md` for every new issue, blocker, workaround, or risk.
5. Add tests for everything testable in pure Kotlin.
6. Run the most meaningful checks the environment allows. If a check cannot
   run, document why without treating environment-only limitations as product defects.
7. End every gate with a `handoff/GATE_X_HANDOFF.md` file with status
   `PASS`, `PARTIAL`, or `BLOCKED`.
8. Commit once per gate. Never silently mix gates.
9. If status is `PARTIAL` or `BLOCKED` because of a real project issue, stop.
   If the only unresolved item is local environment verification delegated to CI,
   mark the gate according to the CI-first policy below.

## CI-first gate policy

GitHub Actions is the authoritative build verifier for Painkiller.

Claude Code sessions and other cloud agents may not have the Android SDK,
caches, emulator tooling, or full CI environment. A missing local Android SDK
is **not** a product defect and must not be repeatedly escalated as a gate
blocker when GitHub Actions is configured to verify Android builds.

Default assumption for follow-up prompts:

```text
Everything is OK until the user says otherwise, GitHub Actions fails, or a repository file documents a real blocker.
```

Claude Code should treat the previous gate as passed when:

- the previous gate handoff is `PASS`, or
- the user says the workflow/build passed, or
- the relevant GitHub Actions workflow on `main` is green, or
- the only unresolved issue is a local/environment-only build limitation already delegated to CI.

Claude Code must not reopen old environment-only blockers unless new evidence appears.

If GitHub Actions fails, the failing workflow output becomes the source of
truth and the current gate must not be marked `PASS` until fixed.

Do not create or keep a `BLOCKED`/`PARTIAL` state solely because local
`:app:assembleDebug` cannot run in a session without Android SDK while CI is
available and expected to run it.

## Module layout

- `:domain` — pure Kotlin / JVM. Domain models, the GitHub Git Data API
  interface contract, and pure validators. Reachable in unit tests without
  the Android SDK.
- `:app` — Android module. Activity, theme, Compose components, screens, and
  the Android-side data wiring (SAF, secure token store, network client).
  Depends on `:domain`.

The boundary is intentional: Android-coupled code may depend on domain code,
but domain code must never depend on Android.

## Current baseline

- Latest completed handoff: `handoff/GATE_14_HANDOFF.md` (`PASS`)
- Gate 14 delivered ZIP intake + multi-file commit path wiring in the app flow.
- Next work follows the user-directed expanded plan in `handoff/NEXT_GATES_PLAN.md` (next: Gate 22 PR merge assist).

## Build and check expectations

The default local checks for any gate are:

```bash
./gradlew :domain:test
./gradlew :domain:build
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest   # once unit tests exist on the app side
```

`:app:assembleDebug` requires a working Android SDK. If `ANDROID_HOME` is not
set and `local.properties` does not contain `sdk.dir`, the assembly task
will fail at configuration with a clear "SDK location not found" message.
This is a local environment issue, not a project issue.

When this happens:

- run the verifiable subset, especially `:domain:test` and `:domain:build`
- document the local limitation briefly in the handoff
- rely on GitHub Actions for the Android SDK-backed verification
- do not add a repeated knownbugs entry if this is the same local SDK limitation
- do not block the next gate solely for this local limitation if CI is green or expected to verify it

## Safety rules

- No real upload behavior before Gate 6.
- No credential storage before Gate 3.
- No tokens in logs, ever.
- No `force = true` on ref updates unless an explicit, audited future
  feature requires it.
- No automatic conflict resolution.
- No silent overwrites.
- No destructive cleanup of unfamiliar files. Investigate first.

## Routine behavior

- Do not invent UI details that are not in `instructions.md` or the
  CATALON-GUARD reference grammar.
- Do not redesign the product.
- Do not add dependencies that the current gate does not need.
- Do not skip tests to make a gate "pass".
- Do not edit `instructions.md` unless the user asks for it.
- Do not repeatedly flag local Android SDK absence as a blocker when CI is the intended verifier.
