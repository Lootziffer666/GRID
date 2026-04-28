# Gate 24.6 Handoff

## Status

PASS

## Gate Scope

- Remove GitHub App broker from the product/runtime auth path.
- Keep PAT as the active auth path.
- Document OAuth Device Flow / OAuth App as future candidate only (not implemented).

## Implemented

- Removed production broker wiring:
  - deleted `BuildConfig.GITHUB_APP_BROKER_BASE_URL` from app module config.
  - removed `RetrofitGithubAppAuthApi` and `GithubAppAuthApi` from app code.
  - removed `appAuthApi` injection path from `PainkillerContainer` and simplified `GithubAuthRepository` constructor.
- Removed GitHub App broker UI path:
  - deleted installation-id state/actions from `AuthViewModel`.
  - removed GitHub App installation section from `AuthScreen`.
- Removed local Node broker spike folder `tools/github-app-exchange-server` from repository.
- Updated docs:
  - README now states PAT is current auth path and OAuth Device Flow / OAuth App is a future candidate.
  - knownbugs updates: BUG-20260427-019 set to FIXED with decision; BUG-20260427-017 marked FIXED due broker path removal.

## Files Changed

- `app/build.gradle.kts`
- `app/src/main/java/com/painkiller/data/github/GithubAuthRepository.kt`
- `app/src/main/java/com/painkiller/di/PainkillerContainer.kt`
- `app/src/main/java/com/painkiller/ui/auth/AuthViewModel.kt`
- `app/src/main/java/com/painkiller/ui/auth/AuthScreen.kt`
- `app/src/main/java/com/painkiller/data/github/GithubAppAuthApi.kt` (deleted)
- `app/src/main/java/com/painkiller/data/github/RetrofitGithubAppAuthApi.kt` (deleted)
- `app/src/test/java/com/painkiller/data/github/RetrofitGithubAppAuthApiTest.kt` (deleted)
- `tools/github-app-exchange-server/` (deleted)
- `README.md`
- `knownbugs.md`
- `handoff/GATE_24_6_HANDOFF.md`

## Checks Run

- command: `./gradlew :domain:test`
- result: PASS

- command: `./gradlew :domain:build`
- result: PASS

- command: `./gradlew :app:assembleDebug`
- result: local SDK missing (`SDK location not found`); CI remains authoritative per CI-first policy.

## CI Status

- workflow: `.github/workflows/build.yml`
- result: pending push

## Known Bugs / Risks

- Release asset upload remains single-file and memory-heavy (tracked in BUG-20260427-018).
- OAuth Device Flow / OAuth App remains a documented candidate only; not implemented.

## Explicitly Not Done

- No OAuth implementation.
- No changes to upload, PR, release, ZIP, or merge behavior beyond compile-safe auth wiring cleanup.
- No Gate 25 implementation.

## Next Gate May Start Only If

- CI verifies Android build green, or user explicitly allows CI-first progression.
