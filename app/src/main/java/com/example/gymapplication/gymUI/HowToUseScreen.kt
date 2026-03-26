package com.example.gymapplication.gymUI

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isOnboarding) "WILLKOMMEN" else "ANLEITUNG & FUNKTIONEN",
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (isOnboarding) {
                Text(
                    "Bevor du loslegst, hier ein kurzer Überblick über die wichtigsten Funktionen:",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            GuideSection(
                title = "1. Geräte",
                text = "Hier legst du den Grundstein: Erstelle Geräte und Übungen und ordne sie Muskelgruppen zu. Optional kannst du eigene Bilder hochladen (und in der Ansicht zoomen). Du kannst alles jederzeit bearbeiten, umbenennen oder löschen. Auch das manuelle Nachtragen von vergangenen Sätzen inklusive Datum ist hier problemlos möglich."
            )

            GuideSection(
                title = "2. Trainingspläne & Live-Workout",
                text = "Erstelle eigene Pläne, ordne deine Geräte über Dropdowns zu und ändere die Reihenfolge durch Tippen auf die Zahlen. Pläne können importiert, geteilt und jederzeit angepasst werden.\n\nStartest du ein Workout, legst du zuerst deine Pausenzeit fest. Im Workout trägst du deine Sätze ein (Aufwärmsätze werden nicht gespeichert) und hast deinen letzten Rekord als blassen 'Ghost-Wert' immer im Blick. Ein Timer trackt deine Pausen und das Handy vibriert, wenn es weitergeht. Verlässt du die App, läuft das Training im Hintergrund weiter. Du kannst jederzeit über den gelben 'LIVE'-Button unten ins aktive Workout zurückkehren."
            )

            GuideSection(
                title = "3. Fortschritt",
                text = "Tracke deinen Körper (Gewicht, Bizepsumfang etc.). Wähle beim Gewicht dein Ziel (Abnehmen, Zunehmen, Halten) für smarte Trend-Farben. In der Vorschau kannst du durch die Graphen wischen, im Vollbildmodus (tippen!) auch zoomen und genaue Werte ablesen.\n\nUnter 'Gewichte' siehst du deine Kraftsteigerung anhand deiner absolvierten Workouts. Der Bereich 'Rekorde' feiert deine Bestleistungen und berechnet sogar dein theoretisches 1RM (One Rep Max)!"
            )

            GuideSection(
                title = "4. Kalender & Planung",
                text = "Plane deine Workouts im Voraus! Geplante Einheiten kannst du erst am jeweiligen Tag starten. Um 00:01 Uhr an einem Trainingstag erinnert dich die App per Benachrichtigung an deinen Plan. Ein Tipp auf ein absolviertes Training bringt dich zu den Details, ein Tipp auf ein geplantes direkt zum Start des Workouts."
            )

            GuideSection(
                title = "5. Optionen",
                text = "Wechsle jederzeit zwischen Dark- und Light-Mode. Erstelle vollständige Backups deiner Daten (z.B. für einen Handywechsel) und importiere sie wieder, nach einem Neustart der App ist alles wieder da. Auch diese Anleitung findest du hier jederzeit wieder."
            )

            GuideSection(
                title = "6. Sonstiges",
                text = "Diese App läuft 100% lokal auf deinem Gerät, es ist keine Internetverbindung nötig und deine Daten gehören nur dir! \n\nTipp für Backups: Speichere sie am besten direkt in Google Drive oder einem ähnlichen Cloud-Dienst, da dafür der Datei-Explorer deines Handys geöffnet wird. Bei Fehlern oder Wünschen zur App, melde dich einfach bei mir!"
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isOnboarding) {
                Button(
                    onClick = onFinishOnboarding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("VERSTANDEN", fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun GuideSection(title: String, text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.5f
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )
        }
    }
}