# Gate 24.5 Handoff

## Status

PASS

## Gate Scope

- Runtime & scope reconciliation only (no new features, no Gate 25 work).
- Align UI labels/docs/knownbugs with actual runtime behavior.

## Implemented

- Auth truth-labeling:
  - OAuth path labeled **experimental** and disabled when exchange backend is unavailable.
  - GitHub App installation path labeled **dev-only broker** and disabled when broker is not configured.
- Added explicit runtime availability flags in auth state (`isOAuthAvailable`, `isGithubAppAvailable`) sourced from `GithubAuthRepository` capability checks.
- Release asset truth-labeling in upload UI:
  - explicit copy: single-file source only
  - explicit copy: memory-heavy upload limitation.
- Reconciled docs:
  - added runtime feature status matrix in README (`Stable`, `Experimental`, `Dev-only`, `Deferred`, `Hidden`).
  - logged reconciliation in `knownbugs.md` (BUG-20260427-020).

## Files Changed

- `app/src/main/java/com/painkiller/data/github/GithubAuthRepository.kt`
- `app/src/main/java/com/painkiller/ui/auth/AuthViewModel.kt`
- `app/src/main/java/com/painkiller/ui/auth/AuthScreen.kt`
- `app/src/main/java/com/painkiller/ui/flow/UploadFlowScreen.kt`
- `README.md`
- `knownbugs.md`
- `handoff/GATE_24_5_HANDOFF.md`

## Checks Run

- command: `./gradlew :domain:test`
- result: PASS

- command: `./gradlew :app:assembleDebug`
- result: local SDK missing (`SDK location not found`), CI remains authoritative.

## CI Status

- workflow: `.github/workflows/build.yml`
- result: pending push

## Known Bugs / Risks

- Release asset upload remains single-file and memory-heavy (explicitly labeled in UI/docs).
- OAuth and GitHub App paths still require configured backend/broker to become active.

## Explicitly Not Done

- No behavior expansion (no new auth provider flow, no Gate 25 work).
- No UploadFlowViewModel refactor.
- No PR management scope expansion.

## Next Gate May Start Only If

- CI validates Android build green, or user explicitly accepts CI-first progression.
