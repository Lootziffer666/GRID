# Gate 24 Handoff

## Status

PASS

## Gate Scope

- Implement Gate 24 only: GitHub Release Assets workflow.
- Add release selection/creation support and release asset upload orchestration with explicit error mapping.
- Harden release asset input validation (name/content-type/data) before API call.

## Implemented

- Extended release boundary contract with `uploadReleaseAsset(owner, repo, releaseId, request)`.
- Added release asset domain models: `UploadReleaseAssetRequest` and `GithubReleaseAssetSummary`.
- Added domain `ReleaseAssetValidation` with tests for blank name/content-type and empty payload.
- Implemented uploads host call in `KtorGithubReleaseApi` using `uploads.github.com` with `name` query parameter and binary body upload.
- Extended `GithubReleaseRepository` with:
  - `createRelease(...)`
  - `uploadReleaseAsset(...)`
  - explicit result types for create/upload failures.
  - preflight validation errors for blank content type and empty upload payload.
- Extended `UploadFlowViewModel` with:
  - optional release creation inputs (tag/name)
  - release creation action
  - upload selected single file as release asset action
  - state flags/messages for release creation and asset upload result handling.
- Extended `UploadFlowScreen` with:
  - release asset success info card
  - release creation section (tag + optional name + create button)
  - release asset upload button inside selected release section.
- Added focused Ktor unit tests for release asset upload endpoint behavior and auth failure mapping.
- Added a local Node/Express GitHub App exchange server (`POST /github-app/exchange`) that:
  - reads `APP_ID` + private key from env (`APP_PRIVATE_KEY` or `APP_PRIVATE_KEY_PATH`)
  - mints a 10-minute RS256 JWT
  - calls GitHub `POST /app/installations/{installation_id}/access_tokens`
  - returns `{ token, expires_at }`.
- Added Android Retrofit implementation for `GithubAppAuthApi` with `ExchangeRequest`/`ExchangeResponse`.
- Updated `PainkillerContainer` to wire `appAuthApi` **only when**
  `BuildConfig.GITHUB_APP_BROKER_BASE_URL` is configured; default build keeps
  `appAuthApi = null` so normal users are never forced to run a local helper server.
- Added `MockWebServer` test for Retrofit exchange API request/response behavior.

## Files Changed

- `domain/src/main/kotlin/com/painkiller/domain/github/GithubGitDataApi.kt`
- `domain/src/main/kotlin/com/painkiller/domain/github/GitDataModels.kt`
- `domain/src/main/kotlin/com/painkiller/domain/github/ReleaseAssetValidation.kt`
- `domain/src/test/kotlin/com/painkiller/domain/github/ReleaseAssetValidationTest.kt`
- `app/src/main/java/com/painkiller/data/github/KtorGithubReleaseApi.kt`
- `app/src/main/java/com/painkiller/data/github/GithubReleaseRepository.kt`
- `app/src/main/java/com/painkiller/ui/flow/UploadFlowViewModel.kt`
- `app/src/main/java/com/painkiller/ui/flow/UploadFlowScreen.kt`
- `app/src/test/java/com/painkiller/data/github/KtorGithubReleaseApiTest.kt`
- `README.md`
- `handoff/GATE_24_HANDOFF.md`
- `app/src/main/java/com/painkiller/data/github/RetrofitGithubAppAuthApi.kt`
- `app/src/test/java/com/painkiller/data/github/RetrofitGithubAppAuthApiTest.kt`
- `tools/github-app-exchange-server/server.js`
- `tools/github-app-exchange-server/package.json`
- `tools/github-app-exchange-server/start-local.sh`
- `tools/github-app-exchange-server/README.md`
- `.gitignore`
- `app/build.gradle.kts`

## Checks Run

- command: `./gradlew :domain:test :domain:build`
- result: PASS

- command: `./gradlew :domain:test --tests "com.painkiller.domain.github.ReleaseAssetValidationTest"`
- result: PASS

- command: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
- result: local SDK missing (`SDK location not found`), CI remains authoritative per CI-first policy.

- command: `cd tools/github-app-exchange-server && npm install`
- result: PASS

- command: `cd tools/github-app-exchange-server && APP_ID=123 APP_PRIVATE_KEY_PATH=./missing.pem node server.js`
- result: PASS (server starts; exchange returns expected config error until valid credentials are provided)

## CI Status

- workflow: `.github/workflows/build.yml`
- result: pending push

## Known Bugs / Risks

- Release asset upload currently targets the selected single-file source only; multi-file/folder/ZIP release-asset batch upload is not yet wired.
- Upload uses in-memory decoded bytes from base64; very large artifacts may increase app memory pressure and should be optimized in a later gate.
- Local Node exchange server is dev/test bridge only; production path requires external broker URL configuration.

## Explicitly Not Done

- No multi-file release asset batch uploader.
- No release asset progress UI.
- No delete/replace asset flow.

## Next Gate May Start Only If

- CI validates Android build green, or user explicitly accepts CI-first progression.
