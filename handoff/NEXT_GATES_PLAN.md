# Next 10 Gates Plan (User-Directed Scope Expansion)

## Historical Plan Snapshot (Gates 17–26)

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

## Gate Ledger Reconciliation (Gates 27–38)

- Gates 27–37: treated as completed implementation sequence per current ledger truth and handoff chain through Gate 36 plus user-confirmed Gate 37 PASS state.
- Gate 38: BLOCKED because no concrete implementation scope existed in repository planning artifacts at execution time.
- Gate 38 recovery: documented as PASS in `handoff/GATE_38_RECOVERY_HANDOFF.md` (ledger/docs recovery only; no runtime feature work).

## Forward Concrete Gate Sequence (Gates 39–48)

## Gate 39 — Forward Plan Rebuild 39–48
- Docs only.
- Write the concrete next 10 gates.
- No runtime implementation.

## Gate 40 — CI / Build Truth Audit
- Verify GitHub Actions, domain tests, Android compile/assemble status.
- Document build truth and blockers.
- No features.

## Gate 41 — Runtime Reality Audit
- Audit what actually works in the Android app: startup, PAT login, repo/branch selection, source intake, preview, normal commit, error displays.
- Produce a truth table.
- No speculative feature work.

## Gate 42 — Workbench Flow Spine
- Recenter the main Workbench chain:
  - Source → Target → Diagnose → Route → Confirm → Execute → Result/Recovery.
- Clean flow wording/state transitions only as needed.
- No new feature lanes.

## Gate 43 — Source Intake Hardening
- Harden SAF/source handling: permission loss, ZIP unsafe paths, ZIP collisions, folder summaries, source summary clarity.

## Gate 44 — Normal Commit E2E Hardening
- Stabilize the core promise: normal GitHub commit for single file, multi-file, folder, and safe ZIP entries.
- Cover SHA mismatch, protected branch, permission errors.

## Gate 45 — Large File Routing Hardening
- Stabilize large-file route truth:
  - normal commit blocked where needed,
  - LFS single-file only,
  - Release Asset single-file only,
  - disabled-but-explained routes for multi/folder/ZIP.

## Gate 46 — Conflict Cleanup Lane Hardening
- Harden conflict preset/card/write-back/commit bridge flow.
- Keep ZIP-entry write-back/commit blocked unless explicitly scoped later.

## Gate 47 — User-Facing Error / Recovery Polish
- Ensure failure states explain:
  - what happened,
  - whether anything was written,
  - whether user data was lost,
  - next safe step.

## Gate 48 — Internal Test Candidate Freeze
- Freeze an internal test candidate with README, knownbugs, handoffs, manual test checklist, and status summary.
- Not a public RC.

## Guardrails
- Implement exactly one gate per task unless the user explicitly requests otherwise.
- Future implementation after this document must proceed one gate at a time.
- Preserve commit safety invariants (`force=false`, SHA-guarded updates).
- Keep opt-in user confirmation for destructive operations (merge, publish, overwrite).
- Do not rewrite Gate 38 historical truth (`BLOCKED`) or Gate 38 recovery truth (`PASS`, docs-only).
