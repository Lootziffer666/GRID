# 🚪 GATES — GRID

---

## 🔜 Nächste Gates

### Gate PK-011: Clipboard Manager
- **Branch:** `gate/pk-011-clipboard`
- **To-Dos:**
  - [ ] History (letzte 50)
  - [ ] Pinned-Einträge (persistent)
  - [ ] Schnellzugriff per Notification/Overlay
  - [ ] Sensitiv-Erkennung (Passwörter auto-löschen)
- **Akzeptanz:** History + Pins funktionieren
- **Kill:** Clipboard an Cloud senden

### Gate PK-012: Notification Filter
- **Branch:** `gate/pk-012-notification-filter`
- **To-Dos:**
  - [ ] Whitelist/Blacklist pro App
  - [ ] Keyword-Filter
  - [ ] Zeitbasierte Regeln (Nachtmodus)
  - [ ] Log verpasster Notifications
- **Akzeptanz:** Unerwünschte gefiltert, Log verfügbar
- **Kill:** Listener-Permission ohne Erklärung

### Gate PK-013: Quick Settings Tiles
- **Branch:** `gate/pk-013-quick-tiles`
- **To-Dos:**
  - [ ] Tile-API
  - [ ] Konfigurierbare Tiles
  - [ ] Status-Feedback im Icon
- **Akzeptanz:** Tiles funktionieren aus Quick Settings
- **Kill:** Mehr als 6 Tiles

### Gate PK-014: Battery Optimizer
- **Branch:** `gate/pk-014-battery`
- **To-Dos:**
  - [ ] Battery-Usage-Übersicht
  - [ ] Aggressive-Doze-Profile
  - [ ] Lade-Erinnerungen
  - [ ] Health-Tracking über Zeit
- **Akzeptanz:** Statistiken sichtbar, Profile aktivierbar
- **Kill:** Root nötig

### Gate PK-015: File Cleaner
- **Branch:** `gate/pk-015-file-cleaner`
- **To-Dos:**
  - [ ] Duplikate finden (Hash)
  - [ ] Große Dateien identifizieren
  - [ ] Leere Ordner
  - [ ] Vorschau vor Löschung
- **Akzeptanz:** Korrekte Vorschläge
- **Kill:** Auto-Löschen ohne Bestätigung
