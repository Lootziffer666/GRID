# Gate 5 Handoff

## Status

PARTIAL

Gate 5 domain planning and the preview UI are implemented. All 16 new
unit tests pass alongside the existing 77 domain tests
(`./gradlew :domain:test` — 93 tests, 0 failures).

`./gradlew :app:assembleDebug` was not run in this container (no Android
SDK installed). Per the CI-first policy delegated to the GitHub Actions
build workflow on `main`, this is not treated as a Gate 5 blocker. No new
Android API surfaces were introduced beyond standard Compose Material 3.

This run recovers the Gate 5 work that Codex implemented but failed to push
(likely due to missing GitHub push credentials). Gates 6 and 7 were already
merged to `main` and exist on this branch. Gate 5 was implemented here to
fill the gap and ensure the gate sequence is complete.

## Gate Scope

- Implement Gate 5 only: `UploadPlan` + preview UI.
- Pure-Kotlin domain planning in `:domain/upload/` — no Android, no network.
- Compose preview screen in `:app/ui/screens/` — reads `UploadPlan`, shows
  severity groups, shows suggested commit message, disables Confirm when
  blocked. No upload. No GitHub write.
- Deterministic sample state in `Gate5PreviewSample` for `@Preview` only.
- Commit-message suggestion via `CommitMessageSuggester`.
- Gate 5 / Gate 6 / Gate 7 reconciliation verified: no competing models,
  `UploadPlan.target` → Gate 6/7 `RepoTarget`, `suggestedCommitMessage`
  → editable commit message, `safeEntries + warningEntries` → Gate 7 entry
  list. No changes to Gate 6 or Gate 7 code.

## Implemented

`:domain` (pure Kotlin / JVM):

- `domain/upload/UploadPlanEntry.kt` — one file in the upload plan.
  `repoPath`, `displayName`, `sizeBytes`, `sizeDiagnosis`, `severity`
  (taken directly from `sizeDiagnosis.severity`), `isIgnored`.
- `domain/upload/UploadPlan.kt` — full diagnosis result. Pre-grouped into
  `safeEntries`, `warningEntries`, `blockedEntries`, `deferredEntries`,
  `ignoredEntries`. Computed properties: `isBlockedForCommit` (any blocked
  entries), `willCreateOneCommit` (not blocked and at least one
  safe/warning/deferred entry). Carries `suggestedCommitMessage`.
- `domain/upload/UploadPlanBuilder.kt` — stateless builder. Iterates
  `FilePlan.includedFiles`, maps each to `UploadPlanEntry`, groups by
  `DiagnosticSeverity`. Puts `FilePlan.ignoredFiles` into
  `ignoredEntries`. Calls `CommitMessageSuggester.suggest()` for the
  initial message.
- `domain/upload/CommitMessageSuggester.kt` — pure suggestion logic:
  0 files → "Add .gitkeep"; 1 → "Add <name>"; 2–4 → "Add <a>, <b>, <c>";
  5+ with path → "Add N files to <path>"; 5+ root → "Add N files".
  Only safe + warning entries count as committable; blocked entries
  are excluded from the suggestion.

`:domain` tests (16 new):

- `domain/upload/UploadPlanBuilderTest.kt`
  - `singleSafeFile_plan_hasOneEntryInSafeGroup`
  - `warningFile_plan_hasOneEntryInWarningGroup`
  - `blockedFile_plan_isBlockedForCommit`
  - `ignoredFile_plan_hasOneEntryInIgnoredGroup_notBlocking`
  - `mixedFiles_plan_groupsCorrectly`
  - `noBlockedFiles_willCreateOneCommit_isTrue`
  - `emptyPlan_willCreateOneCommit_isFalse_andNotBlocked`
  - `target_isPreservedInPlan`
  - `commitMessage_noEntries_suggestsGitkeep`
  - `commitMessage_singleFile_addsFileName`
  - `commitMessage_twoFiles_listsBothNames`
  - `commitMessage_fourFiles_listsAllNames`
  - `commitMessage_fiveFiles_usesCountAndTargetPath`
  - `commitMessage_fiveFiles_rootTarget_usesCountOnly`
  - `commitMessage_includesWarningFiles_inCount`
  - `commitMessage_blockedFilesNotCommittable_notCountedInSuggestion`

`:app` (Android):

- `app/ui/screens/UploadPreviewScreen.kt` — Compose screen that renders
  `UploadPlan`. Shows: target card (owner/repo/branch/path), blocked
  warning banner when `isBlockedForCommit`, four severity-group sections
  (safe / warning / blocked / deferred), ignored-file count, commit
  message card, Confirm button. Button is disabled when
  `isBlockedForCommit` or `onConfirm == null`. No GitHub write anywhere.
- `app/ui/preview/Gate5PreviewSample.kt` — deterministic hardcoded
  `UploadPlan` for `@Preview` functions only. Covers all four severity
  groups plus an ignored entry. Never used in production code.

## Files Changed

```
domain/src/main/kotlin/com/painkiller/domain/upload/UploadPlanEntry.kt
domain/src/main/kotlin/com/painkiller/domain/upload/UploadPlan.kt
domain/src/main/kotlin/com/painkiller/domain/upload/UploadPlanBuilder.kt
domain/src/main/kotlin/com/painkiller/domain/upload/CommitMessageSuggester.kt
domain/src/test/kotlin/com/painkiller/domain/upload/UploadPlanBuilderTest.kt
app/src/main/java/com/painkiller/ui/screens/UploadPreviewScreen.kt
app/src/main/java/com/painkiller/ui/preview/Gate5PreviewSample.kt
README.md
knownbugs.md
handoff/GATE_5_HANDOFF.md
```

## Checks Run

- command: `./gradlew :domain:test`
  - result: PASS — `BUILD SUCCESSFUL`. 93 tests across 11 test classes,
    0 failures, 0 ignored. The 16 new tests in `UploadPlanBuilderTest`
    are green alongside the 77 pre-existing tests.
- command: `./gradlew :app:assembleDebug`
  - result: not run in this container — no Android SDK present. Per
    the CI-first policy delegated to GitHub Actions on `main`.

## CI Status

- workflow: `.github/workflows/build.yml`
- result: not yet executed for this branch in this run. Will be
  validated by GitHub Actions on push.

## Gate 5 / Gate 6 / Gate 7 Reconciliation

- No competing `UploadPlan`-like model exists.
- `UploadPlan.target` (`RepoTarget`) maps directly to the `target`
  parameter in `SingleFileCommitInput` (Gate 6) and
  `MultiFileCommitInput` (Gate 7).
- `UploadPlan.safeEntries + warningEntries` maps to
  `List<MultiFileCommitEntry>` in Gate 7 (caller reads content from SAF
  and encodes it).
- `UploadPlan.suggestedCommitMessage` is the editable starting value for
  `commitMessage` in Gates 6 / 7.
- `UploadPreviewScreen.onConfirm` is `null` until Gate 6 / 7 is wired —
  the button is explicitly disabled.
- No GitHub write APIs are reachable from any Gate 5 code path.

## Known Bugs / Risks

- `BUG-20260426-007` (OPEN) — pre-existing local-SDK environment limit;
  not reopened.
- `BUG-20260426-008` — updated: Gate 5 was missing because Codex could not
  push (no GitHub credentials). Now implemented and gate sequence is
  complete through Gate 7.

## Explicitly Not Done

- No SAF wiring — `UploadPreviewScreen.onConfirm` is `null` (deferred).
- No editable commit message UI state — the commit screen (Gate 8+) will
  add a `TextField`. Gate 5 shows the suggestion read-only.
- No remote-existence check — all planned files are treated as
  "planned write"; detecting new vs. updated requires a remote tree lookup
  which is not in Gate 5 scope.
- No actual `createBlob`, `createTree`, `createCommit`, or `updateRef`
  calls — the orchestrators from Gates 6 / 7 handle those.
- No retry, backoff, or error UX (Gate 8).

## Next Gate May Start Only If

- This handoff is committed and pushed.
- The GitHub Actions build workflow on this branch is green (or user
  confirms).
- Gate 8 (robustness) may now start since Gates 5, 6, and 7 are all
  present.
