# Gate 21 Handoff

## Status

PARTIAL

## Gate Scope

- PR management foundations (list/select open pull requests).

## Implemented

- Added PR domain models and `GithubPullRequestApi` contract.
- Added Ktor implementation (`KtorGithubPullRequestApi`) for listing open pull requests.
- Added `GithubPullRequestRepository` and UI-facing result type (`GithubPullRequestListResult`).
- Wired PR repository into `PainkillerContainer`.
- Extended `UploadFlowViewModel` with PR list loading + selection flow.
- Extended `UploadFlowScreen` with “Pick open PR” action and picker dialog.
- Selecting a PR now fills the branch input with the PR head ref.

## Files Changed

- `domain/src/main/kotlin/com/painkiller/domain/github/GitDataModels.kt`
- `domain/src/main/kotlin/com/painkiller/domain/github/GithubGitDataApi.kt`
- `app/src/main/java/com/painkiller/data/github/KtorGithubPullRequestApi.kt`
- `app/src/main/java/com/painkiller/data/github/GithubPullRequestRepository.kt`
- `app/src/main/java/com/painkiller/di/PainkillerContainer.kt`
- `app/src/main/java/com/painkiller/ui/flow/UploadFlowViewModel.kt`
- `app/src/main/java/com/painkiller/ui/flow/UploadFlowScreen.kt`
- `app/src/main/java/com/painkiller/ui/navigation/PainkillerNavGraph.kt`
- `handoff/GATE_21_HANDOFF.md`

## Checks Run

- command: `./gradlew :domain:test`
- result: PASS

- command: `./gradlew :app:assembleDebug`
- result: local SDK missing (`SDK location not found`), CI remains authoritative.

## CI Status

- workflow: `.github/workflows/build.yml`
- result: pending push

## Known Bugs / Risks

- PR listing currently focuses on `state=open` only.
- Merge action/assist is not in this gate (foundation only).

## Explicitly Not Done

- No merge execution.
- No PR creation/update workflow yet.

## Next Gate May Start Only If

- CI validates Android build green, or user explicitly accepts CI-first progression.
