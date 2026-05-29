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
import androidx.activity.viewModels
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import com.example.medtracker.data.MedicationRepository
import com.example.medtracker.data.local.AppDatabase
import com.example.medtracker.model.IntakeTime
import com.example.medtracker.model.Medication
import com.example.medtracker.reminder.ReminderScheduler
import com.example.medtracker.ui.HistoryScreen
import com.example.medtracker.ui.MedTrackerViewModel
import com.example.medtracker.ui.WeeklyScheduleScreen
import com.example.medtracker.ui.theme.MedTrackerTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Main entry point of the app and host for the medication and history screens.
 */
class MainActivity : ComponentActivity() {
    private val viewModel: MedTrackerViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)
        MedTrackerViewModel.Factory(
            MedicationRepository(database.medicationDao()),
            ReminderScheduler(this)
        )
    }

    /**
     * Creates the Compose UI and switches between the screens hosted by this activity.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MedTrackerTheme {
                var currentScreen by remember { mutableStateOf("today") }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                label = { Text("Heute") },
                                selected = currentScreen == "today",
                                onClick = { currentScreen = "today" }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                                label = { Text("Woche") },
                                selected = currentScreen == "week",
                                onClick = { currentScreen = "week" }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.History, contentDescription = null) },
                                label = { Text("Historie") },
                                selected = currentScreen == "history",
                                onClick = { currentScreen = "history" }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            "today" -> MedTrackerScreen(
                                viewModel = viewModel,
                                onRequestExactAlarmAccess = { requestExactAlarmAccess() }
                            )
                            "history" -> HistoryScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = "today" }
                            )
                            "week" -> WeeklyScheduleScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = "today" }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Opens Android settings so the user can allow exact alarm scheduling.
     */
    private fun requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }
}

/**
 * Displays the main medication overview and adapts between phone and tablet layouts.
 */
@Composable
private fun MedTrackerScreen(
    viewModel: MedTrackerViewModel,
    onRequestExactAlarmAccess: () -> Unit
) {
    val medications by viewModel.medications.collectAsState()
    val notificationPermissionLauncher = rememberNotificationPermissionLauncher()

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
            val tabletLayout = this.maxWidth >= 900.dp
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
                            canScheduleExactAlarms = viewModel.canScheduleExactAlarms(),
                            onRequestNotifications = {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            onRequestExactAlarmAccess = onRequestExactAlarmAccess
                        )
                        MedicationForm(onSave = viewModel::addMedication)
                    }

                    MedicationList(
                        medications = medications,
                        modifier = Modifier.weight(1.25f),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        onDelete = viewModel::deleteMedication,
                        onMarkTaken = viewModel::markTaken
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
                            canScheduleExactAlarms = viewModel.canScheduleExactAlarms(),
                            onRequestNotifications = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onRequestExactAlarmAccess = onRequestExactAlarmAccess
                        )
                    }
                    item {
                        MedicationForm(onSave = viewModel::addMedication)
                    }
                    item {
                        MedicationSectionHeader(count = medications.size)
                    }
                    items(medications, key = { medication -> medication.id }) { medication ->
                        MedicationCard(
                            medication = medication,
                            onDelete = { viewModel.deleteMedication(medication.id) },
                            onMarkTaken = { viewModel.markTaken(medication) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Creates the permission launcher used to request notification access on Android 13 and newer.
 */
@Composable
private fun rememberNotificationPermissionLauncher() =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

/**
 * Shows the top summary card with medication count and permission actions.
 */
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
                    text = stringResource(R.string.planned_medications_count, medicationCount),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "MedTracker Übersicht",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Hier hast du deine tägliche Einnahme im Blick.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    OutlinedButton(onClick = onRequestNotifications) {
                        Text("Benachrichtigungen")
                    }
                }
                if (!canScheduleExactAlarms) {
                    OutlinedButton(onClick = onRequestExactAlarmAccess) {
                        Text("Alarme erlauben")
                    }
                }
            }
        }
    }
}

/**
 * Displays the form for entering a new medication, its dosage, weekdays, times, and notes.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MedicationForm(
    onSave: (String, String, List<IntakeTime>, String, Set<DayOfWeek>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var dosageExpanded by remember { mutableStateOf(false) }
    var hourString by remember { mutableStateOf("08") }
    var minuteString by remember { mutableStateOf("00") }
    var notes by remember { mutableStateOf("") }
    val intakeTimes = remember { androidx.compose.runtime.mutableStateListOf(IntakeTime(8, 0)) }
    val selectedDays = remember { mutableStateOf(DayOfWeek.values().toSet()) }

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
            Text(stringResource(R.string.new_medication), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.name_label)) },
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
                    label = { Text(stringResource(R.string.dosage_label)) },
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

            // Wochentage Auswahl
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Wochentage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DayOfWeek.values().forEach { day ->
                        FilterChip(
                            selected = selectedDays.value.contains(day),
                            onClick = {
                                if (selectedDays.value.contains(day)) {
                                    if (selectedDays.value.size > 1) {
                                        selectedDays.value = selectedDays.value - day
                                    }
                                } else {
                                    selectedDays.value = selectedDays.value + day
                                }
                            },
                            label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.GERMAN)) }
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
                    Text(stringResource(R.string.intake_times_label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = hourString,
                            onValueChange = { input ->
                                if (input.length <= 2 && input.all { it.isDigit() }) hourString = input
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Stunde") },
                            placeholder = { Text("HH") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = minuteString,
                            onValueChange = { input ->
                                if (input.length <= 2 && input.all { it.isDigit() }) minuteString = input
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Minute") },
                            placeholder = { Text("mm") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val h = hourString.toIntOrNull()?.coerceIn(0, 23) ?: 8
                            val m = minuteString.toIntOrNull()?.coerceIn(0, 59) ?: 0
                            val candidate = IntakeTime(h, m)
                            if (intakeTimes.none { it.hour == candidate.hour && it.minute == candidate.minute }) {
                                intakeTimes.add(candidate)
                                intakeTimes.sortWith(compareBy<IntakeTime> { it.hour }.thenBy { it.minute })
                            }
                        }
                    ) {
                        Text(stringResource(R.string.add_time))
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
                label = { Text(stringResource(R.string.notes_label)) },
                placeholder = { Text("Mit Wasser, nach dem Essen ...") },
                shape = RoundedCornerShape(20.dp)
            )
            Button(
                onClick = {
                    if (name.isBlank() || intakeTimes.isEmpty()) return@Button
                    onSave(name, dosage, intakeTimes.toList(), notes, selectedDays.value)
                    name = ""
                    dosage = ""
                    hourString = "08"
                    minuteString = "00"
                    notes = ""
                    intakeTimes.clear()
                    intakeTimes.add(IntakeTime(8, 0))
                    selectedDays.value = DayOfWeek.values().toSet()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.add_medication))
            }
        }
    }
}

/**
 * Displays the scrollable list of all medication cards.
 */
@Composable
private fun MedicationList(
    medications: List<Medication>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    onDelete: (Int) -> Unit,
    onMarkTaken: (Medication) -> Unit
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
                onMarkTaken = { onMarkTaken(medication) }
            )
        }
    }
}

/**
 * Shows the heading above the medication list and summarizes how many entries exist.
 */
@Composable
private fun MedicationSectionHeader(count: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.planned_medications),
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

/**
 * Displays one medication with its dosage, intake times, notes, last intake, and actions.
 */
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
                text = stringResource(R.string.last_taken_label, medication.lastTakenAt?.let { timestamp ->
                    formatter.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
                } ?: stringResource(R.string.never_taken)),
                style = MaterialTheme.typography.bodyMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onMarkTaken) {
                    Text(stringResource(R.string.mark_taken_label))
                }
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.delete_label))
                }
            }
        }
    }
}
