# Next 10 Gates Plan (Post Gate 16)

## Gate 17 — Upload Preview Quality
- Add concise severity counters in the upload plan card.
- Improve warning/blocked explanatory copy before confirm.
- Keep orchestration logic unchanged.

## Gate 18 — Auth/Session UX polish
- Improve PAT input hints and token-kind feedback.
- Make sign-in error/empty-token guidance more actionable.
- Keep OAuth auth-code flow deferred.

## Gate 19 — Source Summary UX
- Add richer source summary details (top files / counts / size snapshots).
- Surface ZIP collision warnings to user-facing summary.

## Gate 20 — Repo/Branch Picker UX
- Add quick filters/search in picker dialogs.
- Improve empty/loading state copy.

## Gate 21 — Target Path Assist
- Add lightweight path suggestions and last-used shortcuts.
- Keep TargetPath validation rules unchanged.

## Gate 22 — Commit Message Assist
- Add simple message templates and one-tap regenerate.
- Preserve deterministic suggester output baseline.

## Gate 23 — Error Recovery UX
- Improve retry hints and recovery actions per `RetrySafety`.
- Keep mapper semantics unchanged; UI polish only.

## Gate 24 — Success Screen Polish
- Add clearer commit summary, path grouping, and copy actions.
- No new network or write behavior.

## Gate 25 — Accessibility & Localization pass
- Improve contrast/text semantics and content descriptions.
- Externalize new strings and normalize mixed-language labels.

## Gate 26 — Release Readiness Pass
- Reconcile docs/handoffs/knownbugs.
- CI-first verification checklist and final cleanup of temporary compatibility shims.

## Guardrails
- One gate-focused commit at a time unless user explicitly requests batching.
- No scope expansion into LFS, Releases, PR management, or background sync.
- Preserve commit safety invariants (`force=false`, SHA-guarded updates).
