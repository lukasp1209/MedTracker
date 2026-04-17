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

class MedTrackerViewModel(
    private val repository: MedicationRepository,
    private val scheduler: ReminderScheduler
) : ViewModel() {

    // Wir nutzen Flows für Echtzeit-Updates aus der Datenbank
    val medications: StateFlow<List<Medication>> = repository.medications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<IntakeHistoryEntity>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Alarme automatisch neu planen, wenn sich die Medikamente ändern
        viewModelScope.launch {
            medications.collect { list ->
                if (list.isNotEmpty()) {
                    scheduler.rescheduleAll(list)
                }
            }
        }
    }

    fun addMedication(name: String, dosage: String, intakeTimes: List<IntakeTime>, notes: String, daysOfWeek: Set<java.time.DayOfWeek>) {
        viewModelScope.launch {
            repository.add(name, dosage, intakeTimes, notes, daysOfWeek)
        }
    }

    fun deleteMedication(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
            scheduler.cancelMedication(id)
        }
    }

    fun markTaken(medication: Medication) {
        viewModelScope.launch {
            repository.markTaken(medication)
        }
    }

    fun canScheduleExactAlarms(): Boolean = scheduler.canScheduleExactAlarms()

    class Factory(
        private val repository: MedicationRepository,
        private val scheduler: ReminderScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return MedTrackerViewModel(repository, scheduler) as T
        }
    }
}
