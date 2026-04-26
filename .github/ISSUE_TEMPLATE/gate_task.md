---
name: Gate task
description: Plan or run one implementation gate
title: "Gate X: <short scope>"
labels: [gate]
assignees: []
---

# Gate Task

## Gate

- Gate:
- Scope:
- Previous gate status:
- CI status:

## Required Reading

- [ ] `AGENTS.md`
- [ ] `claude.md`
- [ ] `instructions.md`
- [ ] `README.md`
- [ ] `knownbugs.md`
- [ ] latest relevant `handoff/GATE_X_HANDOFF.md`

## Allowed Work

- ...

## Hard Non-Goals

- ...

## Checks Required

- [ ] `./gradlew :domain:test`
- [ ] `./gradlew :domain:build`
- [ ] `./gradlew :app:assembleDebug` or CI Android build

## Documentation Required

- [ ] README updated if needed
- [ ] `handoff/GATE_X_HANDOFF.md`
- [ ] `knownbugs.md` updated only for real bugs/risks

## Stop Rule

Stop after this gate. Do not start the next gate.
