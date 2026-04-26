---
name: Known bug / risk
description: Track a bug, risk, blocker, or accepted limitation
title: "BUG-YYYYMMDD-NNN: <summary>"
labels: [bug, knownbugs]
assignees: []
---

# Known Bug / Risk

## Entry

```md
## BUG-YYYYMMDD-NNN

Status: OPEN | FIXED | ACCEPTED | BLOCKED
Gate: X
Severity: LOW | MEDIUM | HIGH
Summary: <one line>

Evidence:
- <log line, command output, file path:line, etc.>

Action:
- <fix, workaround, follow-up needed>
```

## Notes

- Do not delete old `knownbugs.md` entries.
- If fixed, mark `FIXED` and keep the entry.
- If intentionally deferred, mark `ACCEPTED` and explain why.
- Do not create repeated entries for local Android SDK absence when CI is the Android verifier.
