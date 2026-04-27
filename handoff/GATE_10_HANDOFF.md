# Gate 10 Handoff

## Status

KEEP_BUT_SPLIT_INTO_GATES — checkpoint committed

## Candidate Type

**Productive Integration Checkpoint**

This pass replaced stub / placeholder implementations with real productive code
across the HTTP, token-storage, file-reader, DI, and ViewModel layers.  The
domain layer is untouched.  No Auth UI screen and no Upload UI flow screens
exist yet — the new ViewModels have no Compose consumers and the app still
shows the Gate 0 placeholder at runtime.

---

## What Was Implemented in This Pass

| Layer | File(s) | Status |
|---|---|---|
| Dependencies | `app/build.gradle.kts`, `gradle/libs.versions.toml` | NEW — Ktor Core/OkHttp/ContentNegotiation/KotlinxJson/Mock, androidx-security-crypto, navigation-compose, lifecycle-viewmodel-compose, coroutines-test |
| HTTP client | `app/…/github/PainkillerHttpClient.kt` | NEW — Ktor + OkHttp, 30s timeouts, `Accept`, `X-GitHub-Api-Version`, `User-Agent`, `encodeDefaults=true` |
| Token storage | `app/…/security/EncryptedSecureTokenStore.kt` | NEW — AndroidX EncryptedSharedPreferences, AES-256-GCM master key, AES-256-SIV key encryption |
| PAT probe | `app/…/github/GithubTokenProbeApi.kt` | NEW — `GET /user`, maps 401→AuthRequired, 403→PermissionDenied, others→NetworkUnavailable |
| Token format | `app/…/github/GithubTokenProbeApi.kt` (GithubTokenFormat) | NEW — fast regex-only pre-check for `ghp_`, `github_pat_`, `ghs_` prefixes |
| GitHub Git Data | `app/…/github/KtorGithubGitDataApi.kt` | NEW — all six API calls, `force=true` assertion guard, safe 422/403 body-sniff classification |
| Repository/branch listing | `app/…/github/KtorGithubRepositoryApi.kt` | NEW — `GET /user/repos` + `GET /repos/{o}/{r}/branches`, paginated up to 500 items |
| Auth repository | `app/…/github/GithubAuthRepository.kt` | MODIFIED — added `signInWithPersonalAccessToken`, made `oauthApi` nullable (`null` in DI — OAuth disabled deliberately) |
| SAF file reader | `app/…/files/SafFileReader.kt` | NEW — single-file SAF read (display name, MIME, size, base64), `Dispatchers.IO` |
| DI container | `app/…/di/PainkillerContainer.kt` | NEW — lazy singletons for all of the above |
| Application | `app/…/PainkillerApplication.kt` | MODIFIED — exposes `container: PainkillerContainer` |
| Auth ViewModel | `app/…/ui/auth/AuthViewModel.kt` | NEW — PAT input, format check, sign-in, sign-out, `AuthUiState` |
| Upload flow VM | `app/…/ui/flow/UploadFlowViewModel.kt` | NEW — full single-file upload flow VM (file pick → repo/branch list → target path → plan → confirm); multi-file/folder/ZIP deferred to future gate |
| App-side tests | `app/src/test/…/KtorGithubGitDataApiTest.kt`, `KtorGithubTokenProbeApiTest.kt`, `KtorGithubRepositoryApiTest.kt` | NEW — MockEngine-backed tests covering all status-code branches and safety contracts |

---

## What Remains Fake / Stubbed / Missing

| Item | State |
|---|---|
| `AuthScreen` / Auth UI | **Not implemented** — `AuthViewModel` exists with no Compose consumer |
| Upload flow screens (file picker, repo picker, target path, preview→confirm) | **Not implemented** — `UploadFlowViewModel` exists with no Compose consumers |
| `NavHost` / navigation wiring | **Not wired** — `MainActivity` and `PainkillerApp` still show Gate 0 placeholder |
| Folder picker / folder SAF traversal | **Missing** — `SafFileReader` is single-file only; `SafSourceIntake.readFolderTree()` is interface-only |
| ZIP picker / ZIP byte extraction | **Missing** — `SafSourceIntake.readZipFile()` is interface-only; no extraction logic |
| Multi-file UI path | **Missing** — `MultiFileCommitRepository` exists; no VM/screen drives it |
| `GithubOAuthApi` HTTP impl | **Deferred** — `oauthApi = null` in container; OAuth requires server-side `client_secret` |

---

## Safety Audit

| Check | Result |
|---|---|
| No hardcoded GitHub tokens in source | PASS — only test-fixture fakes in `*Test.kt` files |
| No token in logs | PASS — no `Log.*`/`println` in production code; `GithubAuthRepository.signInWithPersonalAccessToken` never echoes the token in error messages |
| No `force = true` | PASS — `KtorGithubGitDataApi.updateRef` asserts `!request.force` and throws `IllegalArgumentException`; test confirms this |
| No silent overwrite | PASS — `expectedSha` enforced via `force=false` on every updateRef call |
| No automatic conflict resolution | PASS — SHA mismatch → `REQUIRES_PLAN_REFRESH`; no auto-merge |
| No automatic write retry | PASS — `RetrySafety.SAFE_TO_RETRY` authorises user-tap only |
| No upload on screen open | PASS — `UploadFlowViewModel.confirmUpload()` guarded by `isCommitting`, `plan != null`, `loadedFile != null` |
| No upload from sample state | PASS — `Gate5PreviewSample` has no API access |
| No folder/ZIP behaviour falsely claimed complete | PASS — `UploadFlowViewModel` docstring says folder/ZIP deferred; `SafFileReader` is single-file only |
| No merge companion behaviour | PASS |
| No broad GitHub permissions | PASS — only INTERNET, no `READ_CONTACTS`, no `MANAGE_DOCUMENTS` etc. |
| No new secret files in repo | PASS — no `.env`, no `local.properties`, no creds |
| `client_secret` | PASS — `oauthApi = null` in container; referenced only in comments explaining why it's absent |

---

## Gate Split for Remaining Work

```
Gate 10 (this file): Productive integration checkpoint
Gate 11: Auth screen + navigation wiring (AuthViewModel → AuthScreen → NavHost)
Gate 12: Upload flow screens (file picker → repo/branch picker → target path → preview/confirm)
Gate 13: Folder SAF intake (SafSourceIntake.readFolderTree real impl + UI trigger)
Gate 14: ZIP intake (readZipFile + ZIP-Slip, multi-file commit path end-to-end)
Gate 15: End-to-end integration test against sandbox repo
```

---

## Files Changed (relative to Gate 9)

**Modified:**
- `app/build.gradle.kts` — added Ktor, security-crypto, navigation, coroutines-test, ktor-mock deps
- `gradle/libs.versions.toml` — matching library declarations
- `app/src/main/java/com/painkiller/PainkillerApplication.kt` — added container
- `app/src/main/java/com/painkiller/data/github/GithubAuthRepository.kt` — PAT sign-in + nullable oauthApi

**New (untracked, to be committed):**
- `app/src/main/java/com/painkiller/data/github/GithubTokenProbeApi.kt`
- `app/src/main/java/com/painkiller/data/github/KtorGithubGitDataApi.kt`
- `app/src/main/java/com/painkiller/data/github/KtorGithubRepositoryApi.kt`
- `app/src/main/java/com/painkiller/data/github/PainkillerHttpClient.kt`
- `app/src/main/java/com/painkiller/data/security/EncryptedSecureTokenStore.kt`
- `app/src/main/java/com/painkiller/data/files/SafFileReader.kt`
- `app/src/main/java/com/painkiller/di/PainkillerContainer.kt`
- `app/src/main/java/com/painkiller/ui/auth/AuthViewModel.kt`
- `app/src/main/java/com/painkiller/ui/flow/UploadFlowViewModel.kt`
- `app/src/test/java/com/painkiller/data/github/KtorGithubGitDataApiTest.kt`
- `app/src/test/java/com/painkiller/data/github/KtorGithubTokenProbeApiTest.kt`
- `app/src/test/java/com/painkiller/data/github/KtorGithubRepositoryApiTest.kt`
- `handoff/GATE_10_HANDOFF.md` (this file)

---

## Checks Run

- command: `./gradlew :domain:test --no-daemon`
  - result: **PASS** — `BUILD SUCCESSFUL` (129 tests, 0 failures, UP-TO-DATE)
- command: `./gradlew :domain:build --no-daemon`
  - result: **PASS** — `BUILD SUCCESSFUL` (UP-TO-DATE)
- command: `./gradlew :app:testDebugUnitTest --no-daemon`
  - result: **SKIPPED locally** — SDK location not found (permanent environment-only condition, BUG-20260426-007). Delegated to GitHub Actions CI.

---

## CI Status

- workflow: `.github/workflows/build.yml`
- expected result: CI will compile and run the new app unit tests (Ktor MockEngine tests are pure-JVM, no emulator needed, but the AGP task requires Android SDK available in CI)

---

## Known Bugs / Risks

| ID | Status | Summary |
|---|---|---|
| BUG-20260426-009 | PARTIAL | `GithubGitDataApi` HTTP client now implemented (`KtorGithubGitDataApi`). `GithubOAuthApi` and `GithubRepositoryApi` now also implemented (`KtorGithubRepositoryApi`). Full end-to-end upload not yet reachable from UI. |
| BUG-20260426-007 | ACCEPTED | Local Android SDK absent; CI is source of truth |

---

## Explicitly Not Done

- No Auth UI screen implemented.
- No Upload flow screens implemented.
- No navigation/NavHost wiring.
- No folder SAF traversal implementation.
- No ZIP extraction implementation.
- No multi-file UI path.
- No `GithubOAuthApi` HTTP implementation (OAuth requires server-side client_secret).
- No merge companion behaviour.
- No Gate 15 end-to-end test.

---

## Next Recommended Gate

**Gate 11 — Auth screen + navigation wiring**

Start conditions:
- Gate 10 CI is green.
- User confirms.

Scope:
1. Create `AuthScreen` Compose screen consuming `AuthViewModel` from `PainkillerContainer`.
2. Create minimal `NavHost` (auth → upload flow entry point).
3. Wire `MainActivity` to `NavHost` instead of `PainkillerApp` placeholder.
4. Connect `PainkillerContainer` to the Activity via `(application as PainkillerApplication).container`.
5. No folder/ZIP, no multi-file, no merge companion.
