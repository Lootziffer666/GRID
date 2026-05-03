# Next 10 Gates Plan (User-Directed Scope Expansion)

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

## Gate 20 — OAuth as Additional Login
- Keep PAT login.
- Add OAuth as a second login path (parallel option in auth UI).
- Token exchange/storage must preserve existing token-safety rules.

## Gate 21 — PR Management Foundations
- Add repository PR listing and selection primitives.
- Add PR creation draft workflow scaffolding from upload context.
- No auto-merge yet.

## Gate 22 — PR Merge Assist
- Add mergeability diagnostics (status checks, conflicts, protection hints).
- Add guided merge actions where repo permissions/rules allow.
- Keep explicit user confirmation before merge.
- Optional: add ONNX-based local scoring/ranking for merge-risk hints (device-side inference only, no auto-merge).

## Gate 23 — Git LFS Expansion
- Add Large File Doctor upgrade path that can route eligible files to LFS.
- Add LFS upload planning and constraints checks.
- Keep normal Git Data API path unchanged for non-LFS files.
- Note: model artifacts around 23 MB may be committed directly (below current 25 MB warning threshold), LFS remains optional for larger future models.

## Gate 24 — GitHub Release Assets
- Add release selection/creation workflow for oversized or binary artifacts.
- Add release asset upload orchestration and error mapping.

## Gate 25 — Full PR Management
- Add PR detail view, review state summaries, and update/retry actions.
- Add safe merge strategy selection helper (merge/squash/rebase where allowed).

## Gate 26 — Release Readiness + Scope Reconciliation
- Reconcile docs/handoffs/knownbugs with expanded scope.
- CI-first verification checklist across upload, OAuth, PR, LFS, and Releases.
- Final cleanup of temporary compatibility shims.

## Guardrails
- One gate-focused commit at a time unless user explicitly requests batching.
- Preserve commit safety invariants (`force=false`, SHA-guarded updates).
- Keep opt-in user confirmation for destructive operations (merge, publish, overwrite).


## Gate Ledger Reconciliation (Gates 27–38)

- Gates 27–37: treated as completed implementation sequence per current ledger truth and handoff chain through Gate 36 plus user-confirmed Gate 37 PASS state.
- Gate 38: BLOCKED because no concrete implementation scope existed in repository planning artifacts at execution time.
- Gate 38 recovery action: reconcile README status/index, NEXT_GATES_PLAN continuity notes, and knownbugs blocker entry before any new feature gate starts.

## Gate 39 Placeholder (requires user-approved scope)

- Gate 39 is intentionally undefined until the user approves concrete scope and acceptance criteria.
- Do not implement runtime feature work for Gate 39 from this file alone.
