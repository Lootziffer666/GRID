# Gate 20 Handoff

## Status

PARTIAL

## Gate Scope

- OAuth as additional login path (PAT remains available).

## Implemented

- `AuthViewModel` now supports OAuth authorization-code input and sign-in action via `signInWithAuthorizationCode()`.
- Added `oauthCodeInput` state and validation (`canSubmitOAuthCode`) in `AuthUiState`.
- `AuthScreen` now includes an optional OAuth code section with dedicated input + action button.
- Existing PAT flow remains unchanged and still available.

## Files Changed

- `app/src/main/java/com/painkiller/ui/auth/AuthViewModel.kt`
- `app/src/main/java/com/painkiller/ui/auth/AuthScreen.kt`
- `handoff/GATE_20_HANDOFF.md`
- `README.md`
- `AGENTS.md`
- `claude.md`

## Checks Run

- command: `./gradlew :domain:test`
- result: PASS

- command: `./gradlew :app:assembleDebug`
- result: local SDK missing (`SDK location not found`), CI remains authoritative.

## CI Status

- workflow: `.github/workflows/build.yml`
- result: pending push

## Known Bugs / Risks

- OAuth exchange still depends on configured `GithubOAuthApi`; default container wiring keeps it `null` unless a dedicated exchange path is provided.
- OAuth code path returns a clear failure message when unavailable in this build.

## Explicitly Not Done

- No OAuth browser/device-code flow automation.
- No backend `client_secret` exchange service implementation.
- No PR/LFS/Release features in this gate.

## Next Gate May Start Only If

- CI validates Android build green, or user explicitly accepts CI-first progression.
