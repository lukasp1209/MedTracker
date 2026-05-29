package com.example.medtracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medtracker.data.MedicationRepository
import com.example.medtracker.model.IntakeTime
import com.example.medtracker.model.Medication
import com.example.medtracker.reminder.ReminderScheduler
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds UI state for medications and connects user actions to persistence and reminders.
 */
class MedTrackerViewModel(
    private val repository: MedicationRepository,
    private val scheduler: ReminderScheduler
) : ViewModel() {
    private val medicationFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    private val historyFormatter = DateTimeFormatter.ofPattern("dd. MMMM yyyy, HH:mm 'Uhr'")

    /**
     * Current medication list exposed as Compose-friendly state.
     */
    private val medications: StateFlow<List<Medication>> = repository.medications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Current intake history exposed as Compose-friendly state.
     */
    val medicationCards: StateFlow<List<MedicationCardUiState>> = medications
        .map { list -> list.map { it.toMedicationCardUiState() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyEntries: StateFlow<List<HistoryEntryUiState>> = repository.history
        .map { entries ->
            entries.map { entry ->
                HistoryEntryUiState(
                    id = entry.id,
                    medicationName = entry.medicationName,
                    takenAtText = historyFormatter.format(
                        Instant.ofEpochMilli(entry.takenAt).atZone(ZoneId.systemDefault())
                    ),
                    status = when (entry.status) {
                        "TAKEN" -> HistoryStatusUiType.Taken
                        "SNOOZED" -> HistoryStatusUiType.Snoozed
                        else -> HistoryStatusUiType.Skipped
                    }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklySchedule: StateFlow<List<WeeklyDayUiState>> = medications
        .map { list ->
            DayOfWeek.values().map { day ->
                WeeklyDayUiState(
                    dayLabel = day.getDisplayName(TextStyle.FULL, Locale.GERMAN),
                    medications = list
                        .filter { it.daysOfWeek.contains(day) }
                        .map { medication ->
                            WeeklyMedicationUiState(
                                name = medication.name,
                                dosage = medication.dosage,
                                intakeTimesText = medication.intakeTimes.joinToString(", ") { it.label() }
                            )
                        }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentScreen = MutableStateFlow(MedTrackerScreen.Today)
    val currentScreen: StateFlow<MedTrackerScreen> = _currentScreen

    private val _formState = MutableStateFlow(MedicationFormUiState())
    val formState: StateFlow<MedicationFormUiState> = _formState

    val formDays: StateFlow<List<FormDayUiState>> = formState
        .map { state ->
            DayOfWeek.values().map { day ->
                FormDayUiState(
                    day = day,
                    label = day.getDisplayName(TextStyle.SHORT, Locale.GERMAN),
                    selected = state.selectedDays.contains(day)
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showClearHistoryDialog = MutableStateFlow(false)
    val showClearHistoryDialog: StateFlow<Boolean> = _showClearHistoryDialog

    private val _canScheduleExactAlarms = MutableStateFlow(scheduler.canScheduleExactAlarms())
    val canScheduleExactAlarms: StateFlow<Boolean> = _canScheduleExactAlarms

    val medicationCountText: StateFlow<String> = medicationCards
        .map { cards ->
            if (cards.isEmpty()) {
                "Noch keine Einträge vorhanden."
            } else {
                "${cards.size} Einträge mit einer oder mehreren Tageszeiten."
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Noch keine Einträge vorhanden.")

    init {
        viewModelScope.launch {
            medications.collect { list ->
                scheduler.rescheduleAll(list)
            }
        }
    }

    /**
     * Saves a new medication and lets the repository normalize the stored values.
     */
    fun saveMedicationFromForm() {
        val state = formState.value
        if (state.name.isBlank() || state.intakeTimes.isEmpty()) return

        viewModelScope.launch {
            repository.add(state.name, state.dosage, state.intakeTimes, state.notes, state.selectedDays)
            resetForm()
        }
    }

    fun selectScreen(screen: MedTrackerScreen) {
        _currentScreen.value = screen
    }

    fun updateMedicationName(value: String) {
        _formState.update { it.copy(name = value) }
    }

    fun updateDosage(value: String) {
        _formState.update { it.copy(dosage = value) }
    }

    fun setDosageExpanded(expanded: Boolean) {
        _formState.update { it.copy(dosageExpanded = expanded) }
    }

    fun selectDosageSuggestion(suggestion: String) {
        _formState.update { it.copy(dosage = suggestion, dosageExpanded = false) }
    }

    fun updateHour(value: String) {
        if (value.length <= 2 && value.all { it.isDigit() }) {
            _formState.update { it.copy(hourString = value) }
        }
    }

    fun updateMinute(value: String) {
        if (value.length <= 2 && value.all { it.isDigit() }) {
            _formState.update { it.copy(minuteString = value) }
        }
    }

    fun updateNotes(value: String) {
        _formState.update { it.copy(notes = value) }
    }

    fun toggleSelectedDay(day: DayOfWeek) {
        _formState.update { state ->
            val updatedDays = if (state.selectedDays.contains(day)) {
                if (state.selectedDays.size > 1) state.selectedDays - day else state.selectedDays
            } else {
                state.selectedDays + day
            }
            state.copy(selectedDays = updatedDays)
        }
    }

    fun addIntakeTimeFromInput() {
        _formState.update { state ->
            val hour = state.hourString.toIntOrNull()?.coerceIn(0, 23) ?: 8
            val minute = state.minuteString.toIntOrNull()?.coerceIn(0, 59) ?: 0
            val candidate = IntakeTime(hour, minute)
            if (state.intakeTimes.any { it.hour == candidate.hour && it.minute == candidate.minute }) {
                state
            } else {
                state.copy(
                    intakeTimes = (state.intakeTimes + candidate)
                        .sortedWith(compareBy<IntakeTime> { it.hour }.thenBy { it.minute })
                )
            }
        }
    }

    fun removeIntakeTime(intakeTime: IntakeTime) {
        _formState.update { state ->
            state.copy(intakeTimes = state.intakeTimes - intakeTime)
        }
    }

    /**
     * Deletes a medication and cancels all alarms that belong to it.
     */
    fun deleteMedication(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
            scheduler.cancelMedication(id)
        }
    }

    /**
     * Deletes one item from the intake history.
     */
    fun deleteHistoryEntry(id: Int) {
        viewModelScope.launch {
            repository.deleteHistoryEntry(id)
        }
    }

    /**
     * Deletes the complete intake history.
     */
    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    /**
     * Records that the given medication has been taken.
     */
    fun markTaken(id: Int) {
        viewModelScope.launch {
            repository.markTaken(id)
        }
    }

    /**
     * Reports whether exact alarm scheduling is currently available.
     */
    fun refreshExactAlarmAccess() {
        _canScheduleExactAlarms.value = scheduler.canScheduleExactAlarms()
    }

    fun showClearHistoryDialog() {
        _showClearHistoryDialog.value = true
    }

    fun dismissClearHistoryDialog() {
        _showClearHistoryDialog.value = false
    }

    private fun resetForm() {
        _formState.value = MedicationFormUiState()
    }

    private fun Medication.toMedicationCardUiState(): MedicationCardUiState {
        return MedicationCardUiState(
            id = id,
            name = name,
            dosageText = dosage.ifBlank { "Dosis offen" },
            intakeCountText = "${intakeTimes.size}x täglich",
            intakeTimeLabels = intakeTimes.map { it.label() },
            notesText = notes.ifBlank { null },
            lastTakenText = lastTakenAt?.let { timestamp ->
                medicationFormatter.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
            } ?: "noch nicht bestätigt"
        )
    }

    /**
     * Creates the view model with dependencies that are built outside of the default constructor.
     */
    class Factory(
        private val repository: MedicationRepository,
        private val scheduler: ReminderScheduler
    ) : ViewModelProvider.Factory {
        /**
         * Returns a MedTrackerViewModel instance for Android's ViewModel provider.
         */
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return MedTrackerViewModel(repository, scheduler) as T
        }
    }
}
