package com.example.gymapplication.gymUI

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
                        text = "Erstelle Übungen und ordne sie Muskelgruppen zu. Du kannst eigene Bilder hochladen, zoomen und jederzeit alles bearbeiten."
                    )

                    GuideSection(
                        icon = Icons.Default.PlayCircle,
                        title = "2. Live-Workouts",
                        text = "Starte Pläne mit festen Pausenzeiten. Während des Trainings siehst du deine letzten Sätze als 'Ghost-Werte'. Der Timer läuft im Hintergrund weiter wenn du die App verlässt."
                    )

                    GuideSection(
                        icon = Icons.Default.ShowChart,
                        title = "3. Fortschritt & Rekorde",
                        text = "Tracke dein Körpergewicht, Umfänge und deine Gewichte von den Übungen. Die App berechnet deine Bestleistungen, dein theoretisches 1 Rep Max (1RM) und stellt dir Analysen deiner Übungen zusammen."
                    )

                    GuideSection(
                        icon = Icons.Default.Event,
                        title = "4. Kalender & Planung",
                        text = "Plane Workouts für die Zukunft. Die App erinnert dich am Trainingstag per Benachrichtigung. Ein Tipp auf den Kalendereintrag bringt dich direkt zum Start oder zu den Details."
                    )

                    GuideSection(
                        icon = Icons.Default.CloudDone,
                        title = "5. Backup & Sicherheit",
                        text = "Deine Daten liegen 100% lokal. Nutze die Backup-Funktion in den Optionen, um deine Fortschritte (z.B. in Google Drive) zu sichern."
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
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
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