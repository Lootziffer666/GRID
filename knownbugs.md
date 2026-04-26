# knownbugs.md

Structured log of bugs, blockers, failed assumptions, workarounds, and
unresolved risks. Never delete entries. When something is fixed, mark the
status `FIXED` and keep the entry. When a risk is consciously accepted,
mark it `ACCEPTED` and explain why.

## Entry format

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

---

## BUG-20260426-001

Status: OPEN
Gate: 0
Severity: MEDIUM
Summary: `:app:assembleDebug` cannot run in this environment because no
Android SDK is installed. `:domain:test` runs cleanly and passes.

Evidence:
- `gradlew :app:assembleDebug` reports: "SDK location not found. Define a
  valid SDK location with an ANDROID_HOME environment variable or by
  setting the sdk.dir path in your project's local properties file at
  '/home/user/PAINKILLER/local.properties'."
- `ANDROID_HOME` is unset in the sleep-mode runner.
- Project Gradle / AGP configuration parses cleanly: `:app:help` succeeds,
  AGP 8.7.3 is resolved, and `:app` is included in the build correctly.

Action:
- Validate `:app:assembleDebug` on a workstation or CI runner that has the
  Android SDK installed (`compileSdk = 35`, `minSdk = 26`).
- Once `:app:assembleDebug` is green, this entry can be marked `FIXED`
  and Gate 0 can be promoted from `PARTIAL` to `PASS`.

---

## BUG-20260426-002

Status: ACCEPTED
Gate: 0
Severity: LOW
Summary: `kotlinx.serialization` Json instances must be configured with
`encodeDefaults = true` for outgoing GitHub Git Data API requests. The
default `encoding = "base64"` field on `CreateBlobRequest` is a default
value and will be omitted from output otherwise.

Evidence:
- Initial test
  `domain/src/test/kotlin/com/painkiller/domain/github/GitDataModelsSerializationTest.kt::createBlobRequest_defaultEncoding_isBase64`
  failed with the default Json instance because the `encoding` field was
  omitted.

Action:
- The test now uses `Json { ignoreUnknownKeys = true; encodeDefaults = true }`.
- The Android-side network client (Gate 6) must use the same configuration
  for outgoing requests so GitHub receives explicit `"encoding": "base64"`.
