package com.example.medtracker.data

import android.content.Context
import com.example.medtracker.data.local.AppDatabase
import com.example.medtracker.data.local.IntakeHistoryEntity
import com.example.medtracker.data.local.MedicationDao
import com.example.medtracker.data.local.MedicationEntity
import com.example.medtracker.model.IntakeTime
import com.example.medtracker.model.Medication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Coordinates medication data access and converts database rows into domain models.
 */
class MedicationRepository(private val medicationDao: MedicationDao) {

    /**
     * Creates a repository from an Android context by opening the app database.
     */
    constructor(context: Context) : this(AppDatabase.getDatabase(context).medicationDao())

    /**
     * Observes all medications sorted by their first intake time and then by name.
     */
    val medications: Flow<List<Medication>> = medicationDao.getAllMedications().map { entities ->
        entities.map { it.toDomain() }.sortedWith(
            compareBy<Medication> { medication ->
                medication.intakeTimes.minOfOrNull { it.hour * 60 + it.minute } ?: Int.MAX_VALUE
            }.thenBy { it.name }
        )
    }

    /**
     * Loads all medications once, using the same sorting as the observable medication list.
     */
    suspend fun getAll(): List<Medication> {
        return medicationDao.getAllMedicationsList().map { it.toDomain() }.sortedWith(
            compareBy<Medication> { medication ->
                medication.intakeTimes.minOfOrNull { it.hour * 60 + it.minute } ?: Int.MAX_VALUE
            }.thenBy { it.name }
        )
    }

    /**
     * Observes the complete medication intake history.
     */
    val history: Flow<List<IntakeHistoryEntity>> = medicationDao.getHistory()

    /**
     * Adds a new medication after trimming text fields and sorting its intake times.
     */
    suspend fun add(name: String, dosage: String, intakeTimes: List<IntakeTime>, notes: String, daysOfWeek: Set<java.time.DayOfWeek>) {
        val entity = MedicationEntity(
            name = name.trim(),
            dosage = dosage.trim(),
            notes = notes.trim(),
            lastTakenAt = null,
            intakeTimes = intakeTimes.sortedWith(compareBy<IntakeTime> { it.hour }.thenBy { it.minute }),
            daysOfWeek = daysOfWeek
        )
        medicationDao.insertMedication(entity)
    }

    /**
     * Deletes the medication with the given id.
     */
    suspend fun delete(id: Int) {
        medicationDao.deleteMedication(id)
    }

    /**
     * Marks the medication with the given id as taken at the provided timestamp.
     */
    suspend fun markTaken(medicationId: Int, timestamp: Long = System.currentTimeMillis()) {
        val medication = medicationDao.getAllMedicationsList().find { it.id == medicationId } ?: return
        markTaken(medication.toDomain(), timestamp)
    }

    /**
     * Updates a medication's last intake time and writes a matching history entry.
     */
    suspend fun markTaken(medication: Medication, timestamp: Long = System.currentTimeMillis()) {
        // 1. Update Medication
        medicationDao.updateMedication(medication.toEntity().copy(lastTakenAt = timestamp))
        
        // 2. Add to History (Audit Trail)
        medicationDao.insertHistory(
            IntakeHistoryEntity(
                medicationId = medication.id,
                medicationName = medication.name,
                takenAt = timestamp
            )
        )
    }

    /**
     * Converts a database entity into the domain model used outside the data layer.
     */
    private fun MedicationEntity.toDomain(): Medication {
        return Medication(
            id = id,
            name = name,
            dosage = dosage,
            intakeTimes = intakeTimes,
            notes = notes,
            lastTakenAt = lastTakenAt,
            daysOfWeek = daysOfWeek
        )
    }

    /**
     * Converts a domain model back into the database entity used by Room.
     */
    private fun Medication.toEntity(): MedicationEntity {
        return MedicationEntity(
            id = if (id == 0) 0 else id,
            name = name,
            dosage = dosage,
            notes = notes,
            lastTakenAt = lastTakenAt,
            intakeTimes = intakeTimes,
            daysOfWeek = daysOfWeek
        )
    }
}
