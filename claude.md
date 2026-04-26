# claude.md — PAINKILLER

Project-specific working instructions for Claude Code working on PAINKILLER.

## What PAINKILLER is

A focused Android tool that solves one mobile pain:

> "I have files / folders / ZIPs on my Android phone. I want them safely
> committed and pushed into a GitHub repository."

That is the entire product. Anything beyond this is out of scope for v0.

## What PAINKILLER is not

- Not a GitHub Desktop clone.
- Not a Git client.
- Not an IDE.
- Not a file manager.
- Not a dashboard.
- Not a tool suite.
- Not a PR / branch graph / blame / history browser.
- Not Conflict Cards (deferred).
- Not real Git LFS upload (v0 only diagnoses).
- Not Release Asset upload (v0 only diagnoses).

## Fixed architecture (do not re-open without a hard blocker)

- Android, Kotlin, Jetpack Compose
- Pragmatic MVVM/MVI (ViewModel + StateFlow)
- Coroutines
- Retrofit/OkHttp or Ktor (direct GitHub API calls, no JGit, no libgit2)
- kotlinx.serialization or Moshi
- DataStore for non-secret settings/presets
- `SecureTokenStore` abstraction backed by Keystore / AndroidX Security
- Storage Access Framework for file/folder selection
- `java.util.zip` for ZIP handling
- GitHub Git Data API for the multi-file single-commit upload flow

## Gate discipline

1. Every change belongs to exactly one gate.
2. Do the smallest useful implementation for that gate.
3. Run the most meaningful checks available.
4. Fix only what blocks the current gate.
5. Update `knownbugs.md`.
6. Write `handoff/GATE_X_HANDOFF.md`.
7. Commit before starting the next gate.
8. Do not advance unless the previous gate is `PASS`.

Gate status legend: `PASS` / `PARTIAL` / `BLOCKED`.

## Scope boundaries

- No unrelated refactors.
- No broad cleanup.
- No invented features.
- No design reinterpretation. Use the tokens defined in `instructions.md`.
- No dependency churn unless required by the current gate.
- No silent test skipping.
- No claim of success without evidence.
- No real upload behavior before the gate explicitly allows it.
- No hardcoded secrets. No hidden credential storage.
- No destructive git operations (force push, reset --hard, branch deletion).
- No skipping hooks (`--no-verify`).

## Architecture summary

Layered, mobile-first, preview-first, explicit-confirmation-first.

```
ui/                 Compose surfaces. No domain-Compose coupling outside ui/.
ui.theme            Painkiller theme: colors, shapes, type.
ui.components       Reusable Painkiller* components.
domain/             Pure Kotlin. Testable without Android.
domain.model        Data classes for Painkiller's domain (FilePlan, ...).
domain.usecase      Pure operations on those models.
data.github         GitHub API DTOs and the GitHubGitDataApi interface.
data.files          SAF abstractions (Gate 1+).
data.zip            ZIP handling (Gate 1+).
data.settings       DataStore-backed presets / last-used target (Gate 4+).
data.security       SecureTokenStore abstraction (Gate 3+).
```

The UI never speaks to the API layer directly. Use cases sit between.

## Required commands and checks

When tooling is available:

- `./gradlew test`
- `./gradlew assembleDebug`

When the Android SDK is unavailable in the environment:

- Verify pure-Kotlin domain sources compile (e.g. via `kotlinc`).
- Document the unavailable check honestly in the gate handoff.

Never claim a check passed unless it actually ran.

## Routine behavior rules

- Read `instructions.md`, `claude.md`, `knownbugs.md`, recent git history,
  and the current tree before making changes.
- Prefer editing existing files over creating new ones.
- Keep diffs small and reviewable.
- One coherent commit per gate.
- Working tree must be clean before moving to the next gate.
- If anything is ambiguous, stop with a `BLOCKED` handoff. Do not guess.

## Files that must always exist after Gate 0

- `claude.md`
- `knownbugs.md`
- `README.md`
- `handoff/GATE_0_HANDOFF.md`
- `templates/gated-android-project/README.md`
