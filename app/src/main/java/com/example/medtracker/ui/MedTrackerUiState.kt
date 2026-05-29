package com.example.medtracker.ui

import com.example.medtracker.model.IntakeTime
import java.time.DayOfWeek

enum class MedTrackerScreen {
    Today,
    Week,
    History
}

enum class HistoryStatusUiType {
    Taken,
    Snoozed,
    Skipped
}

data class MedicationFormUiState(
    val name: String = "",
    val dosage: String = "",
    val dosageExpanded: Boolean = false,
    val hourString: String = "08",
    val minuteString: String = "00",
    val notes: String = "",
    val intakeTimes: List<IntakeTime> = listOf(IntakeTime(8, 0)),
    val selectedDays: Set<DayOfWeek> = DayOfWeek.values().toSet(),
    val dosageSuggestions: List<String> = listOf(
        "1 Tablette",
        "1/2 Tablette",
        "2 Tabletten",
        "5 ml",
        "10 ml",
        "1 Kapsel"
    )
)

data class FormDayUiState(
    val day: DayOfWeek,
    val label: String,
    val selected: Boolean
)

data class MedicationCardUiState(
    val id: Int,
    val name: String,
    val dosageText: String,
    val intakeCountText: String,
    val intakeTimeLabels: List<String>,
    val notesText: String?,
    val lastTakenText: String
)

data class HistoryEntryUiState(
    val id: Int,
    val medicationName: String,
    val takenAtText: String,
    val status: HistoryStatusUiType
)

data class WeeklyMedicationUiState(
    val name: String,
    val dosage: String,
    val intakeTimesText: String
)

data class WeeklyDayUiState(
    val dayLabel: String,
    val medications: List<WeeklyMedicationUiState>
)
