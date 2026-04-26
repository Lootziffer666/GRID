# knownbugs.md — PAINKILLER

Structured log of bugs, blockers, failed assumptions, workarounds, and risks.

## Format

Append new entries at the bottom. Never delete old entries — mark them
`FIXED` or `ACCEPTED` instead.

```
## BUG-YYYYMMDD-NNN

Status: OPEN | FIXED | ACCEPTED | BLOCKED
Gate: X
Severity: LOW | MEDIUM | HIGH
Summary: one-line description.

Evidence:
- exact symptom, command output, log excerpt, file:line, etc.

Action:
- what we did, did not do, or recommend doing next.
```

Severity guidance:

- LOW — cosmetic, theoretical, or environmental noise.
- MEDIUM — affects gate quality but does not block correctness.
- HIGH — blocks correctness, safety, or a gate's acceptance criteria.

Status guidance:

- OPEN — needs work.
- FIXED — resolved; keep entry for history.
- ACCEPTED — left as-is on purpose; explain why under Action.
- BLOCKED — cannot progress without external input or new info.

---

## BUG-20260426-001

Status: ACCEPTED
Gate: 0
Severity: MEDIUM
Summary: Android SDK is not installed in the build environment, so
`./gradlew assembleDebug` cannot be executed during Gate 0.

Evidence:
- `ANDROID_HOME` and `ANDROID_SDK_ROOT` are unset.
- `/opt` contains JDK 21 and Gradle 8.14.3 but no Android SDK.
- `dl.google.com/android/repository/` reachable but SDK install is out of
  scope for Gate 0 and would expand scope significantly.

Action:
- Treat the Android build as a deferred check. Domain code is verified by
  compiling pure-Kotlin sources directly with `kotlinc`.
- A future gate run on a host with the Android SDK installed must run
  `./gradlew test` and `./gradlew assembleDebug` before claiming the
  Android build is green.
- See `handoff/GATE_0_HANDOFF.md` for the exact checks that did and did
  not run.
