package com.example.medtracker

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medtracker.data.MedicationRepository
import com.example.medtracker.model.Medication
import com.example.medtracker.reminder.ReminderScheduler
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = MedicationRepository(this)
        val scheduler = ReminderScheduler(this)

        setContent {
            MaterialTheme {
                MedTrackerApp(
                    initialMedications = repository.getAll(),
                    canScheduleExactAlarms = scheduler.canScheduleExactAlarms(),
                    onRequestExactAlarmAccess = { requestExactAlarmAccess() },
                    onAddMedication = { name, dosage, hour, minute, notes ->
                        repository.add(name, dosage, hour, minute, notes)
                        val updated = repository.getAll()
                        scheduler.rescheduleAll(updated)
                        updated
                    },
                    onDeleteMedication = { id ->
                        repository.delete(id)
                        scheduler.cancelMedication(id)
                        repository.getAll()
                    },
                    onMarkTaken = { id ->
                        repository.markTaken(id)
                        repository.getAll()
                    }
                )
            }
        }
    }

    private fun requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }
}

@Composable
private fun MedTrackerApp(
    initialMedications: List<Medication>,
    canScheduleExactAlarms: Boolean,
    onRequestExactAlarmAccess: () -> Unit,
    onAddMedication: (String, String, Int, Int, String) -> List<Medication>,
    onDeleteMedication: (Int) -> List<Medication>,
    onMarkTaken: (Int) -> List<Medication>
) {
    val medications = remember { mutableStateListOf<Medication>().apply { addAll(initialMedications) } }
    val notificationPermissionLauncher = rememberNotificationPermissionLauncher()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Medikamente verfolgen und erinnern", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Die App plant taegliche Alarme neu nach Geraetestart und zeigt eine sichtbare Erinnerung auch auf dem Sperrbildschirm.",
                style = MaterialTheme.typography.bodyMedium
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Button(onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                    Text("Benachrichtigungen erlauben")
                }
            }

            if (!canScheduleExactAlarms) {
                Button(onClick = onRequestExactAlarmAccess) {
                    Text("Exakte Alarme aktivieren")
                }
            }

            MedicationForm(
                onSave = { name, dosage, hour, minute, notes ->
                    medications.clear()
                    medications.addAll(onAddMedication(name, dosage, hour, minute, notes))
                }
            )

            MedicationList(
                medications = medications,
                contentPadding = PaddingValues(bottom = 24.dp),
                onDelete = { id ->
                    medications.clear()
                    medications.addAll(onDeleteMedication(id))
                },
                onMarkTaken = { id ->
                    medications.clear()
                    medications.addAll(onMarkTaken(id))
                }
            )
        }
    }
}

@Composable
private fun rememberNotificationPermissionLauncher() =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

@Composable
private fun MedicationForm(
    onSave: (String, String, Int, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var hour by remember { mutableIntStateOf(8) }
    var minute by remember { mutableIntStateOf(0) }
    var notes by remember { mutableStateOf("") }

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Neues Medikament", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") }
            )
            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Dosis") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = hour.toString(),
                    onValueChange = { hour = it.filter(Char::isDigit).toIntOrNull()?.coerceIn(0, 23) ?: 0 },
                    modifier = Modifier.weight(1f),
                    label = { Text("Stunde") }
                )
                OutlinedTextField(
                    value = minute.toString(),
                    onValueChange = {
                        minute = it.filter(Char::isDigit).toIntOrNull()?.coerceIn(0, 59) ?: 0
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("Minute") }
                )
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Hinweise") }
            )
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    onSave(name, dosage, hour, minute, notes)
                    name = ""
                    dosage = ""
                    hour = 8
                    minute = 0
                    notes = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Speichern und Alarm planen")
            }
        }
    }
}

@Composable
private fun MedicationList(
    medications: List<Medication>,
    contentPadding: PaddingValues,
    onDelete: (Int) -> Unit,
    onMarkTaken: (Int) -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm") }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(medications, key = { medication -> medication.id }) { medication ->
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(medication.name, style = MaterialTheme.typography.titleLarge)
                    Text("Dosis: ${medication.dosage.ifBlank { "-" }}")
                    Text("Taeglich um ${medication.hour.toString().padStart(2, '0')}:${medication.minute.toString().padStart(2, '0')}")
                    if (medication.notes.isNotBlank()) {
                        Text("Hinweise: ${medication.notes}")
                    }
                    Text(
                        "Zuletzt genommen: ${medication.lastTakenAt?.let { timestamp ->
                            formatter.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
                        } ?: "noch nicht bestaetigt"}"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onMarkTaken(medication.id) }) {
                            Text("Jetzt als genommen")
                        }
                        TextButton(onClick = { onDelete(medication.id) }) {
                            Text("Loeschen")
                        }
                    }
                }
            }
        }
    }
}
