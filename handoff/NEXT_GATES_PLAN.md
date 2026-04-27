# Next Gates Plan (Post Gate 15)

## Gate 16 — Intake Hardening + UX Clarity

Scope:
- Improve source-intake feedback in the Upload flow.
- Detect obvious duplicate file-name collisions earlier for multi-file picks.
- Harden ZIP intake normalization against duplicate normalized paths.
- Keep changes Android/UI-facing; no GitHub API scope expansion.

Exit criteria:
- Multi-file picks with colliding display names show a clear actionable error.
- ZIP intake path normalization is deterministic and does not silently overwrite duplicate paths in maps.
- Domain tests remain green; Android build remains CI-verified.

## Gate 17 — Upload Preview Quality

Scope:
- Improve preview readability (source summary, grouped counts, clearer warning copy).
- Keep existing orchestration and safety invariants untouched.

Exit criteria:
- Preview presents concise counts for safe/warning/blocked/ignored entries.
- Commit message preview and edit behavior remains deterministic.

## Gate 18 — Auth/Session UX polish

Scope:
- Refine PAT sign-in flow messaging and validation states.
- Keep OAuth auth-code flow deferred unless server-side exchange is available.

Exit criteria:
- Token-format feedback is clearer.
- Sign-in/loading/error states remain consistent and human-readable.

## Guardrails

- Keep CI-first policy.
- One gate-focused commit at a time.
- No expansion into LFS, Release Assets, PR tooling, or background sync.
