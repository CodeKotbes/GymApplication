# Gym Tracker - Fitness & Workout Tracker

Eine umfassende, datengetriebene Android-Fitness-App zur individuellen Trainingssteuerung, Leistungsanalyse und zum direkten Vergleich mit Freunden. Gym Tracker kombiniert Workout-Planung mit einer Live-Execution und Analysetools.

---

## Hauptfunktionen

### 1. Individuelle Übungsverwaltung
Baue deinen eigenen, Übungskatalog auf.
* **Muskelgruppen** Ordne jeder Übung eine Muskelgruppe zu.
* **Datenintegrität:** Die integrierte Validierung erfordert konsistente Namensgebung zur Vermeidung von Redundanzen (wichtig für die Muskel-Balance-Analyse).
* **Medienunterstützung:** Füge Bilder via Kamera oder Galerie für eine bessere Orientierung im Studio hinzu.
* **Duales Notizsystem:** Trennung von bebilderten Ausführungsanleitungen (Allgemeine Notizen) und sessionbasiertem Feedback (Trainings-Notizen).
* **Vollständige Historie:** Vergangene Einheiten einsehen, editieren oder manuell loggen.

### 2. Trainingspläne & Live-Workout-Modus
Von der Planung bis zur flexiblen Ausführung im Gym.
* **Pläne:** Erstelle Pläne mit Pflichtvalidierung, importiere Setups und ändere die Übungsreihenfolge intuitiv per Drag-and-Drop.
* **Live-Workout-Tracking:** Erfasse Gewicht und Wiederholungen im direkten Vergleich zu deinen "Ghost-Werten" (letzte Einheit) und erhalte sofortiges Feedback bei neuen PRs.
* **Rest-Timer:** Läuft im Hintergrund mit haptischem Feedback (Standard: 2 Min, jederzeit anpassbar).
* **Multitasking & Pausen-Logik:** Verlasse die App während des Trainings oder lass das Training bei App-Schließung automatisch pausieren.
* **Erfolge teilen:** Automatische Generierung von Sharecards nach Abschluss eines Workouts.

### 3. Fortschritt, Analyse & Vergleich
Dein Analysezentrum für maximalen Überblick, unterteilt in fünf spezialisierte Dashboards.
* **Körperwerte & Gewichte:** Verfolge Ziele (Zunehmen/Abnehmen/Halten) mit zoombaren Graphen und einer visuellen Vergleichsfunktion für Fortschrittsbilder.
* **Rekorde:** Automatische Erfassung von Bestleistungen (Max-Gewicht/Reps) und Berechnung des theoretischen 1-Rep-Max (1RM).
* **Analyse:** Visualisierung des Gesamtvolumens und der Muskel-Balance der letzten 30 Tage als Kreisdiagramm. Enthält einen "Efficiency Factor" (Körpergewicht vs. Maximalkraft).
* **Vergleich:** Miss dich mit Freunden per Daten-Im-/Export. Intelligenter Zeit-Sync garantiert faire Vergleiche bei "Max-Kraft Score", Workout-Frequenz und 30-Tage-Volumen.
* **Smart Matching:** Flexibles Übungs-Mapping (automatisch oder manuell) für direkte Gegenüberstellungen von Trainingseinträgen.

### 4. Trainingskalender
Deine strategische Kommandozentrale für die Trainingssteuerung.
* **Zielgerichtete Planung:** Weise Trainingspläne bestimmten Tagen zu und konfiguriere Push-Benachrichtigungen.
* **Visuelles Tracking:** Klare Indikatoren für anstehende und absolvierte Einheiten inklusive erfasster Trainingsdauer.
* **Maximale Flexibilität:** Geplante Einheiten bleiben als Zielsetzung bestehen, auch wenn das tatsächliche Workout an einem anderen Tag absolviert wird.

### 5. Optionen & Backup
Passe die App an und sichere deine Daten zuverlässig.
* **Personalisierung:** Vollständiger Dark- und Light-Mode Support.
* **Automatisiertes Backup:** Tägliche oder wöchentliche Sicherungen (lokal oder Cloud-synchronisiert) zur Wunschuhrzeit.
* **Speicheroptimierte Rotation:** Es werden maximal fünf Sicherungsstände vorgehalten, ältere Dateien werden automatisch überschrieben.
* **Manuelle Kontrolle:** Jederzeitige Möglichkeit für manuelle Exporte und Wiederherstellungen.

---

## Tech Stack & Architektur
* **UI-Framework:** Jetpack Compose
* **Sprache:** Kotlin
* **Architektur:** MVVM (Model-View-ViewModel)
* **Datenbank:** Room / SQLite

---

## Installation & Build

1. Klone das Repository:
   ```bash
   git clone [https://github.com/CodeKotbes/GymApplication.git](https://github.com/CodeKotbes/GymApplication.git)
   ```
2. Öffne das Projekt in **Android Studio**.
3. Synchronisiere Gradle und starte den Build-Prozess.
4. Führe die App auf einem Emulator oder einem physischen Android-Gerät aus.
