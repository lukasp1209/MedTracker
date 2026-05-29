package com.example.medtracker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medtracker.data.MedicationRepository
import com.example.medtracker.data.local.AppDatabase
import com.example.medtracker.model.Medication
import com.example.medtracker.reminder.ReminderScheduler
import com.example.medtracker.ui.theme.MedTrackerTheme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Hosts the weekly medication plan in its own activity.
 */
class WeeklyScheduleActivity : ComponentActivity() {
    private val viewModel: MedTrackerViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)
        MedTrackerViewModel.Factory(
            MedicationRepository(database.medicationDao()),
            ReminderScheduler(this)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MedTrackerTheme {
                WeeklyScheduleScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

/**
 * Shows the weekly medication plan grouped by weekday.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScheduleScreen(
    viewModel: MedTrackerViewModel,
    onBack: () -> Unit
) {
    val medications by viewModel.medications.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wochenplan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurueck")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                )
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DayOfWeek.values().forEach { day ->
                WeeklyDayCard(
                    day = day,
                    medications = medications.filter { it.daysOfWeek.contains(day) }
                )
            }
        }
    }
}

@Composable
private fun WeeklyDayCard(
    day: DayOfWeek,
    medications: List<Medication>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = day.getDisplayName(TextStyle.FULL, Locale.GERMAN),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            if (medications.isEmpty()) {
                Text(
                    "Keine Medikamente geplant",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                medications.forEach { medication ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(medication.name, fontWeight = FontWeight.Medium)
                            Text(medication.dosage, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            medication.intakeTimes.joinToString(", ") { it.label() },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
