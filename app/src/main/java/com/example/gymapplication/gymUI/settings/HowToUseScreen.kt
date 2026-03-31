package com.example.gymapplication.gymUI.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToUseScreen(
    onBack: () -> Unit,
    onFinishOnboarding: () -> Unit = {},
    isOnboarding: Boolean = false
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("ANLEITUNG", "NEUIGKEITEN")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isOnboarding) "WILLKOMMEN" else "HILFE & UPDATES",
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    if (!isOnboarding) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isOnboarding) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                if (selectedTab == 0) {
                    GuideSection(
                        icon = Icons.Default.FitnessCenter,
                        title = "1. Übungen",
                        text = "In diesem Tab verwaltest du deinen individuellen Übungskatalog und ordnest jede Einheit einer Muskelgruppe zu. Eine integrierte Validierung verhindert leere Pflichtfelder, übernimmt jedoch exakte Zeichenfolgen inklusive Leerzeichen. Dies gewährleistet maximale Datenintegrität für Vergleiche oder die Muskel-Balance-Analyse, erfordert jedoch eine konsistente Namensgebung durch den Nutzer, um Redundanzen (z. B. 'Latzug' und 'Latzug   ') zu vermeiden. Zur besseren Orientierung am Gerät können optional Bilder via Kamera oder Galerie hinzugefügt und in der Detailansicht vergrößert werden.\n\nÜber das Drei-Punkte-Menü lassen sich Übungen jederzeit bearbeiten oder löschen. Die Detailansicht bietet zudem ein duales Notizsystem: Nutze 'Allgemeine Notizen' für bebilderte Ausführungsanleitungen und getrennte 'Trainings-Notizen' für sessionbasiertes Feedback (z. B. Fokus auf Technik). Fortschritte können manuell mit Sätzen, Wiederholungen und Gewicht geloggt oder automatisch über den Workout-Modus erfasst werden. Die Historie ermöglicht es, vergangene Einheiten einzusehen, das Datum anzupassen oder Einträge nachträglich zu korrigieren oder zu löschen."
                    )

                    GuideSection(
                        icon = Icons.Default.PlayCircle,
                        title = "2. Trainingspläne & Live-Workout-Modus",
                        text = "Dieser Tab kombiniert die strategische Planung mit einer hochflexiblen Workout-Execution. Erstelle individuelle Trainingspläne mit Pflichtvalidierung oder importiere bestehende Setups über die Import-Funktion. Deine Pläne lassen sich dynamisch organisieren: Füge Übungen aus deinem Katalog hinzu und bestimme die Reihenfolge durch intuitive Neupositionierung, wobei nachfolgende Übungen automatisch verschoben werden. Innerhalb der Plan-Ansicht erhältst du zudem Einblick in deine spezifische Leistungsentwicklung.\n\nDer Workout-Modus startet mit einer definierbaren Pausenzeit (Standard: 2 Min.) und bietet maximale Unterstützung während des Satzes: Erfasse Gewicht und Wiederholungen mit direktem Vergleich zu deinen 'Ghost-Werten' (letzte Einheit) und erhalte sofortiges Feedback bei neuen persönlichen Rekorden. Markiere Aufwärmsätze, um deine Statistik sauber zu halten. Das integrierte Notiz-Management erlaubt den Zugriff auf drei Ebenen: Aktuelle Session-Notizen (editierbar), Notizen der letzten Einheit (Read-only) und allgemeine Übungshinweise (editierbar). Navigiere flüssig per Button oder Swipe zwischen den Übungen.\n\nDer intelligente Rest-Timer läuft im Hintergrund weiter, bietet haptisches Feedback (Vibration) und lässt sich jederzeit manuell anpassen. Dank des 'Live-Buttons' kannst du die App während des Trainings verlassen oder Übungen sogar während der laufenden Einheit hinzufügen. Bei vollständigem Schließen der App pausiert das Training automatisch. Nach Abschluss generiert das System eine Sharecard für deine Erfolge. Deine Historie bleibt jederzeit editierbar, inklusive nachträglicher Datumsanpassung und detaillierter Einsicht in alle Notizen und Parameter."
                    )

                    GuideSection(
                        icon = Icons.Default.ShowChart,
                        title = "3. Fortschritt, Analyse & Vergleich",
                        text = "Dieser Bereich ist dein Analysezentrum, unterteilt in fünf spezialisierte Sub-Tabs (per Swipe oder Tap erreichbar):\n\n1. Körperwerte & Gewichte: Verwalte Gewicht und Umfänge in dedizierten Dashboards. Definiere Ziele (Zunehmen/Halten/Abnehmen), verfolge deinen Fortschritt per Fortschrittsbalken und analysiere Entwicklungen über interaktive, zoombare Graphen. Ein visuelles Highlight ist die Vergleichsfunktion für Fortschrittsbilder.\n\n2. Rekorde: Dein digitaler Trophäenschrank. Hier werden Bestleistungen (Max-Gewicht/Wiederholungen) inklusive Datum und deinem theoretischen 1-Rep-Max (1RM) für jede Übung gelistet.\n\n3. Analyse-Hub: Erhalte tiefe Einblicke in dein Training durch die Auswertung des Gesamtvolumens und der 'Muskel-Balance'. Ein Kreisdiagramm visualisiert die Belastungsverteilung der letzten 30 Tage – bis hinunter auf Übungsebene. Der 'Efficiency Factor' setzt zudem dein Körpergewicht in Relation zu deiner Maximalkraft.\n\n4. Social-Vergleich: Miss dich mit Freunden durch Daten-Import/Export. Das System nutzt einen intelligenten Zeit-Sync, um faire Vergleiche auf Basis aktueller Daten zu gewährleisten. Vergleiche deinen 'Max-Kraft Score' (Top 3 PRs), den 'Workload-Check' (30-Tage-Volumen), die Workout-Frequenz und den monatlichen Progression-Trend.\n\n5. Smart Matching: Da keine starren IDs genutzt werden, ermöglicht die App ein flexibles Mapping deiner Übungen mit denen deiner Freunde (automatisch via Name/Muskelgruppe oder manuell), um detaillierte Vergleichsgraphen und direkte Gegenüberstellungen aller Trainingseinträge zu erstellen."
                    )

                    GuideSection(
                        icon = Icons.Default.Event,
                        title = "4. Trainingskalender",
                        text = "Der integrierte Kalender dient als deine strategische Kommandozentrale für die Trainingssteuerung. Plane zukünftige Sessions, indem du einen deiner erstellten Pläne auswählst und eine spezifische Uhrzeit für die Push-Benachrichtigung festlegst. Visuelle Markierungen (Punkte) im Kalender geben dir sofortigen Aufschluss über anstehende und vergangene Einheiten.\n\nNach Abschluss eines Workouts wird die tatsächliche Trainingsdauer direkt im Kalender erfasst. Ein Klick auf einen Eintrag führt dich entweder zu den Details der absolvierten Einheit oder zur Vorbereitung deines geplanten Trainings. Da die App zwischen verbindlicher Planung und tatsächlicher Ausführung unterscheidet, bleiben zukünftige Einträge als Zielsetzung bestehen, auch wenn du eine Einheit außerplanmäßig an einem anderen Tag absolvierst. So behältst du die volle Kontrolle über dein manuelles Planungs-Management und kannst verpasste oder verschobene Einheiten flexibel organisieren."
                    )

                    GuideSection(
                        icon = Icons.Default.CloudDone,
                        title = "5. Optionen, Backup & Personalisierung",
                        text = "In diesem Tab passt du die App an deine Bedürfnisse an und sicherst deine wertvollen Trainingsdaten. Neben dieser Funktionsübersicht findest du hier die 'Neuigkeiten', welche dich über Updates und neue Features auf dem Laufenden halten. Personalisiere das Interface durch den Wechsel zwischen Dark- und Light-Mode für eine optimale Lesbarkeit in jeder Umgebung.\n\nDas Herzstück der Optionen ist das intelligente Backup-System: Konfiguriere automatische Sicherungen in einem frei wählbaren Verzeichnis (lokal oder Cloud-synchronisiert) auf täglicher oder wöchentlicher Basis zu deiner Wunschuhrzeit. Um Speicherplatz zu sparen, nutzt die App ein Rotationsprinzip, das maximal fünf Sicherungsstände vorhält und ältere Dateien automatisch überschreibt. Ein wichtiger technischer Hinweis: Die Ausführung erfolgt zeitoptimiert durch das Android-Betriebssystem. Um den Akku zu schonen und den Tiefschlaf des Geräts zu fördern, werden Hintergrundaufgaben gebündelt, weshalb die Sicherung leicht zeitversetzt zum geplanten Zeitpunkt erfolgen kann. Zusätzlich hast du jederzeit die volle Kontrolle über manuelle Exporte und die Wiederherstellung deiner Daten über die Import-Funktion."
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(
                                alpha = 0.2f
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.BugReport,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Fehler gefunden? Bitte melde Bugs oder Wünsche direkt an mich, damit ich sie so schnell wie möglich beheben kann!",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                } else {
                    UpdateNote(
                        version = "Version 1.2.1",
                        date = "31.03.2026",
                        description = "Stabilitäts- & UX-Update: Optimierte Navigation, Benachrichtigungen und umfassende Guides.",
                        features = listOf(
                            "NEU: Deep-Dive Guide – Die gesamte Funktionsweise der App wurde für einen Einstieg detailliert aufbereitet.",
                            "Navigation: Ab sofort kannst du im aktiven Workout-Modus intuitiv per Swipe zwischen den Übungen wechseln.",
                            "System: Grundlegende Überarbeitung der Benachrichtigungs-Logik für noch zuverlässigere Trainings-Erinnerungen.",
                            "Stabilität: Kritischer Bugfix im Kameramodul – die Aufnahme von Übungs- und Progress-Bildern läuft nun fehlerfrei.",
                            "Sicherheit: Erfolgreiche Validierung und Optimierung der Backup-Routine zur Gewährleistung maximaler Datensicherheit.",
                        )
                    )
                    UpdateNote(
                        version = "Version 1.2",
                        date = "30.03.2026",
                        description = "Massives UI-Update & das neue Freunde-Vergleichsfeature.",
                        features = listOf(
                            "NEU: Freunde-Battle – Vergleiche deinen Fortschritt jetzt direkt mit deinen Bros.",
                            "NEU: PR-Radar – Die App zeigt dir jetzt live während des Trainings an, wenn du kurz davor bist, einen neuen Rekord aufzustellen.",
                            "UI: Komplett überarbeiteter Fortschritt-Tab für eine flüssigere Analyse deiner Daten.",
                            "UI: Graphen & Kreisdiagramme wurden optimiert und unterstützen jetzt verbesserte Zoom- und Interaktionsgesten.",
                            "UI: Intuitive Navigation – Du kannst jetzt bequem per Swipe zwischen den Tabs und innerhalb der Graphen wechseln.",
                            "UI: Der Bild-Vergleich (Transformation-Split) ist jetzt noch präziser und einfacher zu bedienen.",
                            "NEU: Max-Kraft Score – Dein ultimativer Kraft-Wert aus der Summe deiner Top 3 Personal Records (z.B. Bench, Squat, Deadlift).",
                            "NEU: Workload-Check (30 Tage) – Behalte dein gesamtes bewegtes Volumen der letzten 30 Tage im Blick.",
                            "NEU: Konstanz-Index – Zählt knallhart deine erfolgreich beendeten Workouts der letzten 30 Tage.",
                            "NEU: Progression-Trend – Sieh auf einen Blick, wie viel Prozent du dich im Vergleich zum Vormonat gesteigert hast.",
                            "FIX: Optimierung der bestehenden Analyse-Funktionen für stabilere Performance."
                        )
                    )
                    UpdateNote(
                        version = "Version 1.1",
                        date = "27.03.2026",
                        description = "Optimierungen für den Workflow.",
                        features = listOf(
                            "NEU: Die Workout-Dauer wird jetzt präzise aufgenommen und gespeichert.",
                            "NEU: Du kannst jetzt feste Ziele für Körperwerte festlegen.",
                            "UI: Kein automatischer Screenwechsel mehr – du bleibst nach dem Speichern auf deiner aktuellen Ansicht.",
                            "Sicherheit: Überall in der App wurde eine Löschbestätigung hinzugefügt, um versehentliches Löschen zu verhindern.",
                            "NEU: Analyse-Dashboard mit Workload-Graph und Muskel-Balance-Diagrammen.",
                            "NEU: Automatisches Backup-System mit Zeitplan und Ordnerwahl.",
                            "NEU: Efficiency-Factor (Korrelation Kraft zu Körpergewicht).",
                            "NEU: Bild-Vergleich (Split-Screen) für Körper-Fortschritte.",
                            "NEU: Notizfunktion für Übungen und Einheiten",
                            "NEU: Workout Summary",
                            "UI: Modernisiertes Design für alle Dropdown-Menüs und Zeitwähler."
                        )
                    )
                    UpdateNote(
                        version = "Version 1.0 - Testversion",
                        date = "26.03.2026",
                        description = "Willkommen zur ersten offiziellen Version! Keine Internetverbindung nötig, deine Daten gehören dir.",
                        features = listOf(
                            "Übungen: Eigene Bilder mit Zoom & Muskelgruppen-Zuordnung.",
                            "Live-Workout: Ghost-Modus (Werte der letzten Einheit sehen) & Vibrations-Timer im Hintergrund.",
                            "Pläne: Einfaches Erstellen, Sortieren per Tippen und Export/Import für Freunde.",
                            "Fortschritt: Körperwerte-Tracking mit smarten Trend-Farben (Abnehmen/Zunehmen).",
                            "Analyse: PR-Bereich inklusive 1RM-Kalkulator.",
                            "Kalender: Workouts planen inkl. Erinnerungs-Benachrichtigung um 00:01 Uhr.",
                            "Backup: Vollständige Daten-Sicherung & Wiederherstellung (via Datei-Explorer/Drive)."
                        )
                    )
                }

                if (isOnboarding) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onFinishOnboarding,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(65.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            "VERSTANDEN & STARTEN",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

@Composable
fun GuideSection(icon: ImageVector, title: String, text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.3f
            )
        )
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun UpdateNote(version: String, date: String, description: String = "", features: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                version,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                date,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (description.isNotEmpty()) {
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                features.forEach { feature ->
                    Row {
                        Text(
                            "•",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(feature, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}