# Gate 23 Handoff

## Status

PARTIAL

## Gate Scope

- GitHub App auth groundwork (official app direction) + LFS expansion preparation.

## Implemented

- Added new boundary interface `GithubAppAuthApi` for backend-mediated installation-token exchange.
- Extended `GithubAuthRepository` with `signInWithGithubAppInstallation(installationId)` flow.
- Extended `AuthViewModel`/`AuthUiState` with installation-id input and submit action.
- Extended `AuthScreen` with GitHub App installation sign-in section.
- Updated container wiring to keep `appAuthApi = null` in current build until backend exchange endpoint is configured.

## Files Changed

- `app/src/main/java/com/painkiller/data/github/GithubAppAuthApi.kt`
- `app/src/main/java/com/painkiller/data/github/GithubAuthRepository.kt`
- `app/src/main/java/com/painkiller/di/PainkillerContainer.kt`
- `app/src/main/java/com/painkiller/ui/auth/AuthViewModel.kt`
- `app/src/main/java/com/painkiller/ui/auth/AuthScreen.kt`
- `handoff/GATE_23_HANDOFF.md`
- `README.md`
- `AGENTS.md`
- `claude.md`
- `knownbugs.md`

## Checks Run

- command: `./gradlew :domain:test`
- result: PASS

- command: `./gradlew :app:assembleDebug`
- result: local SDK missing (`SDK location not found`), CI remains authoritative.

## CI Status

- workflow: `.github/workflows/build.yml`
- result: pending push

## Known Bugs / Risks

- GitHub App sign-in path requires backend exchange endpoint; without it, flow fails with explicit message.
- App private key management is intentionally out-of-app and backend-only.

## Explicitly Not Done

- No backend token exchange service implementation in this repository.
- No automatic installation discovery flow.

## Next Gate May Start Only If

- CI validates Android build green, or user explicitly accepts CI-first progression.
