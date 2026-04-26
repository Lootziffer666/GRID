# Gate PR

## Gate

- Gate: <!-- e.g. Gate 5 -->
- Title: <!-- short gate name -->
- Candidate status: <!-- PASS | PARTIAL | BLOCKED -->

## Scope

This PR implements only:

- ...

## Explicitly Not Done

This PR does **not** implement:

- ...

## Safety / Scope Guardrails

Confirm where applicable:

- [ ] No future-gate work
- [ ] No unrelated refactors
- [ ] No hardcoded secrets or tokens
- [ ] No token logging
- [ ] No silent overwrite
- [ ] No automatic conflict resolution
- [ ] No `force = true`
- [ ] No upload/write behavior unless this gate explicitly allows it
- [ ] No sample/preview state can trigger a real write
- [ ] Local Android SDK absence is treated as environment-only and delegated to CI

## Changes

### Domain

- ...

### App / UI

- ...

### Documentation

- ...

## Checks

- [ ] `./gradlew :domain:test`
- [ ] `./gradlew :domain:build`
- [ ] `./gradlew :app:assembleDebug` or CI Android build

## CI Status

- Workflow:
- Result:

## Handoff

- [ ] `handoff/GATE_X_HANDOFF.md` created or updated
- [ ] README updated if gate status/capability changed
- [ ] `knownbugs.md` updated only for real bugs/risks

## Known Bugs / Risks

- ...

## Merge Rule

Merge only when:

- CI is green
- gate scope is respected
- handoff exists
- no future gate leaked into this PR
