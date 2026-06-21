# GRID Architecture

Kickoff-Dokument fuer das Projekt GRID - Intent-Driven GitHub Client fuer Solo-Devs.

---

## 1. Projektvision

- GRID ist ein **intent-driven simplification UI** ueber GitHub
- Nicht GitHub ersetzen, sondern wie GitHub sich dem User praesentiert ersetzen
- Solo-Dev-Scope: Commit, Push, Pull, Branch, PR, Repo-Management, Actions (Basics), Codespaces, Releases, Pages
- Kernphilosophie: **"Was willst du erreichen?"** - Ein Input - GRID erledigt die API-Calls
- Alles lesbar, nutzbar, menschlich machen
- Kein GitHub-Ersatz, sondern eine menschenlesbare Bedienoberflaeche ueber GitHubs API
- Ein Input vom User loest in GitHub mehrere Vorgaenge sequentiell aus
- Ziel: Kognitiven Overload eliminieren, GitHub-Jargon in menschliche Sprache uebersetzen, versteckte Funktionen simpel einsetzbar machen

---

## 2. Plattform-Entscheidung

- **Kotlin Multiplatform (KMP)**
- Shared Business Logic: pure Kotlin, plattformunabhaengig (80% der App)
- Android UI: Jetpack Compose
- Desktop UI (spaeter): Compose Desktop
- iOS (falls je): SwiftUI mit Shared Logic
- Begruendung: Einmal bauen, ueberall lauffaehig, einmaliges Updaten = alle Plattformen

---

## 3. Modulare Split-APK-Architektur

### Grundprinzip:
- Jedes Feature-Modul ist ein Dynamic Feature Module - erzeugt eigenen Split-APK
- Installation/Update einzelner Splits ueber SAI oder jeden Split-faehigen Paketmanager
- Kein Play Store noetig, kein Root noetig
- Fehlerhafte Module crashen isoliert - der Rest laeuft pflichtbewusst weiter

### Modul-Struktur:
```
:app (base)           <- Core, Crash Boundary, Module Registry, Shared Logic, Config Loader
:shared               <- KMP: Intent-Engine, Template Registry, GitHub API Client, Diagnosis, Error Translation
:feature:import       <- Split-APK: File Import Pipeline (ZIP, Ordner, Dateien, Share Sheet)
:feature:organize     <- Split-APK: Ordner erstellen, verschieben, umbenennen
:feature:protect      <- Split-APK: Branch Protection mit einem Klick
:feature:release      <- Split-APK: Release-Workflow
:feature:actions      <- Split-APK: GitHub Actions vereinfacht
:feature:pages        <- Split-APK: GitHub Pages aktivieren
```

### Crash-Isolation:
```kotlin
fun executeFeature(id: FeatureId, intent: UserIntent): Result<Outcome> {
    val module = registry.get(id) ?: return Result.failure(ModuleNotFound)
    return runCatching { module.execute(intent) }
        .onFailure { registry.disable(id, reason = it) }
}
```

### Feature-Entry-Interface:
Jedes Modul registriert sich beim Core ueber ein `FeatureEntry`-Interface:
```kotlin
interface FeatureEntry {
    val id: FeatureId
    val displayName: String
    val intents: List<IntentDefinition>
    suspend fun execute(intent: UserIntent): Outcome
    fun composableEntry(): @Composable () -> Unit
}
```

---

## 4. Zwei-Ebenen Hot-Fix-System

| Problem | Loesung | Geschwindigkeit |
|---|---|---|
| Bug in Template-Logik / Uebersetzung / Config | JSON-Datei auf Geraet ersetzen, App-Reload | Sekunden |
| Bug in Modul-Code (Kotlin/Compose) | Split-APK neu bauen, ueber SAI installieren | Minuten |
| Bug im Core | Base-APK neu bauen, Full-Install | Selten |

### Template-Hot-Swap (Dev-Modus):
```kotlin
val externalTemplates = File(context.getExternalFilesDir("templates")!!.path)
    .listFiles()
    ?.map { Json.decodeFromString<WorkflowTemplate>(it.readText()) }
    ?: emptyList()

templateRegistry.replaceAll(externalTemplates)
```

### Der Dev-Workflow:
1. Bug in Feature X - Fix nur in :feature:x
2. Nur diesen Split bauen
3. Via SAI den einen Split installieren
4. App-Restart - Fix aktiv

---

## 5. Intent-Template-System

### Architektur:
```
User Intent (1 Input)
       |
Workflow Template (kennt die Schritte)
       |
GitHub API Sequenz (REST/GraphQL, 2-8 Calls)
       |
Ergebnis in menschlicher Sprache
```

### Template-Schema (JSON-driven):
```json
{
  "id": "protect-main-branch",
  "displayName": "Branch schuetzen",
  "description": "Verhindert versehentliches Ueberschreiben deines Hauptzweigs",
  "requiredInputs": [
    { "key": "branch", "type": "branch_selector", "default": "main" }
  ],
  "steps": [
    { "action": "github.branch.getProtection", "params": { "branch": "{{branch}}" } },
    { "action": "github.branch.updateProtection", "params": {
        "branch": "{{branch}}",
        "enforce_admins": true,
        "required_pull_request_reviews": null,
        "restrictions": null,
        "allow_force_pushes": false,
        "allow_deletions": false
    }}
  ],
  "successMessage": "{{branch}} ist jetzt geschuetzt. Niemand kann direkt darauf pushen oder ihn loeschen.",
  "errorMessages": {
    "403": "Du hast keine Admin-Rechte fuer dieses Repo.",
    "404": "Branch '{{branch}}' existiert nicht."
  }
}
```

### Template-Kategorien:
| Kategorie | Beispiele | API-Tiefe |
|---|---|---|
| 1-Klick-Automationen | Branch schuetzen, Website aktivieren, Release machen | 2-5 Calls |
| Gefuehrte Flows | SSH einrichten, Erste Action, Neues Projekt | Multi-Step mit UI |
| Import-Pipelines | Datei/Ordner/ZIP - Repo | Git Data API Sequenz |
| Kontext-Reaktionen | Build fehlgeschlagen - Fehler zeigen, Conflict - Karten | State-Awareness |

---

## 6. Sprach-Uebersetzungsschicht

### Kern-Uebersetzungen:
| GitHub-Jargon | GRID-Sprache |
|---|---|
| Repository | Projekt |
| Branch | Arbeitsflaeche |
| Commit | Aenderung speichern |
| Pull Request | Aenderung einreichen |
| Merge | Uebernehmen |
| Rebase | Stand angleichen |
| Fork | Eigene Kopie |
| Conflict | Kollision |
| Push | Hochladen |
| Clone | Lokale Kopie holen |
| Squash and Merge | Zusammenfassen und abschliessen |
| Force Push | Ueberschreiben (Verlust moeglich) |
| HEAD | Aktueller Stand |
| Origin/Remote | Dein Projekt auf GitHub |
| LFS | Grosse Dateien / Asset-Speicher |
| .gitignore | Ausschlussliste |
| SSH Key | Sicherheitsschluessel |
| Protected Branch | Geschuetzter Zweig |
| Draft PR | Entwurf |

### Fehler-Uebersetzung:
| GitHub/Git Error | GRID sagt |
|---|---|
| `failed to push some refs` | Jemand hat in der Zwischenzeit etwas geaendert. Willst du die Aenderungen holen und zusammenfuehren? |
| `403 Forbidden` | Du hast keine Berechtigung fuer diese Aktion. |
| `branch is protected` | Dieser Zweig ist geschuetzt. Erstelle eine Arbeitsflaeche und reiche die Aenderung ein. |
| `file too large (>100MB)` | Diese Datei ist zu gross fuer ein normales Repo. GRID kann sie als Asset-Datei ablegen. |
| `merge conflict` | Kollision: Zwei Versionen derselben Datei. Welche soll gelten? |
| `rate limit exceeded` | GitHub braucht eine Pause. Versuche es in X Minuten erneut. |

---

## 7. Import-Pipeline (Erster Feature-Cluster)

### Flow:
1. **Quelle waehlen**: Datei / Ordner / ZIP / Share Sheet / Agent-Output
2. **Ziel waehlen**: Projekt - Arbeitsflaeche - Ordner (mit Presets/Favoriten)
3. **Diagnose**: Groesse, Konflikte, Pfade, LFS, gefaehrliche Dateien
4. **Vorschau**: Was wird neu / ersetzt / uebersprungen / blockiert
5. **Speichern**: Automatisch benannter Commit (editierbar)
6. **Ergebnis**: Link, Status, Fehler, Recovery

### Konflikte als Entscheidungskarten:
| Karte | Bedeutung |
|---|---|
| Repo-Version behalten | Vorhandene Datei bleibt |
| Meine Version uebernehmen | Upload ersetzt vorhandene Datei |
| Beide behalten | Neue Datei bekommt sicheren Namen |
| Vergleichen | Textdiff anzeigen (nur bei Text) |
| Spaeter klaeren | Datei aus dem Commit herausnehmen |

### Branch-Entscheidung (Ampelmodell):
| Farbe | Bedeutung | Aktion |
|---|---|---|
| Gruen | Ungefaehrliche Aenderung | Direkt speichern |
| Gelb | Groesserer Import / Risiko | Sichere Arbeitsflaeche erstellen |
| Rot | Konflikt / geschuetzter Branch | Erst klaeren / Aenderung einreichen |

### Large File Doctor:
| Groesse | Diagnose | Aktion |
|---|---|---|
| < 25 MB | Sicher | Normal speichern |
| 25-50 MB | Warnung | Hinweis, aber erlaubt |
| 50-100 MB | Starke Warnung | Empfehlung: Asset-Speicher |
| > 100 MB | Blockiert | Muss als Asset/LFS, kein normaler Commit |

---

## 8. Gate-Plan

Jedes Gate = ein Modul = ein Split = eine Liefereinheit. Jedes Gate ist ein PR, testbar, austauschbar.

### Gate 0: Core Shell
- **Modul:** `:app` (Base)
- **Liefert:** App Shell, Crash Boundary, Module Registry, Config Loader, KMP Setup
- **Akzeptanz:** App startet, Module koennen sich registrieren, Crash in Dummy-Modul wird gefangen
- **Kill:** Kein Feature-Code im Base

### Gate 1: Shared Logic Foundation
- **Modul:** `:shared`
- **Liefert:** Intent-Modell, Template-Schema, GitHub API Interface-Shape (nicht Implementierung), Error-Translation-Grundstruktur
- **Akzeptanz:** Template kann geladen und validiert werden, Intent-to-Template-Matching funktioniert
- **Kill:** Keine API-Calls, keine UI

### Gate 2: GitHub Auth
- **Modul:** `:shared` (Erweiterung)
- **Liefert:** OAuth-Flow, Secure Token Storage, Token-Refresh
- **Akzeptanz:** User kann sich einloggen, Token sicher gespeichert, Logout funktioniert
- **Kill:** Kein Token in Logs

### Gate 3: File Import - Source Intake
- **Modul:** `:feature:import`
- **Liefert:** SAF-Abstraktion (Datei/Ordner/ZIP), FilePlan-Generierung, Pfad-Normalisierung, Ignore-Rules
- **Akzeptanz:** User waehlt Quelle, Domain erzeugt Plan ohne GitHub-Calls
- **Kill:** Kein Upload, kein Commit

### Gate 4: File Import - Diagnosis
- **Modul:** `:feature:import` (Erweiterung)
- **Liefert:** Large File Doctor, ZIP-Slip-Prevention, Conflict Detection (gegen Remote)
- **Akzeptanz:** Jede Datei hat Diagnose, blockierte Dateien verhindern Commit
- **Kill:** Kein automatisches Ueberschreiben

### Gate 5: File Import - Preview & Confirm
- **Modul:** `:feature:import` (Erweiterung)
- **Liefert:** Preview-Screen (Severity Groups), Commit Message Suggestion, Explicit Confirmation, Ampel-Entscheidung (Branch)
- **Akzeptanz:** User sieht exakt was passiert, blockierte Dateien verhindern Bestaetigung
- **Kill:** Kein stilles Committen

### Gate 6: File Import - Execution
- **Modul:** `:feature:import` (Erweiterung)
- **Liefert:** Git Data API Orchestration (Blob - Tree - Commit - Ref Update), Error Handling, Ergebnisbericht
- **Akzeptanz:** Dateien landen als ein Commit im Repo, Ref-Update nur bei vollstaendigem Commit
- **Kill:** Keine partielle Repo-Aenderung sichtbar

### Gate 7: Organize
- **Modul:** `:feature:organize`
- **Liefert:** Ordner erstellen, Dateien umbenennen/verschieben, .gitkeep-Handling
- **Akzeptanz:** File-Manager-Aktionen funktionieren ueber GitHub API
- **Kill:** Kein lokaler Git-Clone

### Gate 8: Branch Protection
- **Modul:** `:feature:protect`
- **Liefert:** "Schuetze Branch" als 1-Klick-Template mit Solo-Dev-Defaults
- **Akzeptanz:** Ein Input - Branch ist geschuetzt
- **Kill:** Kein Settings-Screen mit 14 Checkboxen

### Gate 9: Release Workflow
- **Modul:** `:feature:release`
- **Liefert:** "Release erstellen" - Version - auto-generierte Notes - Tag - Publish
- **Akzeptanz:** Ein Input - Release existiert auf GitHub
- **Kill:** Kein manuelles Tag-Management noetig

### Gate 10: Actions Simplified
- **Modul:** `:feature:actions`
- **Liefert:** "Automatisch bauen bei Push" - Projekttyp erkennen - Workflow generieren - Commit
- **Akzeptanz:** User bekommt funktionierenden CI-Workflow ohne YAML-Kenntnisse
- **Kill:** Kein Workflow-Editor, kein YAML sichtbar

### Gate 11: GitHub Pages
- **Modul:** `:feature:pages`
- **Liefert:** "Website aktivieren" - Pages konfigurieren - URL anzeigen
- **Akzeptanz:** Ein Klick - Website live
- **Kill:** Kein Custom-Domain-Setup im MVP

---

## 9. Shared Logic Architektur (KMP Detail)

```
shared/
├── intent/
│   ├── IntentParser.kt          <- User-Input -> Intent-Objekt
│   ├── IntentDefinition.kt      <- Was ein Intent braucht (inputs, validierung)
│   └── IntentMatcher.kt         <- Intent -> passendes Template finden
├── templates/
│   ├── TemplateRegistry.kt      <- Alle verfuegbaren Templates
│   ├── TemplateLoader.kt        <- Laedt aus embedded + externem Pfad
│   ├── WorkflowTemplate.kt      <- Template-Datenmodell
│   └── TemplateValidator.kt     <- Schema-Validierung
├── api/
│   ├── GitHubClient.kt          <- REST + GraphQL unified
│   ├── GitDataApi.kt            <- Blob/Tree/Commit/Ref
│   ├── RepoApi.kt               <- Repos, Branches, Protection
│   ├── ActionsApi.kt            <- Workflows
│   ├── ReleasesApi.kt           <- Tags, Releases
│   └── PagesApi.kt              <- Pages Config
├── executor/
│   ├── SequenceRunner.kt        <- Fuehrt Template-Steps aus
│   ├── RollbackHandler.kt       <- Bei Fehler: Aufraeumen
│   ├── ResumeState.kt           <- Unterbrochene Sequenz fortsetzen
│   └── PreCheck.kt              <- Validierung vor Ausfuehrung
├── diagnosis/
│   ├── LargeFileDoctor.kt       <- Groessendiagnose
│   ├── ConflictDetector.kt      <- Remote-Vergleich
│   ├── PathValidator.kt         <- Normalisierung, ZIP-Slip, Traversal
│   └── ZipAnalyzer.kt           <- ZIP-Inhalt analysieren
├── translation/
│   ├── JargonMap.kt             <- GitHub-Begriff -> Menschensprache
│   ├── ErrorTranslator.kt       <- API-Error -> verstaendliche Nachricht
│   └── StatusTranslator.kt      <- Zustaende -> menschliche Labels
└── model/
    ├── Project.kt               <- = Repository
    ├── Workspace.kt             <- = Branch
    ├── ChangePackage.kt         <- = Commit-Inhalt
    ├── FilePlan.kt              <- Geplante Dateiaktionen
    ├── Diagnosis.kt             <- Ergebnis der Analyse
    └── Outcome.kt               <- Ergebnis einer Ausfuehrung
```

---

## 10. UI-Designprinzipien

- **Intent-first**: Startscreen zeigt nicht Repos, sondern Aktionen ("Was willst du tun?")
- **Progressive Disclosure**: Einfach - Erweitert - Experte (nie alles auf einmal)
- **Preview before Action**: Jede Aktion zeigt vorher, was passieren wird
- **Human Language**: Kein Git-Jargon in der Hauptansicht (optional als Experten-Untertitel)
- **Fehler = Hilfe**: Jeder Fehler sagt was passiert ist, ob Daten verloren sind, was als naechstes zu tun ist
- **Mobile-first, Touch-first**: Designed fuer Daumen, nicht fuer Maus
- **Calm Technical Density**: CATALON-GUARD-Farbsystem, Material 3, Compact Spacing

### Farbsystem (aus CATALON-GUARD):
- Primary: `#FF5A5F` (RauschRed)
- Secondary: `#00A699` (BabuTeal)
- Warning: `#F7B731` (AccentAmber)
- Dark Background: `#1A1A1A`
- Dark Surface: `#222222`
- Light Background: `#F5F5F5`

---

## 11. Nicht-Ziele (explizit ausgeschlossen)

- Issue-Management (nicht im MVP)
- Full PR-Review-Workflow
- Git History / Blame / Commit Graph
- IDE / Code Editor
- Automatische Konfliktloesung
- Force Push
- Team/Org-Features
- Vollstaendiger Git-Client
- Stille Overwrites
- Actions Workflow-Editor (nur Generator)
