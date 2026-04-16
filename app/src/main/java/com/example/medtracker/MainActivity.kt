package com.example.medtracker

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medtracker.data.MedicationRepository
import com.example.medtracker.model.IntakeTime
import com.example.medtracker.model.Medication
import com.example.medtracker.reminder.ReminderScheduler
import com.example.medtracker.ui.theme.MedTrackerTheme
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
            MedTrackerTheme {
                MedTrackerApp(
                    initialMedications = repository.getAll(),
                    canScheduleExactAlarms = scheduler.canScheduleExactAlarms(),
                    onRequestExactAlarmAccess = { requestExactAlarmAccess() },
                    onAddMedication = { name, dosage, intakeTimes, notes ->
                        repository.add(name, dosage, intakeTimes, notes)
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

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun MedTrackerApp(
    initialMedications: List<Medication>,
    canScheduleExactAlarms: Boolean,
    onRequestExactAlarmAccess: () -> Unit,
    onAddMedication: (String, String, List<IntakeTime>, String) -> List<Medication>,
    onDeleteMedication: (Int) -> List<Medication>,
    onMarkTaken: (Int) -> List<Medication>
) {
    val medications = remember { mutableStateListOf<Medication>().apply { addAll(initialMedications) } }
    val notificationPermissionLauncher = rememberNotificationPermissionLauncher()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                        )
                    )
                )
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1360.dp)
                    .align(Alignment.TopCenter)
            ) {
                val tabletLayout = maxWidth >= 900.dp
                val sharedModifier = Modifier.fillMaxSize()

                if (tabletLayout) {
                    Row(
                        modifier = sharedModifier,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(0.95f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            HeroCard(
                                medicationCount = medications.size,
                                canScheduleExactAlarms = canScheduleExactAlarms,
                                onRequestNotifications = {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                                onRequestExactAlarmAccess = onRequestExactAlarmAccess
                            )
                            MedicationForm(
                                onSave = { name, dosage, intakeTimes, notes ->
                                    medications.clear()
                                    medications.addAll(onAddMedication(name, dosage, intakeTimes, notes))
                                }
                            )
                        }

                        MedicationList(
                            medications = medications,
                            modifier = Modifier.weight(1.25f),
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
                } else {
                    LazyColumn(
                        modifier = sharedModifier,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            HeroCard(
                                medicationCount = medications.size,
                                canScheduleExactAlarms = canScheduleExactAlarms,
                                onRequestNotifications = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onRequestExactAlarmAccess = onRequestExactAlarmAccess
                            )
                        }
                        item {
                            MedicationForm(
                                onSave = { name, dosage, intakeTimes, notes ->
                                    medications.clear()
                                    medications.addAll(onAddMedication(name, dosage, intakeTimes, notes))
                                }
                            )
                        }
                        item {
                            MedicationSectionHeader(count = medications.size)
                        }
                        items(medications, key = { medication -> medication.id }) { medication ->
                            MedicationCard(
                                medication = medication,
                                onDelete = { onDeleteMedication(medication.id).also {
                                    medications.clear()
                                    medications.addAll(it)
                                } },
                                onMarkTaken = { onMarkTaken(medication.id).also {
                                    medications.clear()
                                    medications.addAll(it)
                                } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberNotificationPermissionLauncher() =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

@Composable
private fun HeroCard(
    medicationCount: Int,
    canScheduleExactAlarms: Boolean,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarmAccess: () -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ),
        shape = RoundedCornerShape(32.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "$medicationCount aktiv geplant",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "Deine Medikamente klar, ruhig und alltagstauglich organisiert.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Plane mehrere Einnahmezeiten pro Tag, halte Dosierungen konsistent und behalte den letzten Einnahmezeitpunkt im Blick.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Button(onClick = onRequestNotifications) {
                        Text("Benachrichtigungen")
                    }
                }
                if (!canScheduleExactAlarms) {
                    OutlinedButton(onClick = onRequestExactAlarmAccess) {
                        Text("Exakte Alarme")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MedicationForm(
    onSave: (String, String, List<IntakeTime>, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var dosageExpanded by remember { mutableStateOf(false) }
    var hour by remember { mutableIntStateOf(8) }
    var minute by remember { mutableIntStateOf(0) }
    var notes by remember { mutableStateOf("") }
    val intakeTimes = remember { mutableStateListOf(IntakeTime(8, 0)) }

    val dosageSuggestions = listOf(
        "1 Tablette",
        "1/2 Tablette",
        "2 Tabletten",
        "5 ml",
        "10 ml",
        "1 Kapsel"
    )

    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Neues Medikament", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Lege Name, Dosis und beliebig viele Tageszeiten fest.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                shape = RoundedCornerShape(20.dp),
                singleLine = true
            )
            ExposedDropdownMenuBox(
                expanded = dosageExpanded,
                onExpandedChange = { dosageExpanded = it }
            ) {
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable, true),
                    label = { Text("Dosis") },
                    placeholder = { Text("z. B. 1 Tablette") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dosageExpanded) },
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = dosageExpanded,
                    onDismissRequest = { dosageExpanded = false }
                ) {
                    dosageSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                dosage = suggestion
                                dosageExpanded = false
                            }
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Einnahmezeiten", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = hour.toString(),
                            onValueChange = { hour = it.filter(Char::isDigit).toIntOrNull()?.coerceIn(0, 23) ?: 0 },
                            modifier = Modifier.weight(1f),
                            label = { Text("Stunde") },
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = minute.toString(),
                            onValueChange = { minute = it.filter(Char::isDigit).toIntOrNull()?.coerceIn(0, 59) ?: 0 },
                            modifier = Modifier.weight(1f),
                            label = { Text("Minute") },
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val candidate = IntakeTime(hour, minute)
                            if (intakeTimes.none { it.hour == candidate.hour && it.minute == candidate.minute }) {
                                intakeTimes.add(candidate)
                                intakeTimes.sortWith(compareBy<IntakeTime> { it.hour }.thenBy { it.minute })
                            }
                        }
                    ) {
                        Text("Zeit hinzufügen")
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        intakeTimes.forEach { intakeTime ->
                            FilterChip(
                                selected = false,
                                onClick = { intakeTimes.remove(intakeTime) },
                                label = { Text(intakeTime.label()) },
                                trailingIcon = {
                                    Text("x", color = MaterialTheme.colorScheme.primary)
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Hinweise") },
                placeholder = { Text("Mit Wasser, nach dem Essen, nicht vergessen ...") },
                shape = RoundedCornerShape(20.dp)
            )
            Button(
                onClick = {
                    if (name.isBlank() || intakeTimes.isEmpty()) return@Button
                    onSave(name, dosage, intakeTimes.toList(), notes)
                    name = ""
                    dosage = ""
                    hour = 8
                    minute = 0
                    notes = ""
                    intakeTimes.clear()
                    intakeTimes.add(IntakeTime(8, 0))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Medikament speichern")
            }
        }
    }
}

@Composable
private fun MedicationList(
    medications: List<Medication>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    onDelete: (Int) -> Unit,
    onMarkTaken: (Int) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { MedicationSectionHeader(count = medications.size) }
        items(medications, key = { medication -> medication.id }) { medication ->
            MedicationCard(
                medication = medication,
                onDelete = { onDelete(medication.id) },
                onMarkTaken = { onMarkTaken(medication.id) }
            )
        }
    }
}

@Composable
private fun MedicationSectionHeader(count: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Geplante Medikamente",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = if (count == 0) {
                "Noch keine Einträge vorhanden."
            } else {
                "$count Einträge mit einer oder mehreren Tageszeiten."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun MedicationCard(
    medication: Medication,
    onDelete: () -> Unit,
    onMarkTaken: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm") }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(medication.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = medication.dosage.ifBlank { "Dosis offen" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "${medication.intakeTimes.size}x täglich",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                medication.intakeTimes.forEach { intakeTime ->
                    AssistChip(
                        onClick = {},
                        label = { Text(intakeTime.label()) }
                    )
                }
            }

            if (medication.notes.isNotBlank()) {
                Text(
                    text = medication.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Text(
                text = "Zuletzt genommen: ${medication.lastTakenAt?.let { timestamp ->
                    formatter.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
                } ?: "noch nicht bestätigt"}",
                style = MaterialTheme.typography.bodyMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onMarkTaken) {
                    Text("Jetzt genommen")
                }
                TextButton(onClick = onDelete) {
                    Text("Löschen")
                }
            }
        }
    }
}

private val Color.alphaSurface: Color
    get() = copy(alpha = 0.12f)

