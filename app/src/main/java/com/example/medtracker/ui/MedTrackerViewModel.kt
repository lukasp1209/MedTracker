package com.example.medtracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medtracker.data.MedicationRepository
import com.example.medtracker.data.local.IntakeHistoryEntity
import com.example.medtracker.model.IntakeTime
import com.example.medtracker.model.Medication
import com.example.medtracker.reminder.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds UI state for medications and connects user actions to persistence and reminders.
 */
class MedTrackerViewModel(
    private val repository: MedicationRepository,
    private val scheduler: ReminderScheduler
) : ViewModel() {

    /**
     * Current medication list exposed as Compose-friendly state.
     */
    val medications: StateFlow<List<Medication>> = repository.medications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Current intake history exposed as Compose-friendly state.
     */
    val history: StateFlow<List<IntakeHistoryEntity>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    fun addMedication(name: String, dosage: String, intakeTimes: List<IntakeTime>, notes: String, daysOfWeek: Set<java.time.DayOfWeek>) {
        viewModelScope.launch {
            repository.add(name, dosage, intakeTimes, notes, daysOfWeek)
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
    fun markTaken(medication: Medication) {
        viewModelScope.launch {
            repository.markTaken(medication)
        }
    }

    /**
     * Reports whether exact alarm scheduling is currently available.
     */
    fun canScheduleExactAlarms(): Boolean = scheduler.canScheduleExactAlarms()

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
