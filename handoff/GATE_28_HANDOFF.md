# Gate 28 Handoff

## Status

PASS

## Gate Scope

- Implement Gate 28 only: Large File Routing UI.
- Make route choice explicit and meaning-first for normal commit vs Git LFS vs Release Asset vs blocked/unsupported.
- Keep existing transport safety invariants unchanged.

## Implemented

- Added a small routing model in `:domain/upload`:
  - `LargeFileRoute`
  - `LargeFileRouteAvailability`
  - `LargeFileRouteOption`
  - `LargeFileRoutingInput`
  - `LargeFileRoutingDecision`
  - `LargeFileRoutingDecider`
- Added UI-state integration in `UploadFlowUiState.routingDecision` so route availability is derived from current source type, plan block state, ZIP safety, and release selection state.
- Added a new `Large-file routing` panel in `UploadFlowScreen` that shows:
  - recommendation summary
  - route cards with meaning-first copy
  - GitHub effect and repo-change truth
  - disabled reason for unsupported/unavailable paths
  - safety notes
- Moved route actions behind the decision panel:
  - Git LFS action now appears only on executable LFS route cards
  - Release Asset action now appears only on executable Release route cards
- Preserved existing safety behavior:
  - normal >100 MiB path remains blocked
  - ZIP unsafe path block remains hard gate
  - LFS upload still required before pointer commit
  - release upload remains explicit and non-automatic

## Large-file Routing Audit Result

- Large File Doctor output: **Correct but too technical** (mentions Git terms; now translated in routing UI context).
- UploadPlan severity groups: **Correct but needed better grouping** (severity existed, route-level meaning was missing).
- Blocked >100 MiB behavior: **Correct but missing UX decision layer** (blocked path existed, now explicitly compared to alternatives).
- LFS action button: **UI-visible but previously context-coupled in source section** (now route-card based and availability-driven).
- Release Asset section/action: **Correct but needed routing clarity** (now route-card based with explicit availability + release-selection requirement).
- Normal commit confirm behavior: **Clear and correct** (still disabled when plan blocked).
- Single-file vs multi-file/folder/ZIP behavior: **Partially clear, partially missing** (unsupported source-type routes now explicitly shown as unavailable).
- README/knownbugs claims: **Updated to align with Gate 28 routing behavior**.

## Supported vs Unavailable Routes (Gate 28)

- Supported executable routes:
  - Normal repo commit (when plan is not blocked)
  - Git LFS (single selected large file only)
  - Release Asset (single selected file + selected release only)
- Shown but unavailable routes:
  - Git LFS for multi-file/folder/ZIP
  - Release Asset for multi-file/folder/ZIP
  - Normal repo commit when blocked by >100 MiB or unsafe ZIP

## Source Type Mapping

- Single small file:
  - normal commit recommended
  - LFS shown but unavailable (not required)
  - Release Asset optional and disabled unless release selected
- Single file >100 MiB:
  - normal commit blocked
  - Git LFS executable + recommended
  - Release Asset executable only after release selected
- Multiple files/folder with large entries:
  - normal commit blocked
  - Git LFS unavailable for source type
  - Release Asset unavailable for source type (no batch upload)
- ZIP with large entries:
  - normal commit blocked for blocked entries
  - ZIP-to-LFS unavailable
  - ZIP-entry Release routing unavailable
- Unsafe ZIP:
  - blocked; no route bypass available

## Files Changed

- `domain/src/main/kotlin/com/painkiller/domain/upload/LargeFileRouting.kt`
- `domain/src/test/kotlin/com/painkiller/domain/upload/LargeFileRoutingDeciderTest.kt`
- `app/src/main/java/com/painkiller/ui/flow/UploadFlowViewModel.kt`
- `app/src/main/java/com/painkiller/ui/flow/UploadFlowScreen.kt`
- `README.md`
- `knownbugs.md`
- `handoff/GATE_28_HANDOFF.md`

## Checks Run

- command: `./gradlew :domain:test`
- result: PASS

- command: `./gradlew :domain:build`
- result: PASS

- command: `./gradlew --no-daemon :app:testDebugUnitTest`
- result: local SDK missing (`SDK location not found`), CI remains authoritative per CI-first policy.

## CI Status

- workflow: `.github/workflows/build.yml`
- result: pending push

## Known Bugs / Risks

- Multi-file/folder/ZIP Git LFS routing remains intentionally unavailable.
- Release Asset batch upload remains intentionally unavailable.
- OAuth/PR/conflict features were not changed in this gate.

## Explicitly Not Done

- No OAuth implementation.
- No PR management expansion.
- No conflict cards/presets.
- No multi-file/folder/ZIP LFS transport.
- No release asset batch upload, replacement, or delete flow.
- No Gate 29+ features.

## Next Gate May Start Only If

- CI validates Android build green, or user explicitly accepts CI-first progression.
