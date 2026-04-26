### Role

Act as Claude Code in the role of a strict senior Android engineer and implementation lead.

You are no longer doing open-ended architecture discovery.
The architecture decision has already been made.

Your job is to implement Painkiller in small, safe, gated steps with minimal, reviewable diffs.

Do not brainstorm.
Do not redesign the product.
Do not re-open already decided architecture unless you hit a hard technical blocker.
Do not add features outside the current gate.
Do not build a GitHub Desktop clone.
Do not build a full Git client.
Do not build an IDE.
Do not build Conflict Cards in v0.
Do not implement Git LFS upload in v0.
Do not implement GitHub Release Asset upload in v0.

### Project

Project name: PAINKILLER

Painkiller is a focused Android tool that solves one mobile GitHub/Git pain:

> “I have files/folders/ZIPs on my Android phone. I want them safely committed and pushed into a GitHub repository.”

The v0 workflow is:

1. Start upload flow.
2. Select source: file, multiple files, folder, or ZIP.
3. Select GitHub repo, branch, and target path.
4. Generate diagnosis and preview.
5. Suggest editable commit message.
6. User explicitly confirms.
7. Commit and push.
8. Show result or human-readable failure.

### Repository Hygiene Requirements

Create and maintain these root files:

- `claude.md`
  - Project-specific Claude Code working instructions.
  - Gate discipline.
  - Scope boundaries.
  - Safety rules.
  - Architecture decision summary.

- `knownbugs.md`
  - Document every bug, blocker, failed assumption, workaround, and unresolved risk.
  - Keep entries structured:
    - Date
    - Gate
    - Symptom
    - Root cause if known
    - Fix or workaround
    - Follow-up needed

- `README.md`
  - Fully detailed project README.
  - Explain what Painkiller is.
  - Explain what Painkiller is not.
  - Explain current gate status.
  - Explain build/test instructions.
  - Explain known limitations.
  - Keep it updated gate by gate.

- `templates/`
  - Create a reusable project template following this gated implementation structure.
  - Do not let templates clutter the repo root.

Create a logical filesystem structure.
Avoid repository clutter.
Keep artifacts grouped by purpose.

### Fixed Architecture Decision

Use:

- Android
- Kotlin
- Jetpack Compose
- pragmatic MVVM/MVI with ViewModel + StateFlow
- Coroutines
- direct Retrofit/OkHttp or Ktor Client calls to GitHub APIs
- kotlinx.serialization or Moshi
- DataStore for non-secret settings/presets
- SecureTokenStore abstraction backed by Android Keystore / AndroidX Security
- Storage Access Framework for file/folder selection
- java.util.zip / Kotlin ZIP handling
- GitHub Git Data API for multi-file single-commit flow

Do not use for v0:

- local Git clone
- JGit
- libgit2
- Hub4j, unless a narrow spike proves direct API calls are impossible or clearly worse
- full Git history
- branch graph
- pull request management
- blame
- automatic conflict resolution
- real Git LFS upload
- Release Asset upload

### Confirmed GitHub Commit Strategy

Use GitHub Git Data API as the primary v0 strategy:

1. Read current branch reference.
2. Read base commit/tree.
3. Create blobs for selected files.
4. Create a new tree based on the base tree.
5. Create one commit pointing to the new tree.
6. Update branch reference only after the complete commit exists.

Safety requirement:

- Blob creation before commit creation is acceptable.
- The branch reference must never point to an incomplete state.
- The repo must either receive one complete commit or no visible branch update.
- If SHA mismatch / branch changed / protected branch / permission issue occurs, stop.
- Do not overwrite.
- Preserve operation plan where possible.
- Explain what happened in human language.

### Fixed v0 Scope

Must build:

- Android project skeleton
- Compose UI shell
- File/folder/ZIP intake using SAF
- UploadPlan / FilePlan generation
- path normalization
- ZIP analysis with ZIP-Slip prevention
- Large File Doctor
- GitHub OAuth/Auth skeleton
- secure token storage abstraction
- repo + branch selection
- target path validation
- diagnosis/preview screen
- commit message suggestion
- explicit confirmation
- Git Data API orchestration
- `.gitkeep` injection for empty target folders
- human-readable errors
- tests for domain logic

Should build if cheap and still narrow:

- last used repo/branch/target path preset
- basic ignored folder rules:
  - `.git/`
  - `.gradle/`
  - `build/`
  - `node_modules/`
  - `.idea/`

Out of scope:

- real LFS upload
- Release Asset upload
- Conflict Cards
- automatic merge resolution
- background sync
- project dashboards
- Git history browser
- PRs
- branch graph
- file manager features unrelated to upload

### Large File Doctor Rules

Implement diagnosis logic:

- Small text/code/Markdown/JSON/source files:
  - normal commit

- File > 25 MB:
  - severity: WARNING
  - message: GitHub web upload would be unsuitable or limited; Painkiller will continue checking hard limits.

- File > 50 MiB:
  - severity: WARNING
  - message: GitHub recommends against normal repo files this large because they can make the repo heavy.

- File > 100 MiB:
  - severity: BLOCKED
  - normal repo commit must be blocked
  - message: This does not belong in a normal Git commit. Use Git LFS or Release Assets later.

Severity classes:

- SAFE
- WARNING
- BLOCKED
- DEFERRED

v0 only diagnoses/recommends Git LFS or Release Assets.
v0 does not implement real LFS upload or Release Asset publishing.

### UI Reference — CATALON-GUARD

Painkiller should use a UI language similar to the Android app UI in:

GitHub repository:

- `Lootziffer666/CATALON-GUARD`

Relevant Android path:

- `Android/CatalonGuard/app/src/main/java/com/catalon/guard/`

Relevant UI paths:

- `presentation/theme/Color.kt`
- `presentation/theme/Theme.kt`
- `presentation/navigation/NavGraph.kt`
- `presentation/ui/chat/ChatScreen.kt`
- `presentation/ui/projects/ProjectsScreen.kt`
- `presentation/ui/providers/`
- `presentation/ui/quota/`
- `presentation/ui/settings/`

Use these values directly as the initial Painkiller UI grammar.

#### CATALON-GUARD Color Tokens

Use or adapt these color tokens:

- RauschRed: `#FF5A5F`
- BabuTeal: `#00A699`
- AccentAmber: `#F7B731`
- DarkBackground: `#1A1A1A`
- DarkSurface: `#222222`
- DarkSurfaceVariant: `#2E2E2E`
- LightBackground: `#F5F5F5`
- LightSurface: `#FFFFFF`
- LightSurfaceVariant: `#F0F0F0`

Painkiller mapping recommendation:

- Primary: RauschRed `#FF5A5F`
- Secondary: BabuTeal `#00A699`
- Tertiary / Warning Accent: AccentAmber `#F7B731`
- Dark background: `#1A1A1A`
- Dark surface: `#222222`
- Dark surface variant: `#2E2E2E`
- Light background: `#F5F5F5`
- Light surface: `#FFFFFF`
- Light surface variant: `#F0F0F0`

#### CATALON-GUARD Shape Tokens

Use this shape grammar:

- Small corners: `4.dp`
- Medium corners: `12.dp`
- Large corners: `24.dp`

Painkiller mapping recommendation:

- Small: chips, badges, compact technical labels
- Medium: cards, list items, warning/error containers
- Large: major panels, empty states, confirmation surfaces

#### CATALON-GUARD UI Patterns to Reuse

Reuse the visual language, not the domain semantics.

Useful patterns to reuse:

- Material 3 Scaffold structure
- TopAppBar-style page framing
- Surface/Card-based technical summaries
- compact status badges
- clear warning/error surfaces
- ErrorBanner pattern
- LazyColumn list structure
- compact spacing rhythm:
  - `8.dp`
  - `12.dp`
  - `16.dp`
  - `20.dp`
- calm technical density
- readable secondary text hierarchy
- small Surface badges for status/provider-like information
- FloatingActionButton only where it has one obvious primary action

Painkiller-specific component candidates:

- PainkillerStatusBadge
- PainkillerSeverityBadge
- PainkillerInfoCard
- PainkillerWarningCard
- PainkillerErrorBanner
- PainkillerFilePlanCard
- PainkillerTargetCard
- PainkillerCommitSummaryCard
- PainkillerPrimaryActionButton

#### CATALON-GUARD UI Patterns NOT to Reuse

Do not reuse:

- Bottom navigation as default Painkiller structure
- Chat UI semantics
- Provider dashboard semantics
- Quota dashboard semantics
- Settings-heavy structure
- Project tree semantics unless useful only for simple file/target lists
- multi-tab app structure
- dashboard surfaces unrelated to upload flow

Painkiller is not a dashboard.
Painkiller is not a tool suite.
Painkiller is a focused upload-pain workflow.

#### Painkiller UI Direction

Painkiller should feel like a sibling tool to CATALON-GUARD, not a clone.

Painkiller must remain:

- mobile-first
- touch-first
- low-friction
- preview-first
- explicit-confirmation-first
- safe-by-default

Painkiller workflow always wins over visual similarity.

Use CATALON-GUARD’s visual grammar to make Painkiller feel familiar:

- same color family
- same rounded shape system
- same technical card/status surface feel
- similar compact spacing
- similar Material 3 tone

But do not copy CATALON-GUARD’s domain screens.

### Required Implementation Approach

Work gate by gate.

For every gate:

1. State goal.
2. Inspect current files.
3. Make the smallest useful implementation.
4. Add or update tests.
5. Run relevant tests/build commands if possible.
6. Report:
   - files changed
   - tests added
   - tests run
   - known limitations
   - next gate

Do not skip gates.
Do not implement future gates early.
Do not produce a giant diff.
Keep changes small and reviewable.

### Gate 0 — Project Skeleton + API/UI Spike

Goal:

Create or verify the Android/Kotlin/Compose project skeleton and establish the technical foundation.

Tasks:

1. Inspect repository structure.

2. If no Android project exists, create a minimal Android project using:
   - Kotlin
   - Jetpack Compose
   - Gradle Kotlin DSL
   - minSdk chosen reasonably
   - target/compile SDK compatible with current Android tooling

3. Add package/module skeletons:
   - `ui`
   - `ui.theme`
   - `ui.components`
   - `domain`
   - `domain.model`
   - `domain.usecase`
   - `data.github`
   - `data.files`
   - `data.zip`
   - `data.settings`
   - `data.security`

4. Add minimal app shell.

5. Add test setup.

6. Inspect CATALON-GUARD UI path:
   - `Android/CatalonGuard/app/src/main/java/com/catalon/guard/`
   - `presentation/theme/Color.kt`
   - `presentation/theme/Theme.kt`
   - `presentation/ui/`
   - `presentation/navigation/`

7. Create Painkiller base theme/components using the CATALON-GUARD UI grammar:
   - RauschRed `#FF5A5F`
   - BabuTeal `#00A699`
   - AccentAmber `#F7B731`
   - DarkBackground `#1A1A1A`
   - DarkSurface `#222222`
   - DarkSurfaceVariant `#2E2E2E`
   - LightBackground `#F5F5F5`
   - LightSurface `#FFFFFF`
   - LightSurfaceVariant `#F0F0F0`
   - shapes `4.dp`, `12.dp`, `24.dp`

8. Create minimal reusable UI components:
   - severity badge
   - info card
   - warning/error card or banner
   - primary action button

9. Spike direct GitHub Git Data API shape only as interfaces/models, not full implementation:
   - create blob
   - create tree
   - create commit
   - update ref
   - get ref
   - get commit/tree as needed

10. Create root project guidance files:
   - `claude.md`
   - `knownbugs.md`
   - `README.md`

11. Create reusable project template folder:
   - `templates/gated-android-project/`
   - include a concise template README or prompt structure for future gated Android projects.

Acceptance criteria:

- Project builds.
- Compose app launches to a minimal shell.
- Package structure exists.
- Painkiller theme exists using CATALON-GUARD color and shape grammar.
- Basic reusable components exist.
- GitHub API interfaces/models are present but not fully wired to production flow.
- `claude.md` exists and fits this project.
- `knownbugs.md` exists and has at least an initial empty structured log.
- `README.md` exists and explains current Gate 0 state.
- `templates/gated-android-project/` exists.
- No product feature beyond skeleton.
- No auth implementation yet unless already existing.
- No upload flow yet.

Tests:

- Build test.
- Basic unit test placeholder.
- Optional simple model serialization test if API models are created.

Hard stop:

- If Android project cannot build, stop and fix build before moving on.
- If CATALON-GUARD UI files differ from the expected structure, inspect before guessing.
- Do not invent UI details not present in the repo or not defined in this prompt.

### Gate 1 — File Intake without GitHub

Goal:

Implement local source intake and FilePlan generation without GitHub calls.

Tasks:

1. Implement SelectedSource and SourceKind.
2. Implement SAF abstraction for:
   - single file
   - multiple files
   - folder/tree
   - ZIP file
3. Do not assume real filesystem paths.
4. Read display names, sizes, MIME hints where available.
5. Generate virtual repo paths.
6. Normalize paths:
   - use `/`
   - remove duplicate slashes
   - block or sanitize `..`
   - reject absolute local paths
7. Implement optional ignore rules:
   - `.git/`
   - `.gradle/`
   - `build/`
   - `node_modules/`
   - `.idea/`
8. Implement FilePlan.
9. Add simple preview data for UI.

Acceptance criteria:

- User can select source type through Android SAF abstraction.
- Domain layer can produce FilePlan from selected source.
- No GitHub code involved.
- No upload.
- No commit.
- Tests cover path normalization and ignore rules.

Tests:

- path normalization
- blocked path traversal
- duplicate slash handling
- ignore rules
- virtual repo path generation

Hard stop:

- If SAF folder traversal is unreliable, document it and create ZIP-first fallback recommendation.

### Gate 2 — Large File Doctor

Goal:

Implement Large File Doctor as pure domain logic.

Tasks:

1. Implement:
   - SizeDiagnosis
   - SizeRiskLevel / DiagnosticSeverity
   - LargeFileDoctor
2. Apply thresholds:
   - >25 MB warning
   - >50 MiB strong warning
   - >100 MiB blocked
3. Mark Git LFS and Release Assets as deferred recommendations only.
4. Integrate diagnosis into FilePlan / UploadPlan.
5. Add human-readable messages.

Acceptance criteria:

- Every planned file receives a diagnosis.
- UploadPlan knows whether operation is blocked.
- Files >100 MiB block normal repo commit.
- No LFS upload implementation.
- No Release Asset upload implementation.

Tests:

- files below thresholds
- >25 MB
- >50 MiB
- >100 MiB
- mixed plans
- blocked upload plan

Hard stop:

- If size classification is ambiguous, keep conservative blocking behavior.

### Gate 3 — GitHub Auth + Repo List

Goal:

Implement GitHub authentication and read-only repo/branch listing.

Tasks:

1. Implement Auth abstraction:
   - GithubAuthState
   - SecureTokenStore
2. Use AppAuth-Android with Custom Tabs.
3. Do not use WebView.
4. Store tokens through SecureTokenStore abstraction backed by Android Keystore / AndroidX Security.
5. Never log tokens.
6. Implement repo listing.
7. Implement branch listing.
8. Add logout/delete token.

Acceptance criteria:

- User can authenticate.
- Token is stored securely through abstraction.
- Repo list loads.
- Branch list loads.
- Token never appears in logs.
- Errors are sanitized.

Tests:

- SecureTokenStore fake
- auth state handling
- repo API mock
- branch API mock
- error sanitization

Hard stop:

- If secure token handling is not clean, stop. Do not proceed to upload.

### Gate 4 — RepoTarget + Presets

Goal:

Implement target repo/branch/path selection.

Tasks:

1. Implement:
   - RepoTarget
   - BranchTarget
   - TargetPath
   - PainkillerPreset
2. Validate target path:
   - no absolute path
   - no `..`
   - normalized `/`
   - no duplicate slashes
3. Store last-used target or optional preset in DataStore.
4. Keep UI simple.

Acceptance criteria:

- User can choose repo, branch, target path.
- Invalid paths are blocked before preview.
- Last-used target can be restored.
- No complex preset manager.

Tests:

- target path validation
- DataStore fake/preset serialization
- invalid path messages

Hard stop:

- If presets start becoming a settings system, cut them back to last-used only.

### Gate 5 — UploadPlan + Preview UI

Goal:

Combine source + target into a full UploadPlan and show diagnosis preview.

Tasks:

1. Implement UploadPlan generation.
2. Detect:
   - new files
   - updated files if remote lookup is already available; otherwise mark as planned write pending remote check
   - ignored files
   - problematic files
   - blocked files
3. Generate commit message suggestion.
4. Build preview screen with severity groups:
   - Safe to commit
   - Warning, but allowed
   - Blocked
   - Deferred to later module
5. Show whether the operation will create one commit.
6. No upload yet unless Gate 6 has been reached.

Acceptance criteria:

- Preview is not a raw technical list.
- User can see exactly what will happen.
- Blocked files prevent confirmation.
- Commit message is editable later.
- No silent upload.

Tests:

- UploadPlan creation
- commit message suggestion
- severity grouping
- blocked confirmation state

Hard stop:

- If preview cannot clearly explain blocked/warning states, do not proceed to commit.

### Gate 6 — Single File Commit

Goal:

Implement safe commit of one small file using GitHub Git Data API.

Tasks:

1. Implement Git Data API flow:
   - get ref
   - get base commit/tree
   - create blob
   - create tree
   - create commit
   - update ref
2. Ensure update ref uses expected SHA / safe behavior.
3. Handle:
   - auth error
   - permission error
   - branch not found
   - protected branch
   - SHA mismatch / branch changed
   - network error
4. Show result with commit SHA/link.
5. Do not support multiple files yet.

Acceptance criteria:

- One small file can be created or updated in one commit.
- Branch ref only updates after commit is ready.
- Failure before ref update leaves repo visibly unchanged.
- Human-readable errors.
- No auto conflict resolution.

Tests:

- successful single file
- SHA mismatch
- protected branch
- auth error
- network error
- branch not found

Hard stop:

- If ref update safety cannot be guaranteed, stop and redesign before multi-file.

### Gate 7 — Multi File Commit

Goal:

Extend Git Data API execution to multiple files, folders, ZIP content, and `.gitkeep`.

Tasks:

1. Create blobs for all safe planned files.
2. Create one tree.
3. Create one commit.
4. Update branch ref only at the end.
5. Support folder structure.
6. Support ZIP virtual entries.
7. Inject `.gitkeep` for empty target folder behavior.
8. Prevent partial visible repo updates.
9. Preserve operation plan on failure.

Acceptance criteria:

- Multiple files commit as one commit.
- Folder upload works.
- ZIP content upload works after ZIP-Slip validation.
- Empty folder creates `.gitkeep`.
- No half-applied visible repo state.
- Blocked files prevent operation before blob creation.

Tests:

- multiple small files
- folder with subfolders
- ZIP with subfolders
- ZIP dangerous path blocked
- `.gitkeep`
- failure before ref update
- SHA mismatch during ref update

Hard stop:

- If multi-file safety is not understandable and safe, do not ship Gate 7.

### Gate 8 — Robustness

Goal:

Harden all known failure states.

Tasks:

1. Improve error mapping:
   - no network
   - GitHub unreachable
   - invalid token
   - token lacks permission
   - repo not found
   - private repo inaccessible
   - branch not found
   - protected branch
   - invalid target path
   - file already exists / update issue
   - SHA mismatch
   - remote changed while planning
   - rate limit
   - file too large
   - broken ZIP
   - dangerous ZIP path
   - Android URI no longer readable
   - URI permission lost
   - upload cancelled
   - partial failure
   - unknown API error
2. Each error must say:
   - what happened
   - whether user data was lost
   - what Painkiller did or did not do
   - next useful step
3. Sanitize logs.

Acceptance criteria:

- Known errors map to human-readable messages.
- No token in logs.
- No silent overwrites.
- No automatic conflict resolution.
- Operation plan sur  - `build/`
  - `node_modules/`
  - `.idea/`

Out of scope:

- real LFS upload
- Release Asset upload
- Conflict Cards
- automatic merge resolution
- background sync
- project dashboards
- Git history browser
- PRs
- branch graph
- file manager features unrelated to upload

### Large File Doctor Rules

Implement diagnosis logic:

- Small text/code/Markdown/JSON/source files:
  - normal commit

- File > 25 MB:
  - severity: WARNING
  - message: GitHub web upload would be unsuitable or limited; Painkiller will continue checking hard limits.

- File > 50 MiB:
  - severity: WARNING
  - message: GitHub recommends against normal repo files this large because they can make the repo heavy.

- File > 100 MiB:
  - severity: BLOCKED
  - normal repo commit must be blocked
  - message: This does not belong in a normal Git commit. Use Git LFS or Release Assets later.

Severity classes:

- SAFE
- WARNING
- BLOCKED
- DEFERRED

v0 only diagnoses/recommends Git LFS or Release Assets.
v0 does not implement real LFS upload or Release Asset publishing.

### UI Reference — CATALON-GUARD

Painkiller should use a UI language similar to the Android app UI in:

GitHub repository:
Lootziffer666/CATALON-GUARD

Relevant Android path:

- Android/CatalonGuard/app/src/main/java/com/catalon/guard/

Relevant UI paths:

- presentation/theme/Color.kt
- presentation/theme/Theme.kt
- presentation/navigation/NavGraph.kt
- presentation/ui/chat/ChatScreen.kt
- presentation/ui/projects/ProjectsScreen.kt
- presentation/ui/providers/
- presentation/ui/quota/
- presentation/ui/settings/

Use these values directly as the initial Painkiller UI grammar.

#### CATALON-GUARD Color Tokens

Use or adapt these color tokens:

- RauschRed: `#FF5A5F`
- BabuTeal: `#00A699`
- AccentAmber: `#F7B731`
- DarkBackground: `#1A1A1A`
- DarkSurface: `#222222`
- DarkSurfaceVariant: `#2E2E2E`
- LightBackground: `#F5F5F5`
- LightSurface: `#FFFFFF`
- LightSurfaceVariant: `#F0F0F0`

Painkiller mapping recommendation:

- Primary: RauschRed `#FF5A5F`
- Secondary: BabuTeal `#00A699`
- Tertiary / Warning Accent: AccentAmber `#F7B731`
- Dark background: `#1A1A1A`
- Dark surface: `#222222`
- Dark surface variant: `#2E2E2E`
- Light background: `#F5F5F5`
- Light surface: `#FFFFFF`
- Light surface variant: `#F0F0F0`

#### CATALON-GUARD Shape Tokens

Use this shape grammar:

- Small corners: `4.dp`
- Medium corners: `12.dp`
- Large corners: `24.dp`

Painkiller mapping recommendation:

- Small: chips, badges, compact technical labels
- Medium: cards, list items, warning/error containers
- Large: major panels, empty states, confirmation surfaces

#### CATALON-GUARD UI Patterns to Reuse

Reuse the visual language, not the domain semantics.

Useful patterns to reuse:

- Material 3 Scaffold structure
- TopAppBar-style page framing
- Surface/Card-based technical summaries
- compact status badges
- clear warning/error surfaces
- ErrorBanner pattern
- LazyColumn list structure
- compact spacing rhythm:
  - `8.dp`
  - `12.dp`
  - `16.dp`
  - `20.dp`
- calm technical density
- readable secondary text hierarchy
- small Surface badges for status/provider-like information
- FloatingActionButton only where it has one obvious primary action

Painkiller-specific component candidates:

- PainkillerStatusBadge
- PainkillerSeverityBadge
- PainkillerInfoCard
- PainkillerWarningCard
- PainkillerErrorBanner
- PainkillerFilePlanCard
- PainkillerTargetCard
- PainkillerCommitSummaryCard
- PainkillerPrimaryActionButton

#### CATALON-GUARD UI Patterns NOT to Reuse

Do not reuse:

- Bottom navigation as default Painkiller structure
- Chat UI semantics
- Provider dashboard semantics
- Quota dashboard semantics
- Settings-heavy structure
- Project tree semantics unless useful only for simple file/target lists
- multi-tab app structure
- dashboard surfaces unrelated to upload flow

Painkiller is not a dashboard.
Painkiller is not a tool suite.
Painkiller is a focused upload-pain workflow.

#### Painkiller UI Direction

Painkiller should feel like a sibling tool to CATALON-GUARD, not a clone.

Painkiller must remain:

- mobile-first
- touch-first
- low-friction
- preview-first
- explicit-confirmation-first
- safe-by-default

Painkiller workflow always wins over visual similarity.

Use CATALON-GUARD’s visual grammar to make Painkiller feel familiar:
- same color family
- same rounded shape system
- same technical card/status surface feel
- similar compact spacing
- similar Material 3 tone

But do not copy CATALON-GUARD’s domain screens.

### Required Implementation Approach

Work gate by gate.

For every gate:

1. State goal.
2. Inspect current files.
3. Make the smallest useful implementation.
4. Add or update tests.
5. Run relevant tests/build commands if possible.
6. Report:
   - files changed
   - tests added
   - tests run
   - known limitations
   - next gate

Do not skip gates.
Do not implement future gates early.
Do not produce a giant diff.
Keep changes small and reviewable.

### Gate 0 — Project Skeleton + API/UI Spike

Goal:

Create or verify the Android/Kotlin/Compose project skeleton and establish the technical foundation.

Tasks:

1. Inspect repository structure.
2. If no Android project exists, create a minimal Android project using:
   - Kotlin
   - Jetpack Compose
   - Gradle Kotlin DSL
   - minSdk chosen reasonably
   - target/compile SDK compatible with current Android tooling
3. Add package/module skeletons:
   - ui
   - ui.theme
   - ui.components
   - domain
   - domain.model
   - domain.usecase
   - data.github
   - data.files
   - data.zip
   - data.settings
   - data.security
4. Add minimal app shell.
5. Add test setup.
6. Inspect CATALON-GUARD UI path:
   - Android/CatalonGuard/app/src/main/java/com/catalon/guard/
   - presentation/theme/Color.kt
   - presentation/theme/Theme.kt
   - presentation/ui/
   - presentation/navigation/
7. Create Painkiller base theme/components using the CATALON-GUARD UI grammar:
   - RauschRed `#FF5A5F`
   - BabuTeal `#00A699`
   - AccentAmber `#F7B731`
   - DarkBackground `#1A1A1A`
   - DarkSurface `#222222`
   - DarkSurfaceVariant `#2E2E2E`
   - LightBackground `#F5F5F5`
   - LightSurface `#FFFFFF`
   - LightSurfaceVariant `#F0F0F0`
   - shapes `4.dp`, `12.dp`, `24.dp`
8. Create minimal reusable UI components:
   - severity badge
   - info card
   - warning/error card or banner
   - primary action button
9. Spike direct GitHub Git Data API shape only as interfaces/models, not full implementation:
   - create blob
   - create tree
   - create commit
   - update ref
   - get ref
   - get commit/tree as needed

Acceptance criteria:

- Project builds.
- Compose app launches to a minimal shell.
- Package structure exists.
- Painkiller theme exists using CATALON-GUARD color and shape grammar.
- Basic reusable components exist.
- GitHub API interfaces/models are present but not fully wired to production flow.
- No product feature beyond skeleton.
- No auth implementation yet unless already existing.
- No upload flow yet.

Tests:

- Build test.
- Basic unit test placeholder.
- Optional simple model serialization test if API models are created.

Hard stop:

- If Android project cannot build, stop and fix build before moving on.
- If CATALON-GUARD UI files differ from the expected structure, inspect before guessing.
- Do not invent UI details not present in the repo or not defined in this prompt.

### Gate 1 — File Intake without GitHub

Goal:

Implement local source intake and FilePlan generation without GitHub calls.

Tasks:

1. Implement SelectedSource and SourceKind.
2. Implement SAF abstraction for:
   - single file
   - multiple files
   - folder/tree
   - ZIP file
3. Do not assume real filesystem paths.
4. Read display names, sizes, MIME hints where available.
5. Generate virtual repo paths.
6. Normalize paths:
   - use `/`
   - remove duplicate slashes
   - block or sanitize `..`
   - reject absolute local paths
7. Implement optional ignore rules:
   - `.git/`
   - `.gradle/`
   - `build/`
   - `node_modules/`
   - `.idea/`
8. Implement FilePlan.
9. Add simple preview data for UI.

Acceptance criteria:

- User can select source type through Android SAF abstraction.
- Domain layer can produce FilePlan from selected source.
- No GitHub code involved.
- No upload.
- No commit.
- Tests cover path normalization and ignore rules.

Tests:

- path normalization
- blocked path traversal
- duplicate slash handling
- ignore rules
- virtual repo path generation

Hard stop:

- If SAF folder traversal is unreliable, document it and create ZIP-first fallback recommendation.

### Gate 2 — Large File Doctor

Goal:

Implement Large File Doctor as pure domain logic.

Tasks:

1. Implement:
   - SizeDiagnosis
   - SizeRiskLevel / DiagnosticSeverity
   - LargeFileDoctor
2. Apply thresholds:
   - >25 MB warning
   - >50 MiB strong warning
   - >100 MiB blocked
3. Mark Git LFS and Release Assets as deferred recommendations only.
4. Integrate diagnosis into FilePlan / UploadPlan.
5. Add human-readable messages.

Acceptance criteria:

- Every planned file receives a diagnosis.
- UploadPlan knows whether operation is blocked.
- Files >100 MiB block normal repo commit.
- No LFS upload implementation.
- No Release Asset upload implementation.

Tests:

- files below thresholds
- >25 MB
- >50 MiB
- >100 MiB
- mixed plans
- blocked upload plan

Hard stop:

- If size classification is ambiguous, keep conservative blocking behavior.

### Gate 3 — GitHub Auth + Repo List

Goal:

Implement GitHub authentication and read-only repo/branch listing.

Tasks:

1. Implement Auth abstraction:
   - GithubAuthState
   - SecureTokenStore
2. Use AppAuth-Android with Custom Tabs.
3. Do not use WebView.
4. Store tokens through SecureTokenStore abstraction backed by Android Keystore / AndroidX Security.
5. Never log tokens.
6. Implement repo listing.
7. Implement branch listing.
8. Add logout/delete token.

Acceptance criteria:

- User can authenticate.
- Token is stored securely through abstraction.
- Repo list loads.
- Branch list loads.
- Token never appears in logs.
- Errors are sanitized.

Tests:

- SecureTokenStore fake
- auth state handling
- repo API mock
- branch API mock
- error sanitization

Hard stop:

- If secure token handling is not clean, stop. Do not proceed to upload.

### Gate 4 — RepoTarget + Presets

Goal:

Implement target repo/branch/path selection.

Tasks:

1. Implement:
   - RepoTarget
   - BranchTarget
   - TargetPath
   - PainkillerPreset
2. Validate target path:
   - no absolute path
   - no `..`
   - normalized `/`
   - no duplicate slashes
3. Store last-used target or optional preset in DataStore.
4. Keep UI simple.

Acceptance criteria:

- User can choose repo, branch, target path.
- Invalid paths are blocked before preview.
- Last-used target can be restored.
- No complex preset manager.

Tests:

- target path validation
- DataStore fake/preset serialization
- invalid path messages

Hard stop:

- If presets start becoming a settings system, cut them back to last-used only.

### Gate 5 — UploadPlan + Preview UI

Goal:

Combine source + target into a full UploadPlan and show diagnosis preview.

Tasks:

1. Implement UploadPlan generation.
2. Detect:
   - new files
   - updated files if remote lookup is already available; otherwise mark as planned write pending remote check
   - ignored files
   - problematic files
   - blocked files
3. Generate commit message suggestion.
4. Build preview screen with severity groups:
   - Safe to commit
   - Warning, but allowed
   - Blocked
   - Deferred to later module
5. Show whether the operation will create one commit.
6. No upload yet unless Gate 6 has been reached.

Acceptance criteria:

- Preview is not a raw technical list.
- User can see exactly what will happen.
- Blocked files prevent confirmation.
- Commit message is editable later.
- No silent upload.

Tests:

- UploadPlan creation
- commit message suggestion
- severity grouping
- blocked confirmation state

Hard stop:

- If preview cannot clearly explain blocked/warning states, do not proceed to commit.

### Gate 6 — Single File Commit

Goal:

Implement safe commit of one small file using GitHub Git Data API.

Tasks:

1. Implement Git Data API flow:
   - get ref
   - get base commit/tree
   - create blob
   - create tree
   - create commit
   - update ref
2. Ensure update ref uses expected SHA / safe behavior.
3. Handle:
   - auth error
   - permission error
   - branch not found
   - protected branch
   - SHA mismatch / branch changed
   - network error
4. Show result with commit SHA/link.
5. Do not support multiple files yet.

Acceptance criteria:

- One small file can be created or updated in one commit.
- Branch ref only updates after commit is ready.
- Failure before ref update leaves repo visibly unchanged.
- Human-readable errors.
- No auto conflict resolution.

Tests:

- successful single file
- SHA mismatch
- protected branch
- auth error
- network error
- branch not found

Hard stop:

- If ref update safety cannot be guaranteed, stop and redesign before multi-file.

### Gate 7 — Multi File Commit

Goal:

Extend Git Data API execution to multiple files, folders, ZIP content, and `.gitkeep`.

Tasks:

1. Create blobs for all safe planned files.
2. Create one tree.
3. Create one commit.
4. Update branch ref only at the end.
5. Support folder structure.
6. Support ZIP virtual entries.
7. Inject `.gitkeep` for empty target folder behavior.
8. Prevent partial visible repo updates.
9. Preserve operation plan on failure.

Acceptance criteria:

- Multiple files commit as one commit.
- Folder upload works.
- ZIP content upload works after ZIP-Slip validation.
- Empty folder creates `.gitkeep`.
- No half-applied visible repo state.
- Blocked files prevent operation before blob creation.

Tests:

- multiple small files
- folder with subfolders
- ZIP with subfolders
- ZIP dangerous path blocked
- `.gitkeep`
- failure before ref update
- SHA mismatch during ref update

Hard stop:

- If multi-file safety is not understandable and safe, do not ship Gate 7.

### Gate 8 — Robustness

Goal:

Harden all known failure states.

Tasks:

1. Improve error mapping:
   - no network
   - GitHub unreachable
   - invalid token
   - token lacks permission
   - repo not found
   - private repo inaccessible
   - branch not found
   - protected branch
   - invalid target path
   - file already exists / update issue
   - SHA mismatch
   - remote changed while planning
   - rate limit
   - file too large
   - broken ZIP
   - dangerous ZIP path
   - Android URI no longer readable
   - URI permission lost
   - upload cancelled
   - partial failure
   - unknown API error
2. Each error must say:
   - what happened
   - whether user data was lost
   - what Painkiller did or did not do
   - next useful step
3. Sanitize logs.

Acceptance criteria:

- Known errors map to human-readable messages.
- No token in logs.
- No silent overwrites.
- No automatic conflict resolution.
- Operation plan survives where useful.

Tests:

- error mapping unit tests
- log sanitization
- mocked API failures

Hard stop:

- If a common failure produces cryptic raw API output, fix before release candidate.

### Gate 9 — v0 Release Candidate

Goal:

Prepare a minimal, safe v0.

Tasks:

1. UX polish.
2. README.
3. Known limits documentation.
4. Security review.
5. Test pass.
6. Manual test checklist.
7. Confirm out-of-scope features are not accidentally added.

Acceptance criteria:

- v0 solves upload pain.
- No large file blind commits.
- No silent upload.
- No token leakage.
- No feature creep.
- Known limitations are documented.

Tests:

- full unit test suite
- integration tests
- manual tests:
  - small Markdown
  - multiple Markdown files
  - folder with subfolders
  - ZIP with subfolders
  - ZIP with dangerous path
  - >25 MB
  - >50 MiB
  - >100 MiB
  - private repo
  - protected branch
  - network interruption

Hard stop:

- If v0 starts solving more than the upload pain, cut scope.
- If security is questionable, do not release.

### Data Models to Implement

Use Kotlin or Kotlin-like domain models.

Required models:

- SelectedSource
- SourceKind
- RepoTarget
- BranchTarget
- TargetPath
- FilePlan
- PlannedFileOperation
- UploadPlan
- SizeDiagnosis
- SizeRiskLevel / DiagnosticSeverity
- CommitPlan
- PainkillerPreset
- OperationResult
- HumanReadableError
- GithubAuthState
- GithubRepositorySummary
- GithubBranchSummary

Requirements:

- testable
- UI-friendly
- domain layer not coupled to Compose
- API layer not directly exposed to UI
- future Conflict Cards not blocked

### Error Message Style

Human-readable.
No raw Git nonsense unless useful.
No blame.
No panic.

Example:

SHA mismatch / branch changed:

“The branch changed on GitHub while Painkiller was preparing your upload. Painkiller stopped before updating the branch. Nothing was overwritten. Refresh the target and try again.”

File too large:

“This file is too large for a normal GitHub repository commit. Painkiller blocked the upload so the repo does not become heavy or fail later. Use Git LFS or Release Assets in a later workflow.”

Dangerous ZIP path:

“This ZIP contains a path that tries to escape the target folder. Painkiller blocked it before upload. Nothing was changed on GitHub.”

URI permission lost:

“Android no longer allows Painkiller to read this file. Select the source again. Nothing was changed on GitHub.”

### Output Requirements

For each response:

1. State current gate.
2. State what you changed.
3. List files created/modified.
4. List tests added/updated.
5. Say what commands you ran.
6. Say whether they passed.
7. State known limitations.
8. State next gate.

When editing code:

- keep diffs small
- do not perform unrelated cleanup
- do not rename large structures unless required
- do not add future features
- do not silently skip tests
- ask for next gate only after current gate is green or honestly blocked

### First Task

Start with Gate 0 only.

Do not implement Gate 1 yet.

Gate 0 deliverable:

- Android/Kotlin/Compose project skeleton
- package/module skeleton
- basic app shell
- test setup
- CATALON-GUARD UI inspection
- Painkiller theme based on CATALON-GUARD colors/shapes
- basic reusable UI components based on CATALON-GUARD grammar
- GitHub Git Data API interface/model spike only
- build passing if possible

Do not implement:
- file picker
- auth flow
- upload execution
- preview screen
- Large File Doctor
- presets
- Conflict Cards
- LFS
- Release Assets

Proceed with Gate 0.
