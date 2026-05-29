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
import com.example.medtracker.data.MedicationRepository
import com.example.medtracker.data.local.AppDatabase
import com.example.medtracker.model.IntakeTime
import com.example.medtracker.reminder.ReminderScheduler
import com.example.medtracker.ui.HistoryScreen
import com.example.medtracker.ui.FormDayUiState
import com.example.medtracker.ui.MedTrackerScreen as Screen
import com.example.medtracker.ui.MedicationCardUiState
import com.example.medtracker.ui.MedicationFormUiState
import com.example.medtracker.ui.MedTrackerViewModel
import com.example.medtracker.ui.WeeklyScheduleScreen
import com.example.medtracker.ui.theme.MedTrackerTheme

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
                val currentScreen by viewModel.currentScreen.collectAsState()

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                label = { Text("Heute") },
                                selected = currentScreen == Screen.Today,
                                onClick = { viewModel.selectScreen(Screen.Today) }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                                label = { Text("Woche") },
                                selected = currentScreen == Screen.Week,
                                onClick = { viewModel.selectScreen(Screen.Week) }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.History, contentDescription = null) },
                                label = { Text("Historie") },
                                selected = currentScreen == Screen.History,
                                onClick = { viewModel.selectScreen(Screen.History) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            Screen.Today -> MedTrackerScreen(
                                viewModel = viewModel,
                                onRequestExactAlarmAccess = { requestExactAlarmAccess() }
                            )
                            Screen.History -> HistoryScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.selectScreen(Screen.Today) }
                            )
                            Screen.Week -> WeeklyScheduleScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.selectScreen(Screen.Today) }
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

    override fun onResume() {
        super.onResume()
        viewModel.refreshExactAlarmAccess()
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
    val medicationCards by viewModel.medicationCards.collectAsState()
    val medicationCountText by viewModel.medicationCountText.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val formDays by viewModel.formDays.collectAsState()
    val canScheduleExactAlarms by viewModel.canScheduleExactAlarms.collectAsState()
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
            val requestNotifications = {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

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
                            medicationCount = medicationCards.size,
                            canScheduleExactAlarms = canScheduleExactAlarms,
                            onRequestNotifications = requestNotifications,
                            onRequestExactAlarmAccess = onRequestExactAlarmAccess
                        )
                        MedicationForm(
                            state = formState,
                            dayOptions = formDays,
                            onNameChange = viewModel::updateMedicationName,
                            onDosageChange = viewModel::updateDosage,
                            onDosageExpandedChange = viewModel::setDosageExpanded,
                            onDosageSuggestionSelected = viewModel::selectDosageSuggestion,
                            onDayToggle = viewModel::toggleSelectedDay,
                            onHourChange = viewModel::updateHour,
                            onMinuteChange = viewModel::updateMinute,
                            onAddIntakeTime = viewModel::addIntakeTimeFromInput,
                            onRemoveIntakeTime = viewModel::removeIntakeTime,
                            onNotesChange = viewModel::updateNotes,
                            onSave = viewModel::saveMedicationFromForm
                        )
                    }

                    MedicationList(
                        medications = medicationCards,
                        countText = medicationCountText,
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
                            medicationCount = medicationCards.size,
                            canScheduleExactAlarms = canScheduleExactAlarms,
                            onRequestNotifications = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    requestNotifications()
                                }
                            },
                            onRequestExactAlarmAccess = onRequestExactAlarmAccess
                        )
                    }
                    item {
                        MedicationForm(
                            state = formState,
                            dayOptions = formDays,
                            onNameChange = viewModel::updateMedicationName,
                            onDosageChange = viewModel::updateDosage,
                            onDosageExpandedChange = viewModel::setDosageExpanded,
                            onDosageSuggestionSelected = viewModel::selectDosageSuggestion,
                            onDayToggle = viewModel::toggleSelectedDay,
                            onHourChange = viewModel::updateHour,
                            onMinuteChange = viewModel::updateMinute,
                            onAddIntakeTime = viewModel::addIntakeTimeFromInput,
                            onRemoveIntakeTime = viewModel::removeIntakeTime,
                            onNotesChange = viewModel::updateNotes,
                            onSave = viewModel::saveMedicationFromForm
                        )
                    }
                    item {
                        MedicationSectionHeader(countText = medicationCountText)
                    }
                    items(medicationCards, key = { item -> item.id }) { medication ->
                        MedicationCard(
                            medication = medication,
                            onDelete = { viewModel.deleteMedication(medication.id) },
                            onMarkTaken = { viewModel.markTaken(medication.id) }
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
    state: MedicationFormUiState,
    dayOptions: List<FormDayUiState>,
    onNameChange: (String) -> Unit,
    onDosageChange: (String) -> Unit,
    onDosageExpandedChange: (Boolean) -> Unit,
    onDosageSuggestionSelected: (String) -> Unit,
    onDayToggle: (DayOfWeek) -> Unit,
    onHourChange: (String) -> Unit,
    onMinuteChange: (String) -> Unit,
    onAddIntakeTime: () -> Unit,
    onRemoveIntakeTime: (IntakeTime) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit
) {
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
                value = state.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.name_label)) },
                shape = RoundedCornerShape(20.dp),
                singleLine = true
            )
            ExposedDropdownMenuBox(
                expanded = state.dosageExpanded,
                onExpandedChange = onDosageExpandedChange
            ) {
                OutlinedTextField(
                    value = state.dosage,
                    onValueChange = onDosageChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable, true),
                    label = { Text(stringResource(R.string.dosage_label)) },
                    placeholder = { Text("z. B. 1 Tablette") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.dosageExpanded) },
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = state.dosageExpanded,
                    onDismissRequest = { onDosageExpandedChange(false) }
                ) {
                    state.dosageSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = { onDosageSuggestionSelected(suggestion) }
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
                    dayOptions.forEach { day ->
                        FilterChip(
                            selected = day.selected,
                            onClick = { onDayToggle(day.day) },
                            label = { Text(day.label) }
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
                            value = state.hourString,
                            onValueChange = onHourChange,
                            modifier = Modifier.weight(1f),
                            label = { Text("Stunde") },
                            placeholder = { Text("HH") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.minuteString,
                            onValueChange = onMinuteChange,
                            modifier = Modifier.weight(1f),
                            label = { Text("Minute") },
                            placeholder = { Text("mm") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true
                        )
                    }
                    OutlinedButton(
                        onClick = onAddIntakeTime
                    ) {
                        Text(stringResource(R.string.add_time))
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.intakeTimes.forEach { intakeTime ->
                            FilterChip(
                                selected = false,
                                onClick = { onRemoveIntakeTime(intakeTime) },
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
                value = state.notes,
                onValueChange = onNotesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.notes_label)) },
                placeholder = { Text("Mit Wasser, nach dem Essen ...") },
                shape = RoundedCornerShape(20.dp)
            )
            Button(
                onClick = onSave,
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
    medications: List<MedicationCardUiState>,
    countText: String,
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
        item { MedicationSectionHeader(countText = countText) }
        items(medications, key = { medication -> medication.id }) { medication ->
            MedicationCard(
                medication = medication,
                onDelete = { onDelete(medication.id) },
                onMarkTaken = { onMarkTaken(medication.id) }
            )
        }
    }
}

/**
 * Shows the heading above the medication list and summarizes how many entries exist.
 */
@Composable
private fun MedicationSectionHeader(countText: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.planned_medications),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = countText,
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
    medication: MedicationCardUiState,
    onDelete: () -> Unit,
    onMarkTaken: () -> Unit
) {
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
                        text = medication.dosageText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = medication.intakeCountText,
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
                medication.intakeTimeLabels.forEach { intakeTimeLabel ->
                    AssistChip(
                        onClick = {},
                        label = { Text(intakeTimeLabel) }
                    )
                }
            }

            medication.notesText?.let { notes ->
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Text(
                text = stringResource(R.string.last_taken_label, medication.lastTakenText),
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
