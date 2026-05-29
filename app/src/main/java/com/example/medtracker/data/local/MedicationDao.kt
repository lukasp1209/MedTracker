package com.example.medtracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Defines all Room queries used to read and write medication data.
 */
@Dao
interface MedicationDao {
    /**
     * Observes all medications and emits a new list whenever the table changes.
     */
    @Query("SELECT * FROM medications")
    fun getAllMedications(): Flow<List<MedicationEntity>>

    /**
     * Reads all medications once for background work such as scheduling alarms.
     */
    @Query("SELECT * FROM medications")
    suspend fun getAllMedicationsList(): List<MedicationEntity>

    /**
     * Inserts a medication or replaces an existing row with the same primary key.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity)

    /**
     * Updates an existing medication row.
     */
    @Update
    suspend fun updateMedication(medication: MedicationEntity)

    /**
     * Deletes the medication with the given database id.
     */
    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteMedication(id: Int)

    /**
     * Stores one history entry for an intake event.
     */
    @Insert
    suspend fun insertHistory(history: IntakeHistoryEntity)

    /**
     * Observes the complete intake history, newest entries first.
     */
    @Query("SELECT * FROM intake_history ORDER BY takenAt DESC")
    fun getHistory(): Flow<List<IntakeHistoryEntity>>

    /**
     * Deletes one recorded history entry.
     */
    @Query("DELETE FROM intake_history WHERE id = :id")
    suspend fun deleteHistoryEntry(id: Int)

    /**
     * Deletes the complete recorded intake history.
     */
    @Query("DELETE FROM intake_history")
    suspend fun clearHistory()
}
